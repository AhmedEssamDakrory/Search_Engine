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
    private LinkedHashMap<Request, List<TextSearchResult>> cache;

    public TextSearch() {
        ConnectToDB.establishConnection();
        cache = new LinkedHashMap<>();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        Request request = new Request(query);
        int page_number = Integer.parseInt(req.getParameter("page_number"));
        int per_page = Integer.parseInt(req.getParameter("per_page"));
        String message = new Gson().toJson(Ranker.page(search(request), page_number, per_page));
        resp.setContentType("application/json");
        resp.getWriter().println(message);
    }

    private List<TextSearchResult> search(Request request) {
        if (cache.containsKey(request)) {
            return cache.get(request);
        }
        if (cache.size() >= CACHE_LIMIT)
            cache.remove(cache.entrySet().iterator().next().getKey());
        QueryProcessor queryProcessor = QueryProcessor.getInstance();
        List<TextSearchResult> allResults = Ranker.rankText(queryProcessor.process(request.query));
        cache.put(request, allResults);
        return allResults;
    }

    private static class Request {
        String query;

        // TODO private int userID:
        public Request(String query) {
            this.query = query;
        }
    }
}