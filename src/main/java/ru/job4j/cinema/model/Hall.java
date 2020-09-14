package ru.job4j.cinema.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Hall {
    private LinkedHashMap<String, LinkedHashSet<String>> places;

    public LinkedHashMap<String, LinkedHashSet<String>> getPlaces() {
        return places;
    }

    public void setPlaces(LinkedHashMap<String, LinkedHashSet<String>> places) {
        this.places = places;
    }
}
