package tp.db.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Post {
    private long id;
    private String author;
    private String created;
    private String forum;
    private boolean isEdited;
    private String message;
    private long parent;
    private int thread;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getForum() {
        return forum;
    }

    public void setForum(String forum) {
        this.forum = forum;
    }

    @JsonProperty("isEdited")
    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean is_edited) {
        this.isEdited = is_edited;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(long parent) {
        this.parent = parent;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread_id) {
        this.thread = thread_id;
    }

}
