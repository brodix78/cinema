package ru.job4j.cinema.store;

import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.job4j.cinema.model.Order;
import ru.job4j.cinema.model.Place;
import ru.job4j.cinema.model.Session;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class PsqlStore implements Store {
    private final BasicDataSource pool = new BasicDataSource();

    public PsqlStore() {
        Properties cfg = new Properties();
        String[] init;
        try (BufferedReader pr = new BufferedReader(new FileReader("db.properties"))) {
            cfg.load(pr);
            init = new String(Files.readAllBytes(Paths.get("db/schema.sql"))).split(";");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        pool.setDriverClassName(cfg.getProperty("jdbc.driver"));
        pool.setUrl(cfg.getProperty("jdbc.url"));
        pool.setUsername(cfg.getProperty("jdbc.username"));
        pool.setPassword(cfg.getProperty("jdbc.password"));
        pool.setMinIdle(5);
        pool.setMaxIdle(10);
        pool.setMaxOpenPreparedStatements(100);
        initBase(init);
    }

    private void initBase(String[] init) {
        for (String line : init) {
            if (line.length() > 5) {
                try (Connection cn = pool.getConnection();
                     PreparedStatement ps = cn.prepareStatement(line)) {
                    ps.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public ArrayList<Place> getHall(int sessionId, int customerId) {
        Session session = sessionById(sessionId);
        ArrayList<Place> hall = new ArrayList<>();
        try (Connection cn = pool.getConnection();
             PreparedStatement psHall = cn.prepareStatement(
                     "SELECT * FROM halls WHERE id = ?")) {
            HashMap<String, Map<String, Integer>> reserved = reserved(sessionId);
            psHall.setInt(1, session.getHallId());
            try (ResultSet it = psHall.executeQuery()) {
                while (it.next()) {
                    String id = it.getString("placeId");
                    int status = 0;
                    if (reserved.containsKey(id)) {
                        if (reserved.get(id).get("customerId") == customerId
                                && reserved.get(id).get("status") == 1) {
                            status = 11;
                        } else {
                            status = reserved.get(id).get("status");
                        }
                    }
                    hall.add(new Place(id,it.getString("row"),
                            it.getString("place"), status, session.getPrice()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hall;
    }

    @Override
    public int addCustomer() {
        int customerId = -1;
        try (Connection cn = pool.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO customers(name) VALUES ('empty')",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.execute();
            try (ResultSet i = ps.getGeneratedKeys()) {
                if (i.next()) {
                    customerId = i.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerId;
    }

    @Override
    public void updateCustomer(int customerId, String name, String phone) {
        try (Connection cn = pool.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "UPDATE customers SET name = ?, phone =?  WHERE id = ?")) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setInt(3, customerId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Session sessionById(int sessionId) {
        Session session = new Session();
        try (Connection cn = pool.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT * FROM sessions WHERE id = ?")) {
            ps.setInt(1, sessionId);
            try (ResultSet it = ps.executeQuery()) {
                while (it.next()) {
                    session.setHallId(it.getInt("hallid"));
                    session.setTime(it.getLong("time"));
                    session.setPrice(it.getInt("price"));
                    session.setMovie(it.getString("movie"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return session;
    }

    /*
    returns reserved places for session with status (1 - reserved, 2 - paid)
    and kicks off unpaid reservations in 15 minutes
    */
    private HashMap<String, Map<String, Integer>> reserved(int sessionId) {
        HashMap<String, Map<String, Integer>> reserved = new HashMap<>();
        try (Connection cn = pool.getConnection();
             PreparedStatement psRsv = cn.prepareStatement(
                     "SELECT placeId, time, status, customerId FROM reservations WHERE sessionId = ?")
        ) {
            psRsv.setInt(1, sessionId);
            try (ResultSet it = psRsv.executeQuery()) {
                while (it.next()) {
                    int placeId = it.getInt("placeId");
                    long time = it.getLong("time");
                    int status = it.getInt("status");
                    int customerId = it.getInt("customerId");
                    if (status == 2 || (status == 1 && System.currentTimeMillis() - time < 15.5 * 60000)) {
                        reserved.put(it.getString("placeId"),
                                Map.of("status", it.getInt("status"),
                                        "customerId", it.getInt("customerId")));
                    } else {
                        try (PreparedStatement psRmv = cn.prepareStatement(
                                "DELETE FROM reservations where placeId = ? AND sessionId = ? AND time = ?")) {
                            psRmv.setInt(1, placeId);
                            psRmv.setInt(2, sessionId);
                            psRmv.setLong(3, time);
                            psRmv.execute();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reserved;
    }

    @Override
    public long makeReserve(int sessionId, List<Integer> placesId, int customerId) {
        long time = System.currentTimeMillis();
        double price = sessionById(sessionId).getPrice();
        Connection cn = null;
        Savepoint savepoint = null;
        boolean rsl = false;
        try {
            cn = pool.getConnection();
            cn.setAutoCommit(false);
            savepoint = cn.setSavepoint("SavepointOne");
            try (PreparedStatement psRmv = cn.prepareStatement(
                    "DELETE FROM reservations where customerId = ? AND sessionId = ? AND status = 1")) {
                psRmv.setInt(1, customerId);
                psRmv.setInt(2, sessionId);
                psRmv.execute();
            }
            for (int placeId : placesId) {
                PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO reservations VALUES(?, ?, ?, ?, ?, ?)");
                ps.setInt(1, sessionId);
                ps.setInt(2, placeId);
                ps.setInt(3, customerId);
                ps.setDouble(4, price);
                ps.setLong(5, time);
                ps.setInt(6, 1);
                ps.executeUpdate();
            }
            cn.commit();
            rsl = true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                cn.rollback(savepoint);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } finally {
            try {
                cn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return rsl ? time : -1;
    }

    @Override
    public long makePayment(long orderId, int customerId) {
        long time = System.currentTimeMillis();
        Connection cn = null;
        Savepoint savepoint = null;
        boolean rsl;
        try {
            cn = pool.getConnection();
            cn.setAutoCommit(false);
            savepoint = cn.setSavepoint("SavepointOne");
            PreparedStatement ps = cn.prepareStatement(
                    "UPDATE reservations SET status = 2, time = ? WHERE time = ? AND " +
                            "customerId = ? AND status = 1");
            ps.setLong(1, time);
            ps.setLong(2, orderId);
            ps.setInt(3, customerId);
            cn.commit();
            rsl = true;
        } catch (Exception e) {
            e.printStackTrace();
            rsl = false;
            try {
                cn.rollback(savepoint);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } finally {
            try {
                cn.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return rsl ? time : -1;
    }

    @Override
    public Order order(int customerId, long orderId, int status) {
        Order order = new Order(0, 0, 0, new ArrayList<>());
        try (Connection cn = pool.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT res.price, res.sessionId, hall.row, hall.place FROM reservations res, halls hall " +
                             "WHERE res.customerId = ? AND res.time = ? " +
                             "AND res.status = ? AND hall.placeId = res.placeId")
        ) {
            ps.setInt(1, customerId);
            ps.setLong(2, orderId);
            ps.setInt(3, status);
            try (ResultSet it = ps.executeQuery()) {
                ArrayList<String> placesDesc = new ArrayList<>();
                double price = 0;
                int sessionId = 0;
                while (it.next()) {
                    placesDesc.add(String.format("Ряд:%s Место:%s",it.getString("row")
                            , it.getString("place")));
                    price += it.getDouble("price");
                    sessionId = it.getInt("sessionId");
                }
                order = new Order(orderId, sessionId, price, placesDesc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return order;
    }
}
