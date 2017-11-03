package tp.db.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tp.db.models.Status;

@Service
@Transactional
public class ServiceDAO {
    private JdbcTemplate template;

    public ServiceDAO(JdbcTemplate template) {
        this.template = template;
    }

    public Status getStatus() {
        Integer forumsNum = template.queryForObject("SELECT COUNT(*) FROM forum", Integer.class);
        Long postsNum = template.queryForObject("SELECT COUNT(*) FROM post", Long.class);
        Integer threadsNum = template.queryForObject("SELECT COUNT(*) FROM thread", Integer.class);
        Integer userNum = template.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

        return new Status(forumsNum, postsNum, threadsNum, userNum);
    }

    public void clear() {
        template.execute("TRUNCATE users,forum,thread,post,vote");
    }
}
