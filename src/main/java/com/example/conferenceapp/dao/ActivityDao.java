package com.example.conferenceapp.dao;

import com.example.conferenceapp.model.Activity;
import com.example.conferenceapp.model.ActivityTask;
import com.example.conferenceapp.model.ParticipantActivity;
import com.example.conferenceapp.model.ResourceItem;
import com.example.conferenceapp.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityDao {

    public List<Activity> findByEvent(int eventId) {
        String sql = """
            SELECT a.id,
                   a.event_id,
                   a.title,
                   a.day_num,
                   a.start_time,
                   a.end_time,
                   a.moderator_id,
                   u.full_name AS moderator_name,
                   (SELECT DATE(e.start_datetime) + INTERVAL (a.day_num-1) DAY
                    FROM event e WHERE e.id = a.event_id) AS activity_date
              FROM activity a
              LEFT JOIN user u ON a.moderator_id = u.id
             WHERE a.event_id = ?
             ORDER BY a.day_num, a.start_time
        """;

        List<Activity> activities = new ArrayList<>();

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Activity activity = new Activity(
                            rs.getInt("id"),
                            rs.getInt("event_id"),
                            rs.getString("title"),
                            rs.getInt("day_num"),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime(),
                            (Integer) rs.getObject("moderator_id"),
                            rs.getString("moderator_name")
                    );
                    Date date = rs.getDate("activity_date");
                    if (date != null) {
                        activity.setDate(date.toLocalDate());
                    }
                    activities.add(activity);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        loadJury(activities);
        loadTasks(activities);
        return activities;
    }

    private void loadJury(List<Activity> activities) {
        if (activities.isEmpty()) return;
        String sql = """
            SELECT aj.activity_id,
                   u.full_name
              FROM activity_jury aj
              JOIN user u ON u.id = aj.jury_id
             WHERE aj.activity_id = ?
             ORDER BY u.full_name
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (Activity activity : activities) {
                ps.setInt(1, activity.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        activity.withJury(rs.getString("full_name"));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadTasks(List<Activity> activities) {
        if (activities.isEmpty()) return;

        String sql = """
            SELECT t.id,
                   t.activity_id,
                   t.title,
                   u.full_name AS author
              FROM activity_task t
              LEFT JOIN user u ON u.id = t.created_by
             WHERE t.activity_id = ?
             ORDER BY t.id
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (Activity activity : activities) {
                ps.setInt(1, activity.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        activity.withTask(new ActivityTask(
                                rs.getInt("id"),
                                rs.getInt("activity_id"),
                                rs.getString("title"),
                                rs.getString("author")
                        ));
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public int insert(Activity activity) {
        String sql = """
            INSERT INTO activity(event_id, title, day_num, start_time, end_time)
            VALUES(?,?,?,?,?)
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, activity.getEventId());
            ps.setString(2, activity.getTitle());
            ps.setInt(3, activity.getDayNum());
            ps.setTime(4, Time.valueOf(activity.getStartTime()));
            ps.setTime(5, Time.valueOf(activity.getEndTime()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    activity.setId(id);
                    return id;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public void assignJury(int activityId, List<Integer> juryIds) {
        String sql = "INSERT INTO activity_jury(activity_id, jury_id) VALUES(?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (Integer id : juryIds) {
                ps.setInt(1, activityId);
                ps.setInt(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<ResourceItem> findResources(int activityId) {
        String sql = """
            SELECT r.id,
                   r.activity_id,
                   r.name,
                   r.url,
                   u.full_name AS uploaded_by,
                   r.uploaded_at
              FROM resource r
              LEFT JOIN user u ON u.id = r.uploaded_by
             WHERE r.activity_id = ?
             ORDER BY r.id
        """;
        List<ResourceItem> resources = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(new ResourceItem(
                            rs.getInt("id"),
                            rs.getInt("activity_id"),
                            rs.getString("name"),
                            rs.getString("url"),
                            rs.getString("uploaded_by"),
                            rs.getTimestamp("uploaded_at") != null
                                    ? rs.getTimestamp("uploaded_at").toLocalDateTime()
                                    : null
                    ));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return resources;
    }

    public ResourceItem addResource(int activityId, String name, String url,
                                    Integer userId, String uploadedByName) {
        String sql = "INSERT INTO resource(activity_id, name, url, uploaded_by) VALUES(?,?,?,?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, activityId);
            ps.setString(2, name);
            ps.setString(3, url);
            if (userId != null) {
                ps.setInt(4, userId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new ResourceItem(id, activityId, name, url,
                            uploadedByName, LocalDateTime.now());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean deleteResource(int resourceId) {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM resource WHERE id = ?")) {
            ps.setInt(1, resourceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean hasCollision(int moderatorId, LocalDate date, LocalTime start, LocalTime end) {
        String sql = """
            SELECT COUNT(*)
              FROM activity a
              JOIN event e ON e.id = a.event_id
              LEFT JOIN moderator_application ma
                     ON ma.activity_id = a.id AND ma.moderator_id = ?
             WHERE (
                    (ma.status = 'APPROVED' OR ma.status = 'SENT')
                    OR a.moderator_id = ?
                  )
               AND DATE(e.start_datetime) + INTERVAL (a.day_num-1) DAY = ?
               AND NOT (a.end_time <= ? OR a.start_time >= ?)
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, moderatorId);
            ps.setInt(2, moderatorId);
            ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(start));
            ps.setTime(5, Time.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public List<ParticipantActivity> findForParticipant(int participantId) {
        String sql = """
            SELECT a.id,
                   a.event_id,
                   a.title,
                   e.title AS event_title,
                   DATE(e.start_datetime) + INTERVAL (a.day_num-1) DAY AS activity_date,
                   a.start_time,
                   a.end_time
              FROM participant_event pe
              JOIN event e ON e.id = pe.event_id
              JOIN activity a ON a.event_id = e.id
             WHERE pe.participant_id = ?
             ORDER BY activity_date, a.start_time
        """;

        List<ParticipantActivity> activities = new ArrayList<>();
        Map<Integer, ParticipantActivity> byActivity = new LinkedHashMap<>();
        Set<Integer> eventIds = new HashSet<>();
        Set<Integer> activityIds = new HashSet<>();

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, participantId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date dateValue = rs.getDate("activity_date");
                    Time startValue = rs.getTime("start_time");
                    Time endValue = rs.getTime("end_time");
                    if (dateValue == null || startValue == null || endValue == null) {
                        continue;
                    }

                    LocalDate activityDate = dateValue.toLocalDate();
                    ParticipantActivity activity =
                            new ParticipantActivity(
                                    rs.getInt("id"),
                                    rs.getInt("event_id"),
                                    rs.getString("title"),
                                    rs.getString("event_title"),
                                    LocalDateTime.of(activityDate, startValue.toLocalTime()),
                                    LocalDateTime.of(activityDate, endValue.toLocalTime())
                            );
                    activities.add(activity);
                    byActivity.put(activity.getActivityId(), activity);
                    eventIds.add(activity.getEventId());
                    activityIds.add(activity.getActivityId());
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        if (activities.isEmpty()) {
            return activities;
        }

        loadParticipants(eventIds, activities);
        loadResources(activityIds, byActivity);
        return activities;
    }

    private void loadParticipants(Set<Integer> eventIds,
                                  List<ParticipantActivity> activities) {
        if (eventIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(eventIds.size(), "?"));
        String sql = """
            SELECT pe.event_id,
                   u.full_name
              FROM participant_event pe
              JOIN user u ON u.id = pe.participant_id
             WHERE pe.event_id IN (%s)
             ORDER BY u.full_name
        """.formatted(placeholders);

        Map<Integer, List<String>> byEvent = new HashMap<>();

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = 1;
            for (Integer eventId : eventIds) {
                ps.setInt(idx++, eventId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byEvent.computeIfAbsent(rs.getInt("event_id"), k -> new ArrayList<>())
                            .add(rs.getString("full_name"));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        for (ParticipantActivity activity : activities) {
            List<String> participants = byEvent.get(activity.getEventId());
            if (participants != null) {
                participants.forEach(activity::withParticipant);
            }
        }
    }

    private void loadResources(Set<Integer> activityIds,
                               Map<Integer, ParticipantActivity> byActivity) {
        if (activityIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(activityIds.size(), "?"));
        String sql = """
            SELECT r.id,
                   r.activity_id,
                   r.name,
                   r.url,
                   u.full_name AS uploaded_by,
                   r.uploaded_at
              FROM resource r
              LEFT JOIN user u ON u.id = r.uploaded_by
             WHERE r.activity_id IN (%s)
             ORDER BY r.id
        """.formatted(placeholders);

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = 1;
            for (Integer activityId : activityIds) {
                ps.setInt(idx++, activityId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParticipantActivity activity = byActivity.get(rs.getInt("activity_id"));
                    if (activity == null) {
                        continue;
                    }
                    activity.withResource(new ResourceItem(
                            rs.getInt("id"),
                            rs.getInt("activity_id"),
                            rs.getString("name"),
                            rs.getString("url"),
                            rs.getString("uploaded_by"),
                            rs.getTimestamp("uploaded_at") != null
                                    ? rs.getTimestamp("uploaded_at").toLocalDateTime()
                                    : null
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
