package main.phraseSearching;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.indexer.IndexingThread;
import main.utilities.ConnectToDB;
import main.utilities.Constants;

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
			//System.out.println(url);
			String pageName = ConnectToDB.getCrawledUrlID(url);
			String html = IndexingThread.readHtml(Constants.CRAWLED_WEB_PAGES_FILE_PATH + pageName + ".html");
			Document document = Jsoup.parse(html);
			if(this.matched(document)) {
					found.put(url, true);
			}
		}
	}
	
	private boolean matched(Document document) {
		for (String tagName : Constants.SCORES.keySet()) {
            Elements tagsText = document.getElementsByTag(tagName);
            for (Element tagText : tagsText) {
            	List<String> t = PhraseSearch.preparePhrase(tagText.text());
            	if(kmp.match(t)) {
            		return true;
            	}
            }
        }
		return false;
	}
}
