import opennlp.tools.stemmer.PorterStemmer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryProcessor {

    private Set<String> stopWords;
    private PorterStemmer stemmer;

    public QueryProcessor() {
        initStopWords();
        stemmer = new PorterStemmer();
    }

    public static void main(String[] args) {
        System.out.println(new File(".").getAbsolutePath());
        QueryProcessor queryProcessor = new QueryProcessor();
        List<String> words = queryProcessor.process("I like watching very popular movies");
        for (String word : words)
            System.out.println(word);
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
            BufferedReader reader = new BufferedReader(new FileReader("data/stop_words.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
