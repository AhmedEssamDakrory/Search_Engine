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
	public static final Integer MAX_SCORE = 1000;
	public static final Integer DESCRIPTION_RANGE = 10;
	public static final HashMap<String, Integer> SCORES = new HashMap<String, Integer>(){
		{
			put("title", 50);
			put("h1", 30);
			put("h2", 20);
			put("h3", 10);
			put("h4", 6);
			put("h5", 5);
			put("h6", 4);
			put("p", 1);
			put("strong", 4);
			put("em", 4);
			put("b", 3);
			put("i", 3);
			put("u", 3);

			put("a", 5);

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
