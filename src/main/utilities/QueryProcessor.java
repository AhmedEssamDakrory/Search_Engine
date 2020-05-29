package main.utilities;

import opennlp.tools.stemmer.PorterStemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryProcessor {

    private Set<String> stopWords;
    private PorterStemmer stemmer;
    private static QueryProcessor instance;

    private QueryProcessor() {
        initStopWords();
        stemmer = new PorterStemmer();
    }

    public static QueryProcessor getInstance() {
        if (instance != null)
            return instance;
        instance = new QueryProcessor();
        return instance;
    }

    public String stem(String word) {
        return stemmer.stem(word);
    }

    public List<String> process(String query) {
        query = query.toLowerCase();
        query = query.replaceAll("[^0-9a-zA-Z]", " ");
        String[] words = query.split(" ");
        ArrayList<String> processedQuery = new ArrayList<>();
        for (String word : words) {
            if (word.trim().isEmpty() || stopWords.contains(word))
                continue;
            processedQuery.add(stem(word));
        }
        return processedQuery;
    }

    private void initStopWords() {
        stopWords = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.STOP_WORDS_PATH));
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
