package com.example.conferenceapp.dao;

import com.example.conferenceapp.model.ModeratorSlot;
import com.example.conferenceapp.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ModeratorDao {

    public List<ModeratorSlot> loadSlots(int moderatorId, String direction, Integer eventId) {
        String sql = """
            SELECT a.id,
                   a.title,
                   e.title          AS event_title,
                   d.name           AS direction_name,
                   DATE(e.start_datetime) + INTERVAL (a.day_num-1) DAY AS activity_date,
                   a.start_time,
                   a.end_time,
                   ma.status,
                   CASE WHEN a.moderator_id = ? THEN 'APPROVED' END AS direct_status
              FROM activity a
              JOIN event e      ON e.id = a.event_id
              JOIN direction d  ON d.id = e.direction_id
              LEFT JOIN moderator_application ma
                     ON ma.activity_id = a.id AND ma.moderator_id = ?
             WHERE (? IS NULL OR d.name = ?)
               AND (? IS NULL OR e.id = ?)
             ORDER BY activity_date, a.start_time
        """;

        List<ModeratorSlot> slots = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, moderatorId);
            ps.setInt(2, moderatorId);
            ps.setString(3, direction);
            ps.setString(4, direction);
            if (eventId != null) {
                ps.setInt(5, eventId);
                ps.setInt(6, eventId);
            } else {
                ps.setNull(5, Types.INTEGER);
                ps.setNull(6, Types.INTEGER);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ModeratorSlot.Status status = ModeratorSlot.Status.AVAILABLE;
                    String state = rs.getString("status");
                    if (state != null) {
                        status = ModeratorSlot.Status.valueOf(state);
                    }
                    if (rs.getString("direct_status") != null) {
                        status = ModeratorSlot.Status.APPROVED;
                    }
                    slots.add(new ModeratorSlot(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("event_title"),
                            rs.getString("direction_name"),
                            rs.getDate("activity_date").toLocalDate(),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime(),
                            status
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return slots;
    }

    public void submitApplication(int activityId, int moderatorId) {
        String sql = "INSERT INTO moderator_application(activity_id, moderator_id, status) VALUES(?,?, 'SENT')";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setInt(2, moderatorId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void cancelApplications(int moderatorId) {
        String sql = "DELETE FROM moderator_application WHERE moderator_id = ? AND status = 'SENT'";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, moderatorId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<ModeratorSlot> myActivities(int moderatorId) {
        String sql = """
            SELECT a.id,
                   a.title,
                   e.title    AS event_title,
                   d.name     AS direction_name,
                   DATE(e.start_datetime) + INTERVAL (a.day_num-1) DAY AS activity_date,
                   a.start_time,
                   a.end_time
              FROM activity a
              JOIN event e ON e.id = a.event_id
              JOIN direction d ON d.id = e.direction_id
              LEFT JOIN moderator_application ma ON ma.activity_id = a.id AND ma.moderator_id = ?
             WHERE a.moderator_id = ? OR ma.status = 'APPROVED'
        """;

        List<ModeratorSlot> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, moderatorId);
            ps.setInt(2, moderatorId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ModeratorSlot(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("event_title"),
                            rs.getString("direction_name"),
                            rs.getDate("activity_date").toLocalDate(),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime(),
                            ModeratorSlot.Status.APPROVED
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }
}
