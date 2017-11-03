package tp.db.controllers;

import org.springframework.dao.DuplicateKeyException;
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

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/thread")
public class ThreadController {
    private final PostDAO dao;
    private final ThreadDAO threadDAO;

    public ThreadController(PostDAO dao, ThreadDAO threadDAO) {
        this.dao = dao;
        this.threadDAO = threadDAO;
    }

    @PostMapping("/{slug_or_id}/create")
    public ResponseEntity<?> createPosts(@PathVariable(name = "slug_or_id") String slug,
                                         @RequestBody List<Post> body) {
        try {
                List<Post> posts = dao.createPost(slug, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(posts);
        } catch (ForumDAO.NoSuchForumException | UserDAO.NoSuchUserException |
                ThreadDAO.NoSuchThreadException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(ex.getMessage()));
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new Error("This slug already in use"));
        } catch (PostDAO.NoSuchPostException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Error(ex.getMessage()
                    + String.format("in thread %s", slug)));
        }
    }

    @GetMapping("/{slug_or_id}/details")
    public ResponseEntity<?> getThread(@PathVariable(name = "slug_or_id") String slugOrId) {
        Thread thread = threadDAO.getThreadBySlugOrId(slugOrId);
        if(thread != null) {
            return ResponseEntity.ok(thread);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Error(String.format("Can't find thread with slug or id %s", slugOrId)));
    }

    @PostMapping("/{slug_or_id}/details")
    public ResponseEntity<?> updateThread(@PathVariable(name = "slug_or_id") String slugOrID,
                                          @RequestBody ThreadUpdate body) {
        try {
            Thread thread = threadDAO.updateThread(slugOrID, body);
            return ResponseEntity.ok(thread);
        } catch (ThreadDAO.NoSuchThreadException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error(ex.getMessage()));
        }
    }

    @GetMapping("/{slug_or_id}/posts")
    public ResponseEntity<?> getPosts(@PathVariable(name = "slug_or_id") String slugOrId,
                                      @RequestParam(name = "limit", required = false) Integer limit,
                                      @RequestParam(name = "since", required = false) Long since,
                                      @RequestParam(name = "sort", required = false) String sort,
                                      @RequestParam(name = "desc", required = false,
                                              defaultValue = "false") Boolean desc) {
        try {
            List<Post> posts = dao.getPostsBySlugOrId(slugOrId, limit, since, sort, desc);
            if (posts != null) {
                return ResponseEntity.ok(posts);
            }
            return ResponseEntity.ok(Collections.EMPTY_LIST);
        } catch (ThreadDAO.NoSuchThreadException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Error(ex.getMessage()));
        }
    }

    @PostMapping("/{slug_or_id}/vote")
    public ResponseEntity<?> vote(@PathVariable(name = "slug_or_id") String slugOrId,
                                  @RequestBody Vote body) {
        try {
            Thread thread = threadDAO.voteThread(slugOrId, body);
            if (thread == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new Error(String.format("Can't find thread with slug or id %s",
                                slugOrId)));
            }
            return ResponseEntity.ok(thread);
        } catch (UserDAO.NoSuchUserException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new  Error(ex.getMessage()));
        }
    }
}
