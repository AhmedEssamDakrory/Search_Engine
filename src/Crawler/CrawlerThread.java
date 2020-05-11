import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  
import org.jsoup.nodes.Element;  
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;

public class CrawlerThread extends Thread {
	
	LinkedBlockingQueue<String> linkedQueue;
	Map<String, Boolean> concMap;
	private AtomicInteger pagesCount;
	
	public CrawlerThread(LinkedBlockingQueue<String> linkedQueue, Map<String, Boolean> concMap, AtomicInteger pagesCount) {
		this.linkedQueue = linkedQueue;
		this.concMap = concMap;
		this.pagesCount = pagesCount;
	}
	
	public void run() {
		this.Start();
	}
	
	public void Start()  {
		while(true) {
			
			String url = linkedQueue.poll();
			
			// check if null 
			if(url == null){
				continue;
			}
			
			int currentPagesCount = this.pagesCount.incrementAndGet();	
			if(currentPagesCount > Constants.MAX_CRAWLED_PAGES) {
				return;
			}
			
			System.out.println(url);
		
			try {
				Document doc = Jsoup.connect(url).get();
				String docContentType = doc.documentType().name();
				if(docContentType.equals("html")) {
					this.extractLinks(doc);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
	}
	
	
	private void extractLinks(Document doc) throws IOException {
        Elements links = doc.select("a[href]");
        for (Element link : links) {  
            String extractedUrl = link.attr("href");
            extractedUrl = this.normalizeUrl(extractedUrl);
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
		// check if not absolute address..
		if (URI.create(url).getHost() == null ) {
			return false;
		}
		// To Do check robots.txt...
		return true;
	}
	
	private String normalizeUrl(String url) {
		 return URI.create(url).normalize().toString();
	}
}
