package main.endpoints;

import main.model.TextSearchResult;
import main.ranker.Ranker;

import java.util.List;

public class TextSearch extends Search<TextSearchResult> {

    @Override
    public List<TextSearchResult> rank(List<String> words, String country, String user) {
        return ranker.rankText(words, country);
    }
}