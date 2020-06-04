package main.phraseSearching;

import main.model.TextSearchResult;
import main.queryprocessor.QueryProcessor;
import main.ranker.Ranker;
import main.utilities.ConnectToDB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
public class PhraseSearch {
	public static List<String> preparePhrase(String text) {
		String sphrase = text.toLowerCase();
		sphrase = sphrase.replaceAll("[^0-9a-zA-Z]", " ");
        String[] p_words = sphrase.split(" ");
        List<String> p = new ArrayList<String>();
        for(String s : p_words) {
        	p.add(s);
        }
        return p;
	}

	public static List<TextSearchResult> search(Ranker ranker, String phrase, String country, List<String> queryStemmedWords) {
		ConcurrentHashMap<String,Boolean> found = new ConcurrentHashMap<String,Boolean>();
		List<TextSearchResult> filteredResults = new ArrayList<TextSearchResult>();
		LinkedBlockingQueue<String> linkedQueue = new LinkedBlockingQueue<String>();
		List<Thread> searchThreads = new ArrayList<Thread>();
		List<TextSearchResult> results = ranker.rankPhrase(queryStemmedWords, country);
		List<String> p = preparePhrase(phrase);
		KMP kmp = new KMP(p);
		kmp.buildLps();
		int numberOfThreads =  Math.max(Math.min(20,results.size()/1000),1);
		for(TextSearchResult r : results) {
			linkedQueue.offer(r.getUrl());
		}

		for(int i = 0 ; i < numberOfThreads; ++i) {
			Thread t = new Thread(new PhraseSearchThread(linkedQueue, found, kmp));
			t.start();
			searchThreads.add(t);
		}
		for(Thread t : searchThreads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for(TextSearchResult r : results) {
			if(found.get(r.getUrl()) != null) {
				filteredResults.add(r);
			}
		}
		return filteredResults;
	}

	/*
	public static void main(String[] args) {
		Ranker ranker = new Ranker();
		ConnectToDB.establishConnection();
		QueryProcessor q = QueryProcessor.getInstance();
		List<String> words = q.process("I can't breathe");
		double startTime  = (double)System.nanoTime();
		List<TextSearchResult> l = PhraseSearch.search(ranker, "I can't breathe", null, words);
		System.out.println("finished");
		double endTime  = (double)System.nanoTime();
		double totalTime = (endTime - startTime)* (1e-9);
		System.out.println(totalTime);
		for(TextSearchResult r : l) {
			System.out.println(r.getUrl());
		}
	}
	*/
}
