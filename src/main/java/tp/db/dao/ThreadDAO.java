package tp.db.dao;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import tp.db.models.*;
import tp.db.models.Thread;

@Service
@Transactional
public class ThreadDAO {
    private final JdbcTemplate template;
    private final UserDAO userDAO;
    private final ForumDAO forumDAO;

    public ThreadDAO(JdbcTemplate template, UserDAO userDAO, ForumDAO forumDAO) {
        this.template = template;
        this.userDAO = userDAO;
        this.forumDAO = forumDAO;
    }

    public class ThreadRowMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            Thread thread = new Thread();
            thread.setAuthor(rs.getString("author"));
            thread.setCreated(LocalDateTime.ofInstant(rs.getTimestamp("created").toInstant(),
                    ZoneOffset.ofHours(0)).format(DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
            thread.setForum(rs.getString("forum"));
            thread.setMessage(rs.getString("message"));
            thread.setSlug(rs.getString("slug"));
            thread.setTitle(rs.getString("title"));
            thread.setVotes(rs.getInt("votes"));
            thread.setId(rs.getInt("id"));
            return thread;
        }
    }

    public class VoteSumRowMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("sum");
        }
    }

    public Thread createThread(final String forumSlug, final Thread thread) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            User user = userDAO.getUserByNickname(thread.getAuthor());
            if(user == null) {
                throw new UserDAO.NoSuchUserException(thread.getAuthor());
            }
            Forum forum = forumDAO.getForumBySlug(forumSlug);
            if(forum == null) {
                throw new ForumDAO.NoSuchForumException(forumSlug);
            }
            thread.setAuthor(user.getNickname());
            thread.setForum(forum.getSlug());
            template.update(con -> {
                PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO thread(author, forum, created, message, title, slug) " +
                                "VALUES (?, ?, COALESCE(?::TIMESTAMPTZ, CURRENT_TIMESTAMP), ?, ?, ?) " +
                                "returning id;",
                        PreparedStatement.RETURN_GENERATED_KEYS);

                pst.setString(1, thread.getAuthor());
                pst.setString(2, thread.getForum());
                pst.setString(3, thread.getCreated());
                pst.setString(4, thread.getMessage());
                pst.setString(5, thread.getTitle());
                pst.setString(6, thread.getSlug());
                return pst;
            }, keyHolder);
            thread.setId(keyHolder.getKey().intValue());

            template.update(con -> {
                PreparedStatement pst = con.prepareStatement( "UPDATE forum SET " +
                        "threads = threads + 1 WHERE lower(forum.slug) = lower(?)");
                pst.setString(1, forumSlug);
                return pst;
            });

            return thread;
        } catch (UserDAO.NoSuchUserException | ForumDAO.NoSuchForumException |
                DuplicateKeyException ex) {
            throw ex;
        }
    }

    public List<Thread> getThreadsByForumSlug(String slug, Integer limit, String since, Boolean desc) {
        final StringBuilder sql = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();
        sql.append("SELECT * FROM thread t JOIN forum f ON (t.forum = f.slug) WHERE " +
                "LOWER (f.slug) = LOWER (?)");
        params.add(slug);
        if(since != null) {
            params.add(since);
            if(desc) {
                sql.append(" AND created <= ?::TIMESTAMPTZ");
            } else {
                sql.append(" AND created >= ?::TIMESTAMPTZ");
            }
        }
        if(desc) {
            sql.append(" ORDER BY created DESC");
        } else {
            sql.append(" ORDER BY created ASC");
        }
        if(limit != null) {
            params.add(limit);
            sql.append(" LIMIT ?");
        }
        List<Thread> result = template.query(sql.toString(), params.toArray(),
                new ThreadRowMapper());
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public Thread getThreadBySlugOrId(String slugOrId) {
        Integer id = null;
        StringBuilder sql = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();
        sql.append("SELECT * FROM thread WHERE ");
        try {
            id = Integer.parseInt(slugOrId);
        } catch (NumberFormatException ex) {}

        if(id == null) {
            params.add(slugOrId);
            sql.append("lower(slug) = lower(?)");
        } else {
            params.add(id);
            sql.append("id = ?");
        }

        List<Thread> result = template.query(sql.toString(), params.toArray(),
                new ThreadRowMapper());

        if(result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public Thread voteThread(String slugOrId, Vote vote) {
        Thread thread = getThreadBySlugOrId(slugOrId);
        if(thread == null) {
            return thread;
        }

        try {
            template.update(con -> {
                PreparedStatement pst = con.prepareStatement("INSERT INTO vote(nickname, voice," +
                        "thread) VALUES (?, ?, ?) ON CONFLICT (nickname, thread) DO UPDATE SET " +
                        "voice = ?");
                pst.setString(1, vote.getNickname());
                pst.setInt(2, vote.getVoice());
                pst.setInt(3, thread.getId());
                pst.setInt(4, vote.getVoice());
                return pst;
            });

            ArrayList<Object> params = new ArrayList<>();
            params.add(thread.getId());
            Integer sum = template.queryForObject("SELECT SUM(voice) AS sum FROM vote WHERE " +
                    "thread = ? GROUP BY thread", params.toArray(), Integer.class);

            thread.setVotes(sum);

            template.update(con -> {
                PreparedStatement pst = con.prepareStatement("UPDATE thread SET votes = ? WHERE " +
                        "id = ?");
                pst.setInt(1, sum);
                pst.setInt(2, thread.getId());
                return pst;
            });

            return thread;
        } catch (RuntimeException ex) {
            throw new UserDAO.NoSuchUserException(vote.getNickname());
        }
    }

    public Thread updateThread(String slugOrId, ThreadUpdate threadUpdate) {
        Integer id = null;
        StringBuilder sql = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();
        sql.append("UPDATE thread SET title = COALESCE (?, title), message = " +
                "COALESCE (?, message) WHERE ");
        try {
            id = Integer.parseInt(slugOrId);
        } catch (NumberFormatException ex) {}
        params.add(threadUpdate.getTitle());
        params.add(threadUpdate.getMessage());

        if(id == null) {
            sql.append("lower(slug) = lower(?)");
        } else {
            sql.append("id = ?");
        }

        int affectedRows = template.update(sql.toString(), threadUpdate.getTitle(),
                threadUpdate.getMessage(), (id == null ? slugOrId : id));
        if(affectedRows == 0)
            throw new NoSuchThreadException(slugOrId);
        return getThreadBySlugOrId(slugOrId);
    }

    public static class NoSuchThreadException extends RuntimeException {
        public NoSuchThreadException(String slug) {
            super(String.format("Can't find thread with id or slug %s", slug));
        }
    }
}
