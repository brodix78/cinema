package ru.job4j.cinema.store;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.job4j.cinema.model.Order;
import ru.job4j.cinema.model.Place;

import java.util.List;
import java.util.Map;

public interface Store {
    long makeReserve(int sessionId, List<Integer> placesId, int customerId);
    long makePayment(long orderId, int customerId);
    Order order(int customerId, long orderId, int status);
    List<Place> getHall(int sessionId, int customerId);
    int addCustomer();
    void updateCustomer(int customerId, String name, String phone);
}
