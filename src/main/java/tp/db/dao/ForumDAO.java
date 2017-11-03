package tp.db.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tp.db.models.Forum;
import tp.db.models.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Transactional
public class ForumDAO {
    private final JdbcTemplate template;
    private final UserDAO userDAO;

    public ForumDAO(JdbcTemplate template, UserDAO userDAO) {
        this.template = template;
        this.userDAO = userDAO;
    }

    public class ForumRowMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            Forum forum = new Forum();
            forum.setPosts(rs.getLong("posts"));
            forum.setSlug(rs.getString("slug"));
            forum.setThreads(rs.getInt("threads"));
            forum.setTitle(rs.getString("title"));
            forum.setUser(rs.getString("user"));
            return forum;
        }
    }

    public Forum createForum(final Forum forum) {
        try {
            User user = userDAO.getUserByNickname(forum.getUser());
            if(user == null) {
                throw new UserDAO.NoSuchUserException(forum.getUser());
            }
            forum.setUser(user.getNickname());
            template.update(con -> {
                PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO forum(slug, title, \"user\") " +
                                "VALUES (?, ?, ?)");
                pst.setString(1, forum.getSlug());
                pst.setString(2, forum.getTitle());
                pst.setString(3, forum.getUser());
                return pst;
            });
            return forum;
        } catch (UserDAO.NoSuchUserException ex) {
            throw ex;
        }
    }

    public Forum getForumBySlug(final String slug) {
        List<Forum> result = template.query(connection -> {
            PreparedStatement pst = connection.prepareStatement("SELECT * FROM forum WHERE " +
                    "LOWER (slug) = LOWER (?)");
            pst.setString(1, slug);
            return pst;
        }, new ForumRowMapper());

        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public static class NoSuchForumException extends RuntimeException {
        public NoSuchForumException(String slug) {
            super(String.format("Can't find forum with slug %s", slug));
        }
    }
}
