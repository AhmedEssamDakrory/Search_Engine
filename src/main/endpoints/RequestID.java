package main.endpoints;

import main.utilities.ConnectToDB;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestID extends HttpServlet {

    public RequestID() {
        ConnectToDB.establishConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().println(ConnectToDB.requestUserID());
    }
}