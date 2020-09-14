package ru.job4j.cinema.servlet.listener;

import ru.job4j.cinema.store.PsqlStore;
import ru.job4j.cinema.store.Store;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class InitContext implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Store store = new PsqlStore();
        sce.getServletContext().setAttribute("store", store);
    }
}