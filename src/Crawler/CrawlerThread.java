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


public class CrawlerThread extends Thread {
	
	LinkedBlockingQueue<String> linkedQueue;
	private AtomicInteger pagesCount;
	private boolean stopExtracting;
	AtomicInteger pagesInQueue;
	RobotsChecker robotsChecker;
	ExecutorService es ;
	
	public CrawlerThread(LinkedBlockingQueue<String> linkedQueue,AtomicInteger pagesCount,
			AtomicInteger pagesInQueue, RobotsChecker robotsChecker) {
		this.linkedQueue = linkedQueue;
		this.pagesCount = pagesCount;
		this.stopExtracting = false;
		this.pagesInQueue = pagesInQueue;
		this.robotsChecker = robotsChecker;
		es = Executors.newCachedThreadPool();
	}
	
	public void run() {
		this.Start();
	}
	
	public void Start()  {
		while(true) {
			String url = linkedQueue.poll();
			if(this.pagesCount.get() >= Constants.MAX_CRAWLED_PAGES) break;
			// check if blank URL 
			if(url == null){
				System.out.println("Leeeeeeeeeeeeeeeeh!");
				continue;
			}
	
			try {
				Document doc = Jsoup.connect(url).get();
				String docContentType = doc.documentType().name();
				if(docContentType.equals("html")) {
					//download and save page content
					System.out.println(this.pagesCount);
					if(this.isTerminateCondition()) {
						break;
					}
					this.downloadPage(doc, ConnectToDB.getCrawledUrlID(url));
					if(!this.stopExtracting) {
						this.extractLinks(doc);
					}
					ConnectToDB.markUrlAsCrawled(url);
				}
			} catch (IOException e) {
				//e.printStackTrace();
			} catch (NullPointerException e) {
				//
			}
		}
		es.shutdown();
		try {
			boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
	
	void downloadPage(Document doc, String fileName) {
		es.execute(new Runnable() { 
			public void run() {
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter("pages/"+fileName+".html"));
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
            extractedUrl = UrlNormalizer.getNormalizedURL(extractedUrl);
            if(extractedUrl == null) continue;           
            if(this.urlTest(extractedUrl)) {
            	int curPagesCount = this.pagesInQueue.getAndIncrement();
            	if(curPagesCount > Constants.MAX_CRAWLED_PAGES) {
            		this.stopExtracting = true;
            		break;
            	}
            	linkedQueue.offer(extractedUrl);
            }
            ConnectToDB.incUrlsPopularity(extractedUrl);
        }
        
	}
	
	
	private boolean urlTest(String url) {
		// check robots.txt...
		if(!this.robotsChecker.isUrlAllowed(url)) {
			return false;
		}
		// check if not visited before..
		synchronized(this.robotsChecker) {
			if(ConnectToDB.checkIfCrawledBefore(url)) {
				LogOutput.printMessage("URL Crawled Before : " + url);
				return false;
			}  else {
				ConnectToDB.insertUrlToBeCrawled(url);
			}
		}
		return true;
	}
}
