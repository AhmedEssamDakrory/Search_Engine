package main.phraseSearching;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import main.utilities.ConnectToDB;

public class PhraseSearchThread extends Thread {
	LinkedBlockingQueue<String> linkedQueue;
	ConcurrentHashMap<String,Boolean> found;
	KMP kmp;
	
	public PhraseSearchThread(LinkedBlockingQueue<String> linkedQueue, ConcurrentHashMap<String,Boolean> found,
			KMP kmp) {
		this.linkedQueue = linkedQueue;
		this.found = found;
		this.kmp = kmp;
	}
	
	public void run() {
		this.start();
	}
	
	public void start() {
		while(!linkedQueue.isEmpty()) {
			String url = linkedQueue.poll();
			String text = ConnectToDB.getText(url);
			if(text == null) continue;
			List<String> t = PhraseSearch.preparePhrase(text);
			if(kmp.match(t)) {
					found.put(url, true);
			}
		}
	}
}
