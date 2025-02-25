package backend;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private String email;
    private String appName;
    private ArrayList<Integer> conversations = new ArrayList<>();

    public User() {
    }

    public User(String username, String password, String email, String appName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.appName = appName;

    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getAppName() {
        return appName;
    }

    public void addConversation(int conversationID) {
        this.conversations.add(conversationID);
    }
}
