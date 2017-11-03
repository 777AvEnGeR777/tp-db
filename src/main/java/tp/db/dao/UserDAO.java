package tp.db.dao;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tp.db.models.User;
import tp.db.models.UserUpdate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class UserDAO {
    private final JdbcTemplate template;

    public UserDAO(JdbcTemplate template) {
        this.template = template;
    }

    public class UserRowMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setNickname(rs.getString("nickname"));
            user.setFullname(rs.getString("fullname"));
            user.setAbout(rs.getString("about"));
            user.setEmail(rs.getString("email"));
            return user;
        }

    }

    public User createUser(final User user) {
        try {
            template.update(con -> {
                PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO users(nickname, fullname, email, about) " +
                                "VALUES (?, ?, ?, ?)");
                pst.setString(1, user.getNickname());
                pst.setString(2, user.getFullname());
                pst.setString(3, user.getEmail());
                pst.setObject(4, user.getAbout());
                return pst;
            });
            return user;
        } catch (DuplicateKeyException ex) {
            return null;
        }
    }

    public List<User> getUsersByNickOrEmail(final String nickname, final String email) {
        List<User> result = template.query(connection -> {
            PreparedStatement pst = connection.prepareStatement("SELECT * FROM users WHERE " +
                    "LOWER (nickname) = LOWER (?) OR LOWER (email) = LOWER (?)");
            pst.setString(1, nickname);
            pst.setString(2, email);
            return pst;
            }, new UserRowMapper());
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public User getUserByNickname(final String nickname) {
        List<User> result = template.query(connection -> {
            PreparedStatement pst = connection.prepareStatement("SELECT * FROM users WHERE " +
                    "LOWER (nickname) = LOWER (?)");
            pst.setString(1, nickname);
            return pst;
        }, new UserRowMapper());
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public User updateUser(final String nickname, final UserUpdate userUpdate)
            throws NoSuchUserException {
        int affectedRows = template.update(con -> {
            PreparedStatement pst = con.prepareStatement("UPDATE users SET about = " +
                    "COALESCE (?, about), fullname = COALESCE (?, fullname), email = " +
                    "COALESCE (?, email) WHERE LOWER (nickname) = LOWER (?)");
            pst.setString(1, userUpdate.getAbout());
            pst.setString(2, userUpdate.getFullname());
            pst.setString(3, userUpdate.getEmail());
            pst.setString(4, nickname);
            return pst;
        });
        if(affectedRows == 0)
            throw new NoSuchUserException(nickname);
        return getUserByNickname(nickname);
    }

    public List<User> getUsersByForumSlug(String slug, Integer limit, String since, Boolean desc) {
        final StringBuilder sql = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();
        sql.append("SELECT * FROM (SELECT DISTINCT nickname, fullname, about, email FROM users u JOIN " +
                "thread t ON(t.author = u.nickname) WHERE lower(t.forum) = lower(?) UNION SELECT DISTINCT" +
                " nickname, fullname, about, email FROM users u JOIN post p ON(p.author = u.nickname) " +
                "WHERE lower(p.forum) = lower(?)) as us");
        params.add(slug);
        params.add(slug);
        if(since != null) {
            params.add(since);
            if(desc) {
                sql.append(" WHERE LOWER (nickname COLLATE \"ucs_basic\") < LOWER (? " +
                        "COLLATE \"ucs_basic\")");
            } else {
                sql.append(" WHERE LOWER (nickname COLLATE \"ucs_basic\") > LOWER( ? " +
                        "COLLATE \"ucs_basic\")");
            }
        }
        if(desc) {
            sql.append(" ORDER BY LOWER (nickname COLLATE \"ucs_basic\") DESC");
        } else {
            sql.append(" ORDER BY LOWER (nickname COLLATE \"ucs_basic\") ASC");
        }
        if(limit != null) {
            params.add(limit);
            sql.append(" LIMIT ?");
        }
        List<User> result = template.query(sql.toString(), params.toArray(),
                new UserRowMapper());
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public static class NoSuchUserException extends RuntimeException {
        public NoSuchUserException(String nickname) {
            super(String.format("Can't find user with nickname %s", nickname));
        }
    }
}
