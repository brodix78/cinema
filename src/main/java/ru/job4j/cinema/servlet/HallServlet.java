package ru.job4j.cinema.servlet;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.job4j.cinema.store.Store;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;


public class HallServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Store store = (Store) getServletContext().getAttribute("store");
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        JSONObject orderId = new JSONObject();
        String sessionId = req.getParameter("sessionId");
        String status = req.getParameter("status");
        Integer customerId = (Integer) req.getSession().getAttribute("customerId");
        if (status != null && customerId != null) {
            if ("1".equals(status) && sessionId != null) {
                String[] places = req.getParameterValues("place[]");
                if (places != null) {
                    ArrayList<Integer> forReserve = new ArrayList<>();
                    for (String place : places) {
                        forReserve.add(Integer.parseInt(place));
                    }
                    orderId = new JSONObject(Map.of("orderId",
                            store.makeReserve(Integer.parseInt(sessionId), forReserve, customerId)));
                } else {
                    orderId = new JSONObject(Map.of("orderId", -1));
                }
            }
        }
        PrintWriter writer = new PrintWriter(resp.getOutputStream());
        writer.println(orderId);
        writer.flush();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Store store = (Store) getServletContext().getAttribute("store");
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = new PrintWriter(resp.getOutputStream());
        Integer customerId = (Integer) req.getSession().getAttribute("customerId");
        writer.println(new JSONArray(store.getHall(Integer.parseInt(req.getParameter("sessionId")),
                customerId == null ? 0 : customerId)));
        writer.flush();
    }
}
