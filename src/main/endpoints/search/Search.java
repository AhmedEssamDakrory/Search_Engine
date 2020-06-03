package main.endpoints.search;

import com.google.gson.Gson;
import main.queryprocessor.PersonNameThread;
import main.queryprocessor.QueryProcessor;
import main.utilities.ConnectToDB;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class Search<Result> extends HttpServlet implements PersonNameThread.PersonNameListener, SearchResultsWrapper.ResultsDescriber<Result> {

    private static final int CACHE_LIMIT = 20;
    private LinkedHashMap<String, SearchResultsWrapper<Result>> cache;
    private boolean supportsPhraseSearch;
    private QueryProcessor queryProcessor;

    public Search(boolean supportsPhraseSearch) {
        this.supportsPhraseSearch = supportsPhraseSearch;
        cache = new LinkedHashMap<>();
        queryProcessor = QueryProcessor.getInstance();
        queryProcessor.loadClassifier();
        ConnectToDB.establishConnection();
    }

    public Search() {
        this(false);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        int page_number = Integer.parseInt(req.getParameter("page_number"));
        int per_page = Integer.parseInt(req.getParameter("per_page"));
        String country = req.getParameter("country");
        String user = req.getParameter("user");
        SearchResultsWrapper.setResultsPerPage(per_page);
        if (page_number == 1)
            queryProcessor.extractPersonName(country, query, this);
        SearchResultsWrapper<Result> allResults = search(query, country, user);
        if (!allResults.isEmpty()) ConnectToDB.addSuggestion(query);
        String message = new Gson().toJson(allResults.page(page_number));
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(message);
    }

    // TODO userID
    private SearchResultsWrapper<Result> search(String query, String country, String user) {
        String key = query + country + user;
        if (cache.containsKey(key)) return cache.get(key);
        if (cache.size() >= CACHE_LIMIT)
            cache.remove(cache.entrySet().iterator().next().getKey());
        List<Result> allResults;
        List<String> stemmedQueryWords = queryProcessor.process(query);
        if (supportsPhraseSearch && query.length() > 2 && query.startsWith("\"") && query.endsWith("\"")) {
            allResults = phraseSearch(query.substring(1, query.length() - 1), country, user);
        } else {
            allResults = rank(stemmedQueryWords, country, user);
        }
        SearchResultsWrapper<Result> resultsWrapper = new SearchResultsWrapper<>(allResults, stemmedQueryWords, this);
        cache.put(key, resultsWrapper);
        return resultsWrapper;
    }

    public List<Result> phraseSearch(String phrase, String country, String user) {
        return null;
    }

    public abstract List<Result> rank(List<String> words, String country, String user);

    @Override
    public void onPersonNameExtracted(String country, String name) {
        ConnectToDB.addPersonToTrends(country, name);
    }
}
