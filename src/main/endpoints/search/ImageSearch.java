package main.endpoints.search;

import main.model.ImageSearchResult;
import main.ranker.Ranker;

import java.util.List;

public class ImageSearch extends Search<ImageSearchResult> {
    @Override
    public List<ImageSearchResult> rank(List<String> words, String country, String user) {
        return Ranker.rankImages(words);
    }

    @Override
    public void describe(List<ImageSearchResult> imageSearchResults) {
    }
}