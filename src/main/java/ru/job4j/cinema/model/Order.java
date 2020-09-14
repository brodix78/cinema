package ru.job4j.cinema.model;

import java.util.List;

public class Order {
    long id;
    int sessionId;
    double price;
    List<String> placesDesc;

    public Order(long id, int sessionId, double price, List<String> placesDesc) {
        this.id = id;
        this.sessionId = sessionId;
        this.price = price;
        this.placesDesc = placesDesc;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<String> getPlacesDesc() {
        return placesDesc;
    }

    public void setPlacesDesc(List<String> placesDesc) {
        this.placesDesc = placesDesc;
    }
}
