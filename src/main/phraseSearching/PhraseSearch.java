package main.phraseSearching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import main.model.TextSearchResult;
import main.queryprocessor.QueryProcessor;
import main.ranker.Ranker;
import main.utilities.ConnectToDB;
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
	
	public static List<TextSearchResult> search(String phrase) {
		ConcurrentHashMap<String,Boolean> found = new ConcurrentHashMap<String,Boolean>();
		List<TextSearchResult> filteredResults = new ArrayList<TextSearchResult>();
		QueryProcessor q = QueryProcessor.getInstance();
		List<String> words  = q.process(phrase);
		HashMap<String, Integer> isAllWordsInPage = new HashMap<String, Integer>();
		List<TextSearchResult> results = new ArrayList<TextSearchResult>();
		LinkedBlockingQueue<String> linkedQueue = new LinkedBlockingQueue<String>();
		List<Thread> searchThreads = new ArrayList<Thread>();
		List<Thread> rankThreads = new ArrayList<Thread>();
		List<String> p = preparePhrase(phrase);
		for(String word : words) {
			Thread t = new Thread() {
				public void run() {
					List<String> w = new ArrayList<String>();
					w.add(word);
					List<TextSearchResult> res = Ranker.rankText(w);
					for(TextSearchResult r : res) {
						synchronized(isAllWordsInPage) {
							if(isAllWordsInPage.get(r.getUrl()) == null) {
								isAllWordsInPage.put(r.getUrl(), 1);
							} else {
								isAllWordsInPage.put(r.getUrl(), isAllWordsInPage.get(r.getUrl())+1);
							}
							if(isAllWordsInPage.get(r.getUrl()) == words.size()) {
								results.add(r);
							}
						}
					}
				}
			};
			t.start();
			rankThreads.add(t);
		}
		KMP kmp = new KMP(p);
		kmp.buildLps();
		for(Thread t : rankThreads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
	
	public static void main(String[] args) {
		ConnectToDB.establishConnection();
		double startTime  = (double)System.nanoTime();
		List<TextSearchResult> l = PhraseSearch.search("I can't breathe");
		System.out.println("finished");
		double endTime  = (double)System.nanoTime();
		double totalTime = (endTime - startTime)* (1e-9);
		System.out.println(totalTime);
		for(TextSearchResult r : l) {
			System.out.println(r.getUrl());
		}
	}
}
