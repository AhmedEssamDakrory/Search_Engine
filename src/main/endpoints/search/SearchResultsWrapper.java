package main.endpoints.search;

import java.util.List;

public class SearchResultsWrapper<Result> {

    private static int resultsPerPage;
    private List<Result> results;
    private ResultsDescriber<Result> describer;
    private boolean[] describedBefore;

    SearchResultsWrapper(List<Result> results, ResultsDescriber<Result> describer) {
        this.results = results;
        this.describer = describer;
        describedBefore = new boolean[(int) Math.ceil((double) results.size() / resultsPerPage)];
    }

    static void setResultsPerPage(int resultsPerPage) {
        SearchResultsWrapper.resultsPerPage = resultsPerPage;
    }

    List<Result> page(int pageNumber) {
        int startIndex = (pageNumber - 1) * resultsPerPage;
        int endIndex = pageNumber * resultsPerPage;
        List<Result> pageResults = results.subList(startIndex, Math.min(endIndex, results.size()));
        if (describedBefore[pageNumber]) return pageResults;
        describer.describe(pageResults);
        describedBefore[pageNumber] = true;
        return pageResults;
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    interface ResultsDescriber<Result> {
        void describe(List<Result> results);
    }
}
