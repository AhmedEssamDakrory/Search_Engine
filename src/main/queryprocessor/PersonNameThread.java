package main.queryprocessor;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import main.utilities.ConnectToDB;

import java.util.List;

public class PersonNameThread extends Thread {

    private AbstractSequenceClassifier<CoreLabel> classifier;
    private String country;
    private String query;
    private PersonNameListener listener;

    PersonNameThread(AbstractSequenceClassifier<CoreLabel> classifier, String country, String query, PersonNameListener listener) {
        this.classifier = classifier;
        this.country = country;
        this.query = query;
        this.listener = listener;
    }

    @Override
    public void run() {
        if (classifier == null) return;
        String name = extractPersonName();
        if (name == null) return;
        listener.onPersonNameExtracted(country, name);
    }

    private String extractPersonName() {
        StringBuilder personName = new StringBuilder();
        List<List<CoreLabel>> sentences = classifier.classify(query);
        for (List<CoreLabel> sentence : sentences) {
            for (CoreLabel label : sentence) {
                boolean person = label.get(CoreAnnotations.AnswerAnnotation.class).equals("PERSON");
                if (person) {
                    if (personName.length() != 0) personName.append(" ");
                    personName.append(capitalize(label.word()));
                } else if (personName.length() != 0) {
                    return personName.toString();
                }
            }
            if (personName.length() != 0) return personName.toString();
        }
        return null;
    }

    private static String capitalize(String name) {
        if (name == null) return null;
        if (name.length() == 1) return name.toUpperCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public interface PersonNameListener {
        void onPersonNameExtracted(String country, String name);
    }
}
