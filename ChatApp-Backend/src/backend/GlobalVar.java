package backend;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalVar {
    private static GlobalVar instance;
    //private Map<ClientHandler, User> clientConnections = new ConcurrentHashMap<>();
    private Map<String, User> registeredUsers;
    private Map<String, User> activeUsers = new ConcurrentHashMap<>();
    private static final String ADMIN_USERNAME = "1997alon";
    private static final String ADMIN_PASSWORD = "1997alon";
    private Map<Integer, ArrayList<String>> conversationsMembers;
    private Map<Integer, ArrayList<ArrayList<String>>> conversationDatas = new HashMap<>();

    private GlobalVar() {
    }

    public static GlobalVar getInstance() {
        if (instance == null) {
            synchronized (GlobalVar.class) {
                if (instance == null) {
                    instance = new GlobalVar();
                    instance.loadUsers();
                    instance.innitializedConversation();
                    instance.innitializedData();
                    System.out.println(instance.listUsers());
                }
            }
        }
        return instance;
    }

    public void loadUsers() {
        registeredUsers = UserStorage.loadUsers();
    }

    public ArrayList<String> getRegisteredUsers(String username) {
        ArrayList<String> usersList = new ArrayList<>(registeredUsers.keySet());

        if (username != null) {
            usersList.remove(username); // Exclude the provided username
        }

        return usersList;
    }

    public Map<String, User> getActiveUsers() {
        return activeUsers;
    }

    public boolean isUserExist(String username) {
        return registeredUsers.containsKey(username);
    }

    public boolean isActiveUserExist(String username) {
        return activeUsers.containsKey(username);
    }

    public boolean checkLogin(String username, String password) {
        return isUserExist(username) && registeredUsers.get(username).getPassword().equalsIgnoreCase(password);
    }

    public void addUser(String username, String password, String email, String appName){
        User user = new User(username, password, email, appName);
        registeredUsers.put(username, user);
        UserStorage.saveUser(user);
    }

    public ArrayList<ArrayList<String>> getExistedChats(String username){
        innitializedConversation();
        ArrayList<ArrayList<String>> chats = new ArrayList<>();
        for (ArrayList<String> members : conversationsMembers.values()) {
            // Check if the username is part of the current chat
            if (members.contains(username) && members.size() > 1) {
                // Create a new list excluding the username
                ArrayList<String> otherMembers = new ArrayList<>(members);
                otherMembers.remove(username); // Remove the current username

                // Add the list of other members to the chats list
                chats.add(otherMembers);
            }
        }
        return chats;
    }

    public void addActiveUser(String username) {
        User user = registeredUsers.get(username);
        activeUsers.put(username, user);
    }

    public boolean removeActiveUser(String username) {
        if(activeUsers.containsKey(username)) {
            activeUsers.remove(username);
            return true;
        }
        return false;
    }

    public boolean createConversation(ArrayList<String> usernames) {
        Integer conversationID = UserStorage.findConversationID(usernames);
        if(conversationID != null){
            return false;
        }
        if(usernames == null){
            return false;
        }
        ArrayList<User> users= new ArrayList<>();
        for(String username: usernames) {
            User user = registeredUsers.get(username);
            users.add(user);
        }
        conversationID = UserStorage.createConversation(users);
        if(conversationID == null){
            return false;
        }
        conversationsMembers.put(conversationID, new ArrayList<String>());
        conversationDatas.put(conversationID, new ArrayList<>());

        for(User user: users) {
            user.addConversation(conversationID);
            conversationsMembers.get(conversationID).add(user.getUsername());
        }
        return true;
    }

    public Integer getConversationKeyByMembers(ArrayList<String> members) {
        for (Map.Entry<Integer, ArrayList<String>> entry : conversationsMembers.entrySet()) {
            // Check if the current conversation members are the same as the provided list
            if (entry.getValue().equals(members)) {
                return entry.getKey(); // Return the conversation key
            }
        }
        return null;
    }

    public String getTimeMessage(String username, ArrayList<String> members, String message) {
        if (!members.contains(username)) {
            members.add(username);
        }
        System.out.println("getTimeMessage-global");
        Integer conversationID = UserStorage.findConversationID(members);
        System.out.println(conversationID + "= con ID");
        if (conversationID == null) {
            return "";
        }
        return UserStorage.getTimeOfMessage(username, conversationID, message);
    }

    public boolean sendMessageInConversation(String username, ArrayList<String> membersList, String msg) {
        if (!membersList.contains(username)) {
            membersList.add(username);
        }
        Integer conversationID = UserStorage.findConversationID(membersList);
        if (conversationID == null) {
            System.out.println("No conversation found for the given members.");
            return false;
        }
        boolean answer = UserStorage.addMessageToConversation(conversationID, username, msg);
        ArrayList<String> addMsg = new ArrayList<>();
        addMsg.add(username);
        addMsg.add(msg);
        addMsg.add(UserStorage.getTimeOfMessage(username, conversationID, msg));
        conversationDatas.get(conversationID).add(addMsg);
        return answer;
    }

    public Integer getConversationID(ArrayList<String> usernames) {
        return UserStorage.findConversationID(usernames);
    }

    public ArrayList<ArrayList<String>> getConversationData(Integer conversationID) {
        return conversationDatas.get(conversationID);
    }

    public void innitializedData() {
        for(Integer conID: conversationsMembers.keySet()) {
            conversationDatas.put(conID, UserStorage.getConversationData(conID));
        }
        UserStorage.deleteSingleMemberConversations();
    }

    public ArrayList<ArrayList<String>> fetchChat(ArrayList<String> members, String username) {
        HashMap<ArrayList<String>, ArrayList<ArrayList<String>>> chatsData = fetchChatsData(username);
        for (Map.Entry<ArrayList<String>, ArrayList<ArrayList<String>>> entry : chatsData.entrySet()) {
            ArrayList<String> chatMembers = entry.getKey();

            if (chatMembers.containsAll(members) && members.containsAll(chatMembers)) {
                return entry.getValue();
            }
        }
        return new ArrayList<>();
    }

    public HashMap<ArrayList<String>, ArrayList<ArrayList<String>>> fetchChatsData(String username) {
        HashMap<ArrayList<String>, ArrayList<ArrayList<String>>> chatData = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<String>> entry : conversationsMembers.entrySet()) {
            ArrayList<String> members = entry.getValue();
            if (members.contains(username)) {
                Integer conversationId = entry.getKey();
                ArrayList<ArrayList<String>> messages = conversationDatas.get(conversationId);
                chatData.put(members, messages);
            }
        }
        System.out.println(chatData.toString());
        return chatData;
    }

    public void innitializedConversation() {
        conversationsMembers = UserStorage.getConversationMembers();
    }


    public ArrayList<ArrayList<String>> getMessages(ArrayList<String> users) {
        ArrayList<ArrayList<String>> result = null;
        Integer conversationID = UserStorage.findConversationID(users);
        result = UserStorage.getConversationData(conversationID);

        System.out.println(result.toString());
        return result;
    }

    public String listUsers() {
        if (registeredUsers.isEmpty()) {
            return "No users found.";
        }
        StringBuilder userList = new StringBuilder("List of users:\n");

        for (Map.Entry<String, User> entry : registeredUsers.entrySet()) {
            String username = entry.getKey();
            User user = entry.getValue();
            userList.append("Username: ").append(username)
                    .append(", Password: ").append(user.getPassword()).append("\n");
        }

        return userList.toString();
    }

}