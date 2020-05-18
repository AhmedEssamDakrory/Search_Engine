import java.util.HashMap;

public class Constants {
	public static final int MAX_CRAWLED_PAGES = 5000;
	public static final int MAX_LINKS_TO_EXTRACT = (int) 1e4;
	public static final int MAX_NUM_HOSTS = 100;
	public static final int MAX_POLL_TIME = 10000;
	public static final String CRAWLED_WEB_PAGES_FILE_PATH = "pages/";
	public static final String CRAWLING_SEEDER_FILE = "crawler_seeders.txt";
	public static final String AGENT = "*";
	
	//---------Indexer---------
	public static final String DATABASE_ADDRESS = "mongodb://localhost:27017";
	public static final String DATABASE_NAME = "search_engine";
	public static final HashMap<String, Integer> SCORES = new HashMap<String, Integer>(){
		{
			put("title", 20);
			put("h1", 4);
			put("h2", 2);
			put("p", 1);
		}
	};
}
