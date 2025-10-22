package com.example.conferenceapp.dao;

import com.example.conferenceapp.model.PersonCard;
import com.example.conferenceapp.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonDao {

    public List<PersonCard> findJuryAndModerators(String roleFilter, String lastName, Integer eventId) {
        StringBuilder sql = new StringBuilder("""
            SELECT u.id,
                   u.id_number,
                   u.full_name,
                   u.email,
                   u.phone,
                   r.label_ru AS role_label,
                   d.name     AS direction,
                   e.title    AS event_title,
                   u.photo
              FROM user u
              JOIN role r ON r.id = u.role_id
              LEFT JOIN direction d ON d.id = u.direction_id
              LEFT JOIN moderator_assignment ma ON ma.user_id = u.id
              LEFT JOIN event e ON e.id = ma.event_id
             WHERE r.code IN ('jury','moderator')
        """);

        if (roleFilter != null) {
            sql.append(" AND r.code = ?");
        }
        if (lastName != null && !lastName.isBlank()) {
            sql.append(" AND u.full_name LIKE ?");
        }
        if (eventId != null) {
            sql.append(" AND ma.event_id = ?");
        }

        sql.append(" ORDER BY u.full_name");

        List<PersonCard> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int idx = 1;
            if (roleFilter != null) {
                ps.setString(idx++, roleFilter);
            }
            if (lastName != null && !lastName.isBlank()) {
                ps.setString(idx++, lastName + "%");
            }
            if (eventId != null) {
                ps.setInt(idx++, eventId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PersonCard(
                            rs.getInt("id"),
                            rs.getString("id_number"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("role_label"),
                            rs.getString("direction"),
                            rs.getString("event_title"),
                            rs.getString("photo")
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public List<PersonCard> findParticipants(String lastName, Integer eventId) {
        StringBuilder sql = new StringBuilder("""
            SELECT u.id,
                   u.id_number,
                   u.full_name,
                   u.email,
                   u.phone,
                   r.label_ru,
                   d.name     AS direction,
                   e.title    AS event_title,
                   u.photo
              FROM user u
              JOIN role r ON r.id = u.role_id
              LEFT JOIN direction d ON d.id = u.direction_id
              LEFT JOIN participant_event pe ON pe.participant_id = u.id
              LEFT JOIN event e ON e.id = pe.event_id
             WHERE r.code = 'participant'
        """);

        if (lastName != null && !lastName.isBlank()) {
            sql.append(" AND u.full_name LIKE ?");
        }
        if (eventId != null) {
            sql.append(" AND pe.event_id = ?");
        }

        sql.append(" ORDER BY u.full_name");

        List<PersonCard> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int idx = 1;
            if (lastName != null && !lastName.isBlank()) {
                ps.setString(idx++, lastName + "%");
            }
            if (eventId != null) {
                ps.setInt(idx++, eventId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PersonCard(
                            rs.getInt("id"),
                            rs.getString("id_number"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("label_ru"),
                            rs.getString("direction"),
                            rs.getString("event_title"),
                            rs.getString("photo")
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public Optional<PersonCard> findByIdNumber(String idNumber) {
        String sql = """
            SELECT u.id,
                   u.id_number,
                   u.full_name,
                   u.email,
                   u.phone,
                   r.label_ru,
                   d.name,
                   NULL AS event_title,
                   u.photo
              FROM user u
              JOIN role r ON r.id = u.role_id
              LEFT JOIN direction d ON d.id = u.direction_id
             WHERE u.id_number = ?
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, idNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PersonCard(
                            rs.getInt("id"),
                            rs.getString("id_number"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("label_ru"),
                            rs.getString("name"),
                            rs.getString("event_title"),
                            rs.getString("photo")
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return Optional.empty();
    }

    public String nextIdNumber(String prefix) {
        String sql = "SELECT LPAD(MAX(CAST(SUBSTRING(id_number, ?) AS UNSIGNED)) + 1, 6, '0') FROM user WHERE id_number LIKE ?";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, prefix.length() + 1);
            ps.setString(2, prefix + "%");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String suffix = rs.getString(1);
                    if (suffix != null) {
                        return prefix + suffix;
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return prefix + "000001";
    }

    public List<com.example.conferenceapp.model.LookupValue> loadUsersByRole(String roleCode) {
        String sql = """
            SELECT u.id, u.full_name
              FROM user u
              JOIN role r ON r.id = u.role_id
             WHERE r.code = ?
             ORDER BY u.full_name
        """;
        List<com.example.conferenceapp.model.LookupValue> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, roleCode);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new com.example.conferenceapp.model.LookupValue(
                            rs.getInt("id"),
                            rs.getString("full_name")
                    ));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public int register(String roleCode, String idNumber, String fullName,
                        String gender, LocalDate birthDate, Integer directionId,
                        Integer countryId, String email, String phone,
                        String password, String photoPath) {

        String sql = """
            INSERT INTO user(id_number, role_id, full_name, gender, birth_date,
                             direction_id, country_id, email, phone, password_hash, photo)
            VALUES(?, (SELECT id FROM role WHERE code = ?), ?, ?, ?, ?, ?, ?, ?, SHA2(?,256), ?)
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, idNumber);
            ps.setString(2, roleCode);
            ps.setString(3, fullName);
            ps.setString(4, gender);

            if (birthDate != null) {
                ps.setDate(5, Date.valueOf(birthDate));
            } else {
                ps.setNull(5, Types.DATE);
            }

            if (directionId != null) {
                ps.setInt(6, directionId);
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            if (countryId != null) {
                ps.setInt(7, countryId);
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.setString(8, email);
            ps.setString(9, phone);
            ps.setString(10, password);
            ps.setString(11, photoPath);

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
}
