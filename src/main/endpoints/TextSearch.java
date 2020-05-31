package main.endpoints;

import com.google.gson.Gson;
import main.model.TextSearchResult;
import main.ranker.Ranker;
import main.utilities.ConnectToDB;
import main.utilities.QueryProcessor;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public class TextSearch extends HttpServlet {

    private static final int CACHE_LIMIT = 20;
    private LinkedHashMap<String, List<TextSearchResult>> cache;

    public TextSearch() {
        ConnectToDB.establishConnection();
        cache = new LinkedHashMap<>();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        int page_number = Integer.parseInt(req.getParameter("page_number"));
        int per_page = Integer.parseInt(req.getParameter("per_page"));
        List<TextSearchResult> allResults = search(query, 0);
        if (!allResults.isEmpty()) ConnectToDB.addSuggestion(query);
        String message = new Gson().toJson(Ranker.page(allResults, page_number, per_page));
        resp.setContentType("application/json");
        resp.getWriter().println(message);
    }

    // TODO userID
    private List<TextSearchResult> search(String query, int userID) {
        String key = query + userID;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        if (cache.size() >= CACHE_LIMIT)
            cache.remove(cache.entrySet().iterator().next().getKey());
        QueryProcessor queryProcessor = QueryProcessor.getInstance();
        List<TextSearchResult> allResults = Ranker.rankText(queryProcessor.process(query));
        cache.put(key, allResults);
        return allResults;
    }
}