import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Constants {
	public static final int MAX_CRAWLED_PAGES = 5000;
	public static final int MAX_LINKS_TO_EXTRACT = (int) 1e4;
	public static final int MAX_NUM_HOSTS = 100;
	public static final int MAX_POLL_TIME = 10000;
	public static final String CRAWLED_WEB_PAGES_FILE_PATH = "data/pages/";
	public static final String CRAWLING_SEEDER_FILE = "data/crawler_seeders.txt";
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
	public static final Set<String> STOP_WORDS = new HashSet<String>(){
		{
			add("I"); 	add("a"); 	add("about");
			add("an");  add("ar");	add("as");
			add("at");  add("be");  add("by");
			add("com"); add("for"); add("from");
			add("how"); add("of"); 	add("the");
			add("in");  add("is"); 	add("it");
			add("on");  add("or"); 	add("that");
			add("to");	add("wa"); add("what");
			add("thi");add("when");add("where");
		}
	};
}
