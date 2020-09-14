package ru.job4j.cinema.servlet;

import org.json.JSONObject;
import ru.job4j.cinema.store.Store;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PaymentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String message = "Что-то пошло не так...";
        Store store = (Store) getServletContext().getAttribute("store");
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        Integer customerId = (Integer) req.getSession().getAttribute("customerId");
        long orderId = Long.parseLong(req.getParameter("orderId"));
        double paid = Double.parseDouble(req.getParameter("paid"));
        if(paid == store.order(customerId, orderId, 1).getPrice()) {
            if (store.makePayment(orderId, customerId) > 0) {
                message = "Заказ успешно оплачен";
            }
        }
        PrintWriter writer = new PrintWriter(resp.getOutputStream());
        writer.println(new JSONObject(Map.of("message", message)));
        writer.flush();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Store store = (Store) getServletContext().getAttribute("store");
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = new PrintWriter(resp.getOutputStream());
        Integer customerId = (Integer) req.getSession().getAttribute("customerId");
        long orderId = Long.parseLong(req.getParameter("orderId"));
        writer.println(new JSONObject(store.order(customerId, orderId, 1)));
        writer.flush();
    }
}
