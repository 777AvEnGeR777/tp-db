package tp.db.dao;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tp.db.models.*;
import tp.db.models.Thread;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PostDAO {
    private final JdbcTemplate template;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final UserDAO userDAO;
    private final ForumDAO forumDAO;
    private final ThreadDAO threadDAO;

    public PostDAO(JdbcTemplate template, NamedParameterJdbcTemplate namedTemplate,
                   UserDAO userDAO, ForumDAO forumDAO, ThreadDAO threadDAO) {
        this.template = template;
        this.namedTemplate = namedTemplate;
        this.userDAO = userDAO;
        this.forumDAO = forumDAO;
        this.threadDAO = threadDAO;
    }

    public class PostRowMapper implements RowMapper {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            Post post = new Post();
            post.setAuthor(rs.getString("author"));
            post.setCreated(LocalDateTime.ofInstant(rs.getTimestamp("created").toInstant(),
                    ZoneOffset.ofHours(0)).format(DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
            post.setForum(rs.getString("forum"));
            post.setId(rs.getInt("id"));
            post.setMessage(rs.getString("message"));
            post.setEdited(rs.getBoolean("is_edited"));
            post.setParent(rs.getLong("parent"));
            post.setThread(rs.getInt("thread"));
            return post;
        }
    }

    public List<Post> createPost(final String slug, List<Post> posts) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            Thread thread = threadDAO.getThreadBySlugOrId(slug);
            if(thread == null) {
                throw new ThreadDAO.NoSuchThreadException(slug);
            }

            for (Post post : posts) {
                post.setThread(thread.getId());
                post.setForum(thread.getForum());
                post.setCreated(thread.getCreated());

                if(post.getParent() != 0) {
                    List<Post> p = template.query("SELECT * FROM post WHERE id = ? AND thread = ?",
                            new Object[] { post.getParent(), thread.getId() }, new PostRowMapper());
                    if(p.isEmpty()) {
                        throw new NoSuchPostException(post.getParent());
                    }
                }
                User user = userDAO.getUserByNickname(post.getAuthor());
                if(user == null) {
                    throw new UserDAO.NoSuchUserException(post.getAuthor());
                }
                Forum forum = forumDAO.getForumBySlug(thread.getForum());
                if(forum == null) {
                    throw new ForumDAO.NoSuchForumException(post.getForum());
                }
                post.setAuthor(user.getNickname());
                post.setForum(forum.getSlug());

                final Long id = template.queryForObject("SELECT nextval('post_id_seq')", Long.class);
                post.setId(id);
                template.update(con -> {
                    PreparedStatement pst = con.prepareStatement("INSERT INTO post(id, author," +
                            "created, forum, is_edited, message, parent, thread, path) VALUES (" +
                            "?, ?, COALESCE(?::TIMESTAMPTZ, CURRENT_TIMESTAMP), ?, ?, ?, ?, ?, (SELECT path " +
                            "FROM post WHERE id = ?) || ?)");
                    pst.setLong(1, post.getId());
                    pst.setString(2, post.getAuthor());
                    pst.setString(3, post.getCreated());
                    pst.setString(4, post.getForum());
                    pst.setBoolean(5, post.isEdited());
                    pst.setString(6, post.getMessage());
                    pst.setLong(7, post.getParent());
                    pst.setInt(8, post.getThread());
                    pst.setLong(9, post.getParent());
                    pst.setLong(10, post.getId());
                    return pst;
                });
            }
            template.update("UPDATE forum SET posts = posts + ? WHERE slug = ?",
                    posts.size(), thread.getForum());
            return posts;

        } catch (UserDAO.NoSuchUserException | ForumDAO.NoSuchForumException |
                DuplicateKeyException ex) {
            throw ex;
        }
    }

    public List<Post> getPostsBySlugOrId(String slugOrId, Integer limit, Long since,
                                         String sort, Boolean desc) {
        final StringBuilder sql = new StringBuilder();
        ArrayList<Object> params = new ArrayList<>();

        Thread thread = threadDAO.getThreadBySlugOrId(slugOrId);

        if(thread == null) {
            throw new ThreadDAO.NoSuchThreadException(slugOrId);
        }

        if(sort == null) {
            sort = "flat";
        }

        switch(sort) {
            case "tree":
                sql.append("SELECT * FROM post WHERE thread = ?");
                params.add(thread.getId());
                if(since != null) {
                    params.add(since);
                    if(desc) {
                        sql.append(" AND path < (SELECT post.path FROM post WHERE post.id = ?)");
                    } else {
                        sql.append(" AND path > (SELECT post.path FROM post WHERE post.id = ?)");
                    }
                }
                sql.append(" ORDER BY post.path ");
                if(desc) {
                    sql.append("DESC");
                } else {
                    sql.append("ASC");
                }
                if(limit != null) {
                    params.add(limit);
                    sql.append(" LIMIT ?");
                }
                break;
            case "parent_tree":
                sql.append("WITH sub as (SELECT path FROM post WHERE thread = ? and parent = 0");
                params.add(thread.getId());
                if(since != null) {
                    params.add(since);
                    if(desc) {
                        sql.append(" AND path < (SELECT post.path FROM post WHERE post.id = ?)");
                    } else {
                        sql.append(" AND path > (SELECT post.path FROM post WHERE post.id = ?)");
                    }
                }
                sql.append(" ORDER BY post.id ");
                if(desc) {
                    sql.append("DESC");
                } else {
                    sql.append("ASC");
                }
                if(limit != null) {
                    params.add(limit);
                    sql.append(" LIMIT ?");
                }
                sql.append(") SELECT * FROM post JOIN sub ON (sub.path <@ post.path) ORDER BY post.path ");
                if(desc) {
                    sql.append("DESC");
                } else {
                    sql.append("ASC");
                }
                break;
            default: sql.append("SELECT * FROM post WHERE thread = ?");
                params.add(thread.getId());
                if(since != null) {
                    params.add(since);
                    if(desc) {
                        sql.append(" AND id < ?");
                    } else {
                        sql.append(" AND id > ?");
                    }
                }
                sql.append(" ORDER BY created, id ");
                if(desc) {
                    sql.append("DESC");
                } else {
                    sql.append("ASC");
                }
                if(limit != null) {
                    params.add(limit);
                    sql.append(" LIMIT ?");
                }
                break;
        }

        List<Post> result = template.query(sql.toString(), params.toArray(),
                new PostRowMapper());
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    public Post getPostById(Long id) {
        List<Post> result = template.query(con -> {
            PreparedStatement pst = con.prepareStatement("SELECT * FROM post WHERE id = ?");
            pst.setLong(1, id);
            return pst;
        }, new PostRowMapper());
        if(result.isEmpty()) {
            throw new NoSuchPostException(id);
        }
        return result.get(0);
    }

    public Post updatePost(Long id, PostUpdate update) {
        int affectedRows = template.update(con -> {
            PreparedStatement pst = con.prepareStatement("UPDATE post SET message = ?," +
                    "is_edited = TRUE WHERE id = ?");
            pst.setString(1, update.getMessage());
            pst.setLong(2, id);
            return pst;
        });
        if(affectedRows == 0) {
            throw new NoSuchPostException(id);
        }
        return getPostById(id);
    }

    public static class NoSuchPostException extends RuntimeException {
        public NoSuchPostException(Long id) {
            super(String.format("Can't find post with id %s", id));
        }
    }
}
