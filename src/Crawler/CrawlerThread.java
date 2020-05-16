import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  
import org.jsoup.nodes.Element;  
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class CrawlerThread extends Thread {
	
	LinkedBlockingQueue<String> linkedQueue;
	ConcurrentHashMap<String, Boolean> concMap;
	private AtomicInteger pagesCount;
	RobotsChecker robotsChecker;
	ExecutorService es ;
	
	public CrawlerThread(LinkedBlockingQueue<String> linkedQueue, ConcurrentHashMap<String, Boolean> concMap,
			AtomicInteger pagesCount, RobotsChecker robotsChecker) {
		this.linkedQueue = linkedQueue;
		this.concMap = concMap;
		this.pagesCount = pagesCount;
		this.robotsChecker = robotsChecker;
		es = Executors.newCachedThreadPool();
	}
	
	public void run() {
		this.Start();
	}
	
	public void Start()  {
		while(true) {
			String url = linkedQueue.poll();
			
			// check if blank URL 
			if(url == null){
				continue;
			}
	
			try {
				Document doc = Jsoup.connect(url).get();
				String docContentType = doc.documentType().name();
				if(docContentType.equals("html")) {
					//download and save page content
					if(this.isTerminateCondition()) {
						break;
					}
					System.out.println(this.pagesCount);
					this.downloadPage(doc);
					this.extractLinks(doc);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		es.shutdown();
		try {
			boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
	
	void downloadPage(Document doc) {
		es.execute(new Runnable() { 
			public void run() {
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(Integer.toString(pagesCount.get())+".html"));
					writer.write(doc.html());
					writer.close();
				} catch (IOException e) {
					//System.out.println("failed to download");
				}
			}
		});
	}
	
	boolean isTerminateCondition() {
		int currentPagesCount = this.pagesCount.incrementAndGet();	
		if(currentPagesCount > Constants.MAX_CRAWLED_PAGES) {
			return true;
		}
		return false;
	}
	
	private void extractLinks(Document doc) throws IOException {
        Elements links = doc.select("a[href]");
        for (Element link : links) {  
            String extractedUrl = link.attr("abs:href");
            extractedUrl = this.normalizeUrl(extractedUrl);
            if(extractedUrl == null) continue;
            if(this.urlTest(extractedUrl)) {
            	linkedQueue.offer(extractedUrl);
            }
        }  
	}
	
	
	private boolean urlTest(String url) {
		// check if not visited before..
		synchronized(this.concMap) {
			if(this.concMap.get(url) != null) {
				return false;
			}
		}
		// check robots.txt...
		return this.robotsChecker.isUrlAllowed(url);
	}
	
	private String normalizeUrl(String url) {
		String s = null;
		try {
			s = URI.create(url).normalize().toString();
		} catch(java.lang.IllegalArgumentException e) {
			//
		}
		return s;
	}
}
