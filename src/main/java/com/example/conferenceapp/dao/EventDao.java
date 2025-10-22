package com.example.conferenceapp.dao;

import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventDao {

    public List<Event> find(String directionFilter, LocalDate dateFilter) {
        List<Event> list = new ArrayList<>();

        String sql = """
            SELECT e.id,
                   e.title,
                   d.name            AS dir_name,
                   e.start_datetime,
                   e.logo,
                   g.name            AS city,
                   u.full_name       AS organizer,
                   e.description
            FROM event e
            JOIN direction d ON e.direction_id  = d.id
            JOIN city      g ON e.city_id       = g.id
            LEFT JOIN user u ON e.organizer_id  = u.id
            WHERE (? IS NULL OR d.name = ?)
              AND (? IS NULL OR DATE(e.start_datetime) = ?)
            ORDER BY e.start_datetime
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, directionFilter);
            ps.setString(2, directionFilter);

            if (dateFilter != null) {
                ps.setDate(3, Date.valueOf(dateFilter));
                ps.setDate(4, Date.valueOf(dateFilter));
            } else {
                ps.setNull(3, Types.DATE);
                ps.setNull(4, Types.DATE);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Event(
                            rs.getInt   ("id"),
                            rs.getString("title"),
                            rs.getString("dir_name"),
                            rs.getTimestamp("start_datetime")
                                    .toLocalDateTime(),
                            rs.getString("logo"),
                            rs.getString("city"),
                            rs.getString("organizer"),
                            rs.getString("description")
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();       // замените на логгер при желании
        }
        return list;
    }
}
