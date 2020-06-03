package main.queryprocessor;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import main.utilities.Constants;
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
    private AbstractSequenceClassifier<CoreLabel> classifier;
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
        try {
            return stemmer.stem(word);
        } catch (ArrayIndexOutOfBoundsException ignore) {
            return null;
        }
    }

    public List<String> process(String query) {
        query = query.toLowerCase();
        query = query.replaceAll("[^0-9a-zA-Z]", " ");
        String[] words = query.split(" ");
        ArrayList<String> processedQuery = new ArrayList<>();
        for (String word : words) {
            if (word.trim().isEmpty() || stopWords.contains(word))
                continue;
            String stemmedWord = stem(word);
            if (stemmedWord != null)
                processedQuery.add(stem(word));
        }
        return processedQuery;
    }

    public String processWord(String word) {
        if (word.trim().isEmpty() || stopWords.contains(word))
            return null;
        return stem(word);
    }

    public void extractPersonName(String country, String query, PersonNameThread.PersonNameListener listener) {
        new PersonNameThread(classifier, country, query, listener).start();
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

    public void loadClassifier() {
        if (classifier != null) return;
        String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";
        try {
            classifier = CRFClassifier.getClassifier(serializedClassifier);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
