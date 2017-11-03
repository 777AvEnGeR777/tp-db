package tp.db.controllers;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tp.db.dao.ForumDAO;
import tp.db.dao.ThreadDAO;
import tp.db.dao.UserDAO;
import tp.db.models.Error;
import tp.db.models.Forum;
import tp.db.models.Thread;
import tp.db.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    private final ForumDAO dao;
    private final ThreadDAO threadDAO;
    private final UserDAO userDAO;

    public ForumController(ForumDAO dao, ThreadDAO threadDAO, UserDAO userDAO) {
        this.dao = dao;
        this.threadDAO = threadDAO;
        this.userDAO = userDAO;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createForum(@RequestBody Forum body) {
        Forum forum = dao.getForumBySlug(body.getSlug());
        if(forum != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(forum);
        }
        try {
            forum = dao.createForum(body);
            return ResponseEntity.status(HttpStatus.CREATED).body(forum);
        } catch (UserDAO.NoSuchUserException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(ex.getMessage()));
        }
    }

    @PostMapping("/{slug}/create")
    public ResponseEntity<?> createThread(@PathVariable(name = "slug") String slug,
                                          @RequestBody Thread body) {
        try {
            Thread thread = threadDAO.createThread(slug, body);
            return ResponseEntity.status(HttpStatus.CREATED).body(thread);
        } catch (UserDAO.NoSuchUserException | ForumDAO.NoSuchForumException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(ex.getMessage()));
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(threadDAO.getThreadBySlugOrId(body.getSlug()));
        }
    }

    @GetMapping("/{slug}/details")
    public ResponseEntity<?> getForum(@PathVariable(name = "slug") String slug) {
        Forum forum = dao.getForumBySlug(slug);
        if(forum != null) {
            return ResponseEntity.ok(forum);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(String.format("Can't find forum with slug %s", slug)));
        }
    }

    @GetMapping("/{slug}/threads")
    public ResponseEntity<?> getThreads(@PathVariable(name = "slug") String slug,
                                        @RequestParam(name = "limit", required = false) Integer limit,
                                        @RequestParam(name = "since", required = false) String since,
                                        @RequestParam(name = "desc", required = false,
                                                defaultValue = "false") Boolean desc) {
        List<Thread> threads = threadDAO.getThreadsByForumSlug(slug, limit, since, desc);
        if(threads != null) {
            return ResponseEntity.ok(threads);
        } else {
            if(dao.getForumBySlug(slug) != null) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(String.format("Can't find forum with slug %s", slug)));
        }
    }

    @GetMapping("/{slug}/users")
    public ResponseEntity<?> getUsers(@PathVariable(name = "slug") String slug,
                                      @RequestParam(name = "limit", required = false) Integer limit,
                                      @RequestParam(name = "since", required = false) String since,
                                      @RequestParam(name = "desc", required = false,
                                              defaultValue = "false") Boolean desc) {
        List<User> users = userDAO.getUsersByForumSlug(slug, limit, since, desc);
        if(users != null) {
            return ResponseEntity.ok(users);
        } else {
            if(dao.getForumBySlug(slug) != null) {
                return ResponseEntity.ok(Collections.EMPTY_LIST);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(String.format("Can't find forum with slug %s", slug)));
        }
    }

}
