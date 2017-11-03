package tp.db.models;

public class Vote {
    private String nickname;
    private int voice;
    private int thread;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getVoice() {
        return voice;
    }

    public void setVoice(int vote) {
        this.voice = vote;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }
}
