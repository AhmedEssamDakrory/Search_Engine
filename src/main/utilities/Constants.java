package main.utilities;

import java.util.HashMap;

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
	public static final Integer EXTRA_IMAGE_WORDS = 10;
	public static final Integer CAPTION_SCORE = 100;
	public static final HashMap<String, Integer> SCORES = new HashMap<String, Integer>(){
		{
			put("title", 20);
			put("h1", 10);
			put("h2", 8);
			put("h3", 6);
			put("h4", 4);
			put("h5", 3);
			put("h6", 2);
			put("p", 1);
			put("strong", 2);
			put("em", 2);
			put("b", 1);
			put("i", 1);
			put("u", 1);

			put("a", 2);

//			put("img", 2);	// attribute alt

			// only when name is one of:
			//		description
			//		keywords
//			put("meta", 5);	// attribute content
		}
	};
	//--------Query Processor--------
	public static final String STOP_WORDS_PATH = "data/stop_words.txt";
}
