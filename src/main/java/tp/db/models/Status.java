package tp.db.models;

public class Status {
    private int forum;
    private long post;
    private int thread;
    private int user;

    public Status(int f, long p, int t, int u) {
        forum = f;
        post = p;
        thread = t;
        user = u;
    }

    public int getForum() {
        return forum;
    }

    public void setForum(int forum) {
        this.forum = forum;
    }

    public long getPost() {
        return post;
    }

    public void setPost(long post) {
        this.post = post;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }
}
