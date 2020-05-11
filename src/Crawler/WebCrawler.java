import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * To Do .....
 * try web utilities.java instead of Jsoup (time out) ...
 * data base ....  
 * Robots.txt
 * recrawling....
 * check images .....
 */

public class WebCrawler {

	LinkedBlockingQueue<String> linkedQueue;
	Map<String, Boolean> concMap;
	List<Thread> threads;
	private AtomicInteger pagesCount;
	
	
	public WebCrawler() throws IOException {
		concMap = new ConcurrentHashMap<String, Boolean>();
		linkedQueue = new LinkedBlockingQueue<String>();
		threads = new ArrayList<Thread>();
		this.pagesCount = new AtomicInteger(0);
	}
	
	private void initQueueWithSeededUrls() throws IOException {
		File file = new File("data/Crawler_seeded_urls.txt");
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		String st; 
		while ((st = br.readLine()) != null) 
			linkedQueue.offer(st);
		br.close();
	}
	
	public void startCawling(int numberOfThreads) throws IOException {
		// initialize...
		this.initQueueWithSeededUrls();
		// start threads.. 
		for(int i = 0 ;i < numberOfThreads; ++i) {
			Thread t = new Thread(new CrawlerThread(this.linkedQueue, this.concMap, this.pagesCount));
			t.start();
			threads.add(t);
		}
		
		this.waitForThreadsToFinish();
	}
	
	private void waitForThreadsToFinish() {
		for(Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		double startTime = (double)System.nanoTime();
		WebCrawler crawler = new WebCrawler();
		crawler.startCawling(20);
		double endTime  = (double)System.nanoTime();
		double totalTime = (endTime - startTime)* (1e-9);
		System.out.println("finished");
		System.out.println(totalTime);
	}

}
