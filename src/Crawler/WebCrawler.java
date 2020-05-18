import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawler {

	LinkedBlockingQueue<String> linkedQueue;
	List<Thread> threads;
	private AtomicInteger pagesCount;
	private AtomicInteger pagesInQueue;
	RobotsChecker robotsChecker;
	
	
	public WebCrawler() throws IOException {
		linkedQueue = new LinkedBlockingQueue<String>();
		threads = new ArrayList<Thread>();
		this.pagesCount = new AtomicInteger(0);
		pagesInQueue = new AtomicInteger(0);
		this.robotsChecker = new RobotsChecker();
		LogOutput.init();
	}
	
	private void initQueueWithSeededUrls() throws IOException {
		List<String> seeders = ConnectToDB.getAllNotCrawledUrls();
		if(seeders.size() == 0) {
			System.out.println("Crawling for the first time...");
			File file = new File("seeders.txt");   
			BufferedReader br = new BufferedReader(new FileReader(file)); 
			String st; 
			while ((st = br.readLine()) != null) {
				linkedQueue.offer(st);
				seeders.add(st);
			}
			ConnectToDB.seededUrlsToCrawl(seeders);
			br.close();
		} else {
			System.out.println("Recrawling....");
			for(String url : seeders) {
				linkedQueue.offer(url);
			}
		}
		
		
	}
	
	public void startCrawling(int numberOfThreads) throws IOException {
		// initialize...
		this.initQueueWithSeededUrls();
		// start threads.. 
		for(int i = 0 ;i < numberOfThreads; ++i) {
			Thread t = new Thread(new CrawlerThread(this.linkedQueue,this.pagesCount,
					this.pagesInQueue, this.robotsChecker));
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

		ConnectToDB.establishConnection("search_engine", "team", "12345");
		ConnectToDB.clearDB();
		ConnectToDB.init();
	
		WebCrawler crawler = new WebCrawler();
		double startTime  = (double)System.nanoTime();
		crawler.startCrawling(20);
		double endTime  = (double)System.nanoTime();
		double totalTime = (endTime - startTime)* (1e-9);
		System.out.println("finished");
		System.out.println(totalTime);
		
	}

}
