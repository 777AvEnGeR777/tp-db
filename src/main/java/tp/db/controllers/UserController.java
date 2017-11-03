package tp.db.controllers;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tp.db.models.Error;
import tp.db.dao.UserDAO;
import tp.db.models.User;
import tp.db.models.UserUpdate;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserDAO dao;

    public UserController(UserDAO dao) {
        this.dao = dao;
    }

    @PostMapping("/{nickname}/create")
    public ResponseEntity<?> createUser(@PathVariable(name = "nickname") String nickName,
                                     @RequestBody User body) {
        body.setNickname(nickName);
        User newUser = dao.createUser(body);
        if(newUser != null)
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        else
            return ResponseEntity.status(HttpStatus.CONFLICT).body(dao.getUsersByNickOrEmail(nickName,
                    body.getEmail()));
    }

    @GetMapping("/{nickname}/profile")
    public ResponseEntity<?> getUser(@PathVariable(name = "nickname") String nickname) {
        User user = dao.getUserByNickname(nickname);
        if(user != null)
            return ResponseEntity.status(HttpStatus.OK).body(user);
        else
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Error(String.format("Can't find user with nickname %s", nickname)));
    }

    @PostMapping("/{nickname}/profile")
    public ResponseEntity<?> updateUser(@PathVariable(name = "nickname") String nickname,
                                        @RequestBody UserUpdate body) {
        try {
            User user = dao.updateUser(nickname, body);
            return ResponseEntity.ok(user);
        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new Error("This email is already in use"));
        } catch (UserDAO.NoSuchUserException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new Error(ex.getMessage())
            );
        }
    }
}
