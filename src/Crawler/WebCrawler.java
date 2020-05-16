import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * To Do .....
 * database ....  
 * recrawling....
 * check images .....
 */

public class WebCrawler {

	LinkedBlockingQueue<String> linkedQueue;
	ConcurrentHashMap<String, Boolean> concMap;
	List<Thread> threads;
	private AtomicInteger pagesCount;
	RobotsChecker robotsChecker;
	
	
	public WebCrawler() throws IOException {
		concMap = new ConcurrentHashMap<String, Boolean>();
		linkedQueue = new LinkedBlockingQueue<String>();
		threads = new ArrayList<Thread>();
		this.pagesCount = new AtomicInteger(0);
		this.robotsChecker = new RobotsChecker();
	}
	
	private void initQueueWithSeededUrls() throws IOException {
		File file = new File("seeders.txt");   
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
			Thread t = new Thread(new CrawlerThread(this.linkedQueue, this.concMap,
					this.pagesCount, this.robotsChecker));
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
		WebCrawler crawler = new WebCrawler();
		double startTime  = (double)System.nanoTime();
		crawler.startCawling(20);
		double endTime  = (double)System.nanoTime();
		double totalTime = (endTime - startTime)* (1e-9);
		System.out.println("finished");
		System.out.println(totalTime);
		
	}

}
