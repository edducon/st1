package com.example.conferenceapp.dao;

import com.example.conferenceapp.model.Activity;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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

    public List<Event> findByOrganizer(int organizerId, String direction, LocalDate date) {
        List<Event> list = new ArrayList<>();
        String sql = """
            SELECT e.id,
                   e.title,
                   e.direction_id,
                   d.name AS direction,
                   e.start_datetime,
                   e.end_datetime,
                   e.city_id,
                   c.name AS city,
                   e.organizer_id,
                   u.full_name AS organizer,
                   e.logo,
                   e.description
              FROM event e
              JOIN direction d ON d.id = e.direction_id
              LEFT JOIN city c ON c.id = e.city_id
              LEFT JOIN user u ON u.id = e.organizer_id
             WHERE e.organizer_id = ?
               AND (? IS NULL OR d.name = ?)
               AND (? IS NULL OR DATE(e.start_datetime) = ?)
             ORDER BY e.start_datetime
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, organizerId);
            ps.setString(2, direction);
            ps.setString(3, direction);

            if (date != null) {
                ps.setDate(4, Date.valueOf(date));
                ps.setDate(5, Date.valueOf(date));
            } else {
                ps.setNull(4, Types.DATE);
                ps.setNull(5, Types.DATE);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Event(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("direction_id"),
                            rs.getString("direction"),
                            rs.getTimestamp("start_datetime").toLocalDateTime(),
                            rs.getTimestamp("end_datetime").toLocalDateTime(),
                            (Integer) rs.getObject("city_id"),
                            rs.getString("city"),
                            (Integer) rs.getObject("organizer_id"),
                            rs.getString("organizer"),
                            rs.getString("logo"),
                            rs.getString("description")
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public int insert(Event event) {
        String sql = """
            INSERT INTO event(title, direction_id, start_datetime, end_datetime, city_id, organizer_id, logo, description)
            VALUES(?,?,?,?,?,?,?,?)
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, event.getTitle());
            ps.setInt(2, event.getDirectionId());
            ps.setTimestamp(3, Timestamp.valueOf(event.getStart()));
            ps.setTimestamp(4, Timestamp.valueOf(event.getEnd()));

            if (event.getCityId() != null) {
                ps.setInt(5, event.getCityId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            if (event.getOrganizerId() != null) {
                ps.setInt(6, event.getOrganizerId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setString(7, event.getLogoPath());
            ps.setString(8, event.getDescription());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public void updateEndDateTime(int eventId, LocalDateTime end) {
        String sql = "UPDATE event SET end_datetime = ? WHERE id = ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(end));
            ps.setInt(2, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void bulkInsertActivities(int eventId, Collection<Activity> activities) {
        ActivityDao activityDao = new ActivityDao();
        for (Activity activity : activities) {
            activity.setId(0);
            activityDao.insert(new Activity(eventId,
                    activity.getTitle(),
                    activity.getDayNum(),
                    activity.getStartTime(),
                    activity.getEndTime()));
        }
    }
}
