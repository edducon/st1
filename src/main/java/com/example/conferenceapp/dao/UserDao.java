package com.example.conferenceapp.dao;

import com.example.conferenceapp.model.User;
import com.example.conferenceapp.util.DBUtil;

import java.sql.*;

/** DAO-класс для работы с таблицей user. */
public class UserDao {

    /* -------------------------------- Аутентификация -------------------------------- */

    public User authenticate(String idNumber, String rawPassword) {

        final String sql = """
            SELECT u.id,
                   u.id_number,
                   u.full_name,
                   u.email,
                   u.phone,
                   u.photo,
                   r.code AS role_code
            FROM user u
            JOIN role r ON u.role_id = r.id
            WHERE u.id_number = ?
              AND u.password_hash = SHA2(?,256)
        """;

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, idNumber);
            ps.setString(2, rawPassword);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    /* code → enum ----------------------------------------- */
                    User.Role role = switch (rs.getString("role_code")) {
                        case "organizer"  -> User.Role.ORGANIZER;
                        case "moderator"  -> User.Role.MODERATOR;
                        case "jury"       -> User.Role.JURY;
                        default           -> User.Role.PARTICIPANT;
                    };

                    /* собираем модель ------------------------------------ */
                    return new User(
                            rs.getInt   ("id"),
                            rs.getString("id_number"),
                            rs.getString("full_name"),
                            role,
                            rs.getString("photo"),
                            rs.getString("email"),
                            rs.getString("phone")
                    );
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();      // TODO: logger
        }
        return null;
    }

    /* ------------------------------ Обновление профиля ------------------------------ */

    /**
     * Сохраняет изменения профиля организатора.<br>
     * Обновляются:
     * <ul>
     *   <li>full_name, email, phone</li>
     *   <li>direction / country — через внешний ключ</li>
     *   <li>password (если задан raw-пароль)</li>
     * </ul>
     */
    public boolean update(User u) {

        final String sql = """
            UPDATE user
               SET full_name   = ?,
                   email       = ?,
                   phone       = ?,
                   direction_id= (SELECT id FROM direction WHERE name=?),
                   country_id  = (SELECT id FROM country WHERE name_ru=?)
                   %s
             WHERE id = ?
        """.formatted(u.getPasswordHash() == null ? ""
                : ", password_hash = SHA2(?,256)");

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, u.getFullName());
            ps.setString(idx++, u.getEmail());
            ps.setString(idx++, u.getPhone());
            ps.setString(idx++, u.getDirection());
            ps.setString(idx++, u.getCountry());

            /* если пароль меняют — добавляем параметр */
            if (u.getPasswordHash() != null) {
                ps.setString(idx++, u.getPasswordHash());
            }
            ps.setInt(idx, u.getId());

            return ps.executeUpdate() == 1;

        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
