package main.endpoints.search;

import main.model.TextSearchResult;
import main.ranker.Ranker;

import java.util.List;

public class TextSearch extends Search<TextSearchResult> {

    public TextSearch() {
        super(true);
    }

    @Override
    public List<TextSearchResult> rank(List<String> words, String country, String user) {
        return Ranker.rankText(words);
    }

    @Override
    public List<TextSearchResult> phraseSearch(String phrase, String country, String user) {
        return super.phraseSearch(phrase, country, user);
    }

    @Override
    public void describe(List<TextSearchResult> textSearchResults) {
    }
}