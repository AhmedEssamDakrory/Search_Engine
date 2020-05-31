package main.endpoints;

import main.utilities.ConnectToDB;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Complete extends HttpServlet {

    public Complete() {
        ConnectToDB.establishConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("query");
        resp.setContentType("application/json");
        resp.getWriter().println(ConnectToDB.retrieveSuggestions(query));
    }
}