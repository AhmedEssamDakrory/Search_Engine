package main.crawler;

import main.utilities.ConnectToDB;
import main.utilities.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CrawlerThread extends Thread {
	
	LinkedBlockingQueue<String> linkedQueue;
	private AtomicInteger pagesCount;
	private AtomicInteger numExtractedLinks;
	ConcurrentHashMap<String,Boolean> isVisited;
	RobotsChecker robotsChecker;
	boolean stopExtracting;
	ExecutorService es ;
	
	/**
	 * Initializing all data members with parameters sent form the WebCrawler Class. 
	 * These parameters Common in all threads. 
	 * 
	 * @param linkedQueue
	 * @param isVisited
	 * @param numExtractedLinks
	 * @param pagesCount
	 * @param robotsChecker
	 */
	public CrawlerThread(LinkedBlockingQueue<String> linkedQueue, ConcurrentHashMap<String,Boolean> isVisited, 
			AtomicInteger numExtractedLinks, AtomicInteger pagesCount, RobotsChecker robotsChecker) {
		this.linkedQueue = linkedQueue;
		this.pagesCount = pagesCount;
		this.numExtractedLinks = numExtractedLinks; 
		this.isVisited = isVisited;
		this.stopExtracting = false;
		this.robotsChecker = robotsChecker;
		es = Executors.newCachedThreadPool();
	}

	public void run() {
		this.Start();
	}
	
	/**
	 * Start Crawling thread logic.
	 */
	public void Start()  {
		while(true) {
			String url = linkedQueue.poll();
			if(this.pagesCount.get() >= Constants.MAX_CRAWLED_PAGES) break;
			// check if blank URL 
			if(url == null){
				continue;
			} else if(!this.robotsChecker.isUrlAllowed(url)) { // check if URL allowed by robots.
				ConnectToDB.deleteUrl(url); // if not allowed delete it from database
				continue;
			}
	
			try {
				Document doc = Jsoup.connect(url).get();
				String docContentType = doc.documentType().name();
				if(docContentType.equals("html")) {
					System.out.println(this.pagesCount);
					if(this.isTerminateCondition()) {
						break;
					}
					//download and save page content
					this.downloadPage(doc, ConnectToDB.getCrawledUrlID(url));
					ConnectToDB.markUrlAsCrawled(url); // mark URL as crawled in database.
					if(!this.stopExtracting) {
						this.extractLinks(url, doc);
					}
					
				}
			} catch (IOException e) {
				
			} catch (NullPointerException e) {
				
			}
		}
		// wait for all running threads to download pages and finish.
		es.shutdown();
		try {
			boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}
	
	/**
	 * Take HTML document and download it by writing it in HTML file with the sent file name.
	 * The path to save the file exists in the Constants Class. "Constants.CRAWLED_WEB_PAGES_FILE_PATH".
	 * Downloading occurs in a thread.
	 *  
	 * @param doc
	 * @param fileName
	 */
	void downloadPage(Document doc, String fileName) {
		// Create thread for downloading
		es.execute(new Runnable() { 
			public void run() {
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.CRAWLED_WEB_PAGES_FILE_PATH+fileName+".html"));
					writer.write(doc.html());
					writer.close();
				} catch (IOException e) {
					//System.out.println("failed to download");
				}
			}
		});
	}
	
	/**
	 * Check if we reached a terminate condition.
	 * Return true if the crawled pages number equals the max number of pages we crawl at a time.  
	 * @return
	 */
	boolean isTerminateCondition() {
		int currentPagesCount = this.pagesCount.incrementAndGet();	
		if(currentPagesCount > Constants.MAX_CRAWLED_PAGES) {
			return true;
		}
		return false;
	}
	
	/**
	 * Extract absolute links form an HTML document.
	 * Save the outgoing links for this document in the database (associated with the document URL).
	 *  
	 * @param url
	 * @param doc
	 * @throws IOException
	 */
	private void extractLinks(String url, Document doc) throws IOException {
        Elements links = doc.select("a[href]");
        for (Element link : links) {  
            String extractedUrl = link.attr("abs:href");
            // Normalizing URLs to make each URL unique.
            extractedUrl = UrlNormalizer.getNormalizedURL(extractedUrl);
            if(extractedUrl == null) continue;  
            if(this.checkifNotVisited(extractedUrl)) {
            	if(this.numExtractedLinks.getAndIncrement() >= Constants.MAX_LINKS_TO_EXTRACT) {
                	this.stopExtracting = true; // stop extracting if reached the maximum number of extracted URLs at a time.  
                	break;
                }
            	linkedQueue.offer(extractedUrl);
            }
			ConnectToDB.addOutgoingLink(url, extractedUrl);
        }
        
	}
	
	/**
	 * Check if the URL visited before.
	 * Return false if visited before.
	 * First check if the URL in the hash map (isVisited), if not go check the database and save it in the hash map,
	 * this trick is done to hit the database only once for each unique URL to increase performance. 
	 * 
	 * @param url
	 * @return
	 */
	private boolean checkifNotVisited(String url) {
		synchronized(this.robotsChecker) {
			if(this.isVisited.get(url) != null){
				LogOutput.printMessage("URL Crawled Before : " + url);
				return false;
			}
			else if(ConnectToDB.checkIfCrawledBefore(url)) {
				LogOutput.printMessage("URL Crawled Before : " + url);
				this.isVisited.put(url, true);
				return false;
			}  else {
				this.isVisited.put(url, true);
				ConnectToDB.insertUrlToBeCrawled(url);
				return true;
			}
		}
	}

}
