package tp.db.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private String nickname;
    private String fullname;
    private String email;
    private String about;

    public User() {}

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getFullname() {
        return fullname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getAbout() {
        return about;
    }
}
