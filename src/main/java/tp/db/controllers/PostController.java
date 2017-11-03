package tp.db.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tp.db.dao.ForumDAO;
import tp.db.dao.PostDAO;
import tp.db.dao.ThreadDAO;
import tp.db.dao.UserDAO;
import tp.db.models.*;
import tp.db.models.Error;
import tp.db.models.Thread;

import javax.xml.ws.Response;
import java.util.List;

@RestController
@RequestMapping("/api/post")
public class PostController {
    private final PostDAO dao;
    private final UserDAO userDAO;
    private final ForumDAO forumDAO;
    private final ThreadDAO threadDAO;

    public PostController(PostDAO dao, UserDAO userDAO, ForumDAO forumDAO, ThreadDAO threadDAO) {
        this.dao = dao;
        this.userDAO = userDAO;
        this.forumDAO = forumDAO;
        this.threadDAO = threadDAO;
    }

    @GetMapping("{id}/details")
    public ResponseEntity<?> getPostDetails(@PathVariable(name = "id") Long id,
                                            @RequestParam(name = "related", required = false)
                                                    List<String> related) {
        try {
            Post post = dao.getPostById(id);

            PostFull postFull = new PostFull();
            postFull.setPost(post);

            if (related != null) {
                if (related.contains("user")) {
                    User author = userDAO.getUserByNickname(post.getAuthor());
                    if (author != null) {
                        postFull.setAuthor(author);
                    }
                }
                if (related.contains("forum")) {
                    Forum forum = forumDAO.getForumBySlug(post.getForum());
                    if (forum != null) {
                        postFull.setForum(forum);
                    }
                }
                if (related.contains("thread")) {
                    Thread thread = threadDAO.getThreadBySlugOrId(String.valueOf(post.getThread()));
                    if (thread != null) {
                        postFull.setThread(thread);
                    }
                }
            }
            return ResponseEntity.ok(postFull);
        } catch (PostDAO.NoSuchPostException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error(ex.getMessage()));
        }
    }

    @PostMapping("{id}/details")
    public ResponseEntity<?> updatePost(@PathVariable(name = "id") Long id,
                                        @RequestBody PostUpdate body) {
        try {
            Post post = dao.getPostById(id);

            if(body.getMessage() == null) {
                return ResponseEntity.ok(post);
            }

            if(!post.getMessage().equals(body.getMessage())) {
                post = dao.updatePost(id, body);
            }

            return ResponseEntity.ok(post);
        } catch (PostDAO.NoSuchPostException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error(ex.getMessage()));
        }
    }
}
