package backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Server server;
    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String receivedData = in.readLine();
            if (receivedData != null) {
                menu(receivedData, out);
            }
            out.println("Data received successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void menu(String request, PrintWriter out) {
        String[] parts = request.split("-", 2);

        if (parts.length < 2) { // Ensure there is at least one parameter
            out.println("ERROR: Invalid command format");
            return;
        }

        String commandType = parts[0].trim();
        String jsonString = parts[1].trim();

        switch (commandType.toUpperCase()) {
            case "LOGIN":
                handleLogin(jsonString, out); // Pass the JSON string to handleLogin
                break;
            case "SIGNUP":
                handleSignUp(jsonString, out);
                break;
            case "CHATS":
                handleChats(jsonString, out);
                break;
            case "CHAT WITH SERVER":
                handleChatWithServer(jsonString, out);
                break;
            case "MENU":
                handleMenu(jsonString, out);
                break;
            case "SEND MESSAGE":
                handleSendMessage(jsonString, out);
                break;
            case "GET MESSAGES":
                handleGetMessages(jsonString, out);
            default:
                out.println("ERROR: Unknown command: " + commandType);
                System.out.println("Unknown command received: " + commandType);
        }
    }

    public void handleChats(String username, PrintWriter out) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayList<ArrayList<String>> chats = server.getExistedChats(username);
            List<Map<String, List<Map<String, String>>>> formattedChats = new ArrayList<>();

            for (ArrayList<String> chat : chats) {
                List<Map<String, String>> usersInChat = new ArrayList<>();

                // Create a map for each user in the chat
                for (String user : chat) {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("user", user);
                    usersInChat.add(userMap);
                }

                // Add the chat to the list with key "chat"
                Map<String, List<Map<String, String>>> chatMap = new HashMap<>();
                chatMap.put("chat", usersInChat);
                formattedChats.add(chatMap);
            }

            // Create the response map with success status and the formatted chats
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);  // Assuming the operation was successful
            responseMap.put("chats", formattedChats);   // Include the chats in the response

            // Convert the map to a JSON string
            String response = objectMapper.writeValueAsString(responseMap);

            // Send the response
            out.println(response);

        } catch (Exception e) {
            // Handle the exception and return an error message
            e.printStackTrace();

            // Prepare the error response with success false
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error occurred: " + e.getMessage());

            // Send the error response
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                out.println(errorJson);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    public void handleLogin(String jsonString, PrintWriter out) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            String username = jsonNode.get("username").asText();
            String password = jsonNode.get("password").asText();

            boolean isAuthenticated = server.checkLogin(username, password);
            String response = "\"success\": " + isAuthenticated;
            out.println(response);

            if (isAuthenticated) {
                server.addActiveUser(username);
            }

        } catch (Exception e) {
            out.println("{\"error\": \"Failed to parse JSON or handle login\"}");
            e.printStackTrace();
        }
    }

    public void handleSignUp(String jsonString, PrintWriter out) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // Assuming the JSON contains "username" and "password"
            String username = jsonNode.get("username").asText();
            String password = jsonNode.get("password").asText();
            String email = jsonNode.get("email").asText();
            String appName = jsonNode.get("appName").asText();
            boolean isAuthenticated = server.checkExistUser(username);
            String response = null;
            if (!isAuthenticated) {
                server.addUser(username, password, email, appName);
                response = "\"success\": true";
            } else {
                response = "\"success\": false";
            }
            out.println(response);

        } catch (Exception e) {
            out.println("ERROR: Failed to parse JSON");
            e.printStackTrace();
        }
    }
    public void handleChatWithServer(String jsonString, PrintWriter out) {
        ObjectMapper objectMapper = null;
        try {
            objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            String username = jsonNode.get("username").asText();
            String message = jsonNode.get("message").asText();
            String serverResponse = server.getMessageFromServer(username, message);
            System.out.println(serverResponse);
            if (serverResponse != null) {
                // Prepare the response JSON
                ObjectNode responseJson = objectMapper.createObjectNode();
                responseJson.put("success", true);
                responseJson.put("response", serverResponse);
                System.out.println(responseJson.toString());
                out.println(responseJson.toString());
            } else {
                ObjectNode responseJson = objectMapper.createObjectNode();
                responseJson.put("success", false);
                responseJson.put("error", "Failed to process the message.");
                out.println(responseJson.toString());
            }

        } catch (Exception e) {
            ObjectNode responseJson = objectMapper.createObjectNode();
            responseJson.put("success", false);
            responseJson.put("error", "Failed to parse JSON.");
            out.println(responseJson.toString());
            e.printStackTrace();
        }
    }

    public void handleMenu(String jsonString, PrintWriter out) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            String action = jsonNode.path("action").asText();
            String username = jsonNode.path("username").asText();
            System.out.println(action + " " + username);
            switch (action.toUpperCase()) {
                case "LOGOUT":
                    handleLogout(username, out); // Pass the JSON string to handleLogin
                    break;
                case "CHATS":
                    handleChats(username, out); // Pass the JSON string to handleLogin
                    break;
                case "NEW CONVERSATION":
                    handleNewConversation(username, out);
                case "GET CONVERSATIONS":
                    handleGetConversations(username, out);
                case "CREATE":
                    ArrayList<String> otherUsers = new ArrayList<>();
                    for (JsonNode node : jsonNode.path("users")) {
                        otherUsers.add(node.path("user").asText());
                    }
                    System.out.println(otherUsers);
                    handleCreateConversation(username, otherUsers, out);
                case "SELECT CHAT":
                    ArrayList<String> chatMembers = new ArrayList<>();
                    for (JsonNode node : jsonNode.path("users")) {
                        chatMembers.add(node.path("user").asText());
                    }
                    System.out.println(chatMembers);
                    handleSelectedChat(username, chatMembers, out);
                default:
                    out.println("ERROR: Unknown command: " + action);
                    System.out.println("Unknown command received: " + action);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public void handleSelectedChat(String username, ArrayList<String> chatMembers, PrintWriter out) {
        try {
            // Check if the username is already in chatMembers, if not, add it
            if (!chatMembers.contains(username)) {
                chatMembers.add(username);
            }

            // Fetch the chat data from the server
            ArrayList<ArrayList<String>> chatData = server.fetchChatData(chatMembers, username);

            // Prepare the response object
            Map<String, Object> responseMap = new HashMap<>();

            // If chatData is not empty, set success: true and format the chat messages
            if (chatData != null) {
                List<Map<String, String>> formattedMessages = new ArrayList<>();
                for (ArrayList<String> messageData : chatData) {
                    Map<String, String> messageMap = new HashMap<>();
                    messageMap.put("sender", messageData.get(0));  // sender: [0]
                    messageMap.put("message", messageData.get(1)); // message: [1]
                    messageMap.put("time", messageData.get(2));    // time: [2]
                    formattedMessages.add(messageMap);
                }

                // Add the success and chat data to the response
                responseMap.put("success", true);
                responseMap.put("chat", formattedMessages);
            } else {
                // If no chat data found, set success: false
                responseMap.put("success", false);
                responseMap.put("chat", new ArrayList<>());
            }

            // Convert the response map to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String responseJson = objectMapper.writeValueAsString(responseMap);

            // Send the response JSON back to the client
            out.println(responseJson);

        } catch (Exception e) {
            // Handle any exceptions by sending success: false
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("chat", new ArrayList<>());
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                out.println(errorJson);
            } catch (JsonProcessingException jsonException) {
                throw new RuntimeException(jsonException);
            }
        }
    }


    public void handleNewConversation(String username, PrintWriter out) {
        try{
            ArrayList<String> allUsers = server.getUsers(username);
            String response = "";

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = new HashMap<>();

            if (allUsers.isEmpty()) {
                responseMap.put("success", false);
                responseMap.put("message", "No users found.");
            } else {
                responseMap.put("success", true);
                List<Map<String, String>> userList = new ArrayList<>();
                for (String user : allUsers) {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("user", user);
                    userList.add(userMap);
                }
                responseMap.put("users", userList);
            }

            response = objectMapper.writeValueAsString(responseMap);
            System.out.println(response.toString());
            out.println(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleLogout(String username, PrintWriter out) {
        try {
            boolean serverResponse = server.removeActiveUser(username);
            String response = null;
            if (serverResponse) {
                response = "{\"success\": true, \"message\": \"Logout successful\"}";
            } else {
                response = "{\"success\": false, \"message\": \"Failed to logout user\"}";
            }
            out.println(response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleCreateConversation(String username, ArrayList<String> otherUsers, PrintWriter out) {
        try {
            boolean hadCreated = server.createConversation(username, otherUsers);
            String response = null;
            if(hadCreated) {
                response = "{\"success\": true}";
            }
            else{
                response = "{\"success\": false}";
            }
            out.println(response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleSendMessage(String jsonString, PrintWriter out) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            String username = jsonNode.path("username").asText();
            String message = jsonNode.path("message").asText();

            // Extract the list of members (handling the nested "member" field)
            ArrayList<String> members = new ArrayList<>();
            JsonNode membersNode = jsonNode.path("members");
            for (JsonNode memberNode : membersNode) {
                String member = memberNode.path("member").asText();  // Extract the "member" value
                members.add(member);
            }

            boolean serverResponse = server.sendMessage(username, message, members);
            String time = server.getTimeMessage(username, members, message);  // Get the time of the message
            System.out.println(time);
            // Prepare the response including the time
            String response;
            if (serverResponse) {
                response = String.format("{\"success\": true, \"time\": \"%s\"}", time);
            } else {
                response = "{\"success\": false}";
            }

            out.println(response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void handleGetMessages(String jsonString, PrintWriter out) {
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            String username = jsonNode.path("username").asText();

            // Initialize the ArrayList for members
            ArrayList<String> members = new ArrayList<>();

            // Extract the "members" array and iterate over each "member" object
            JsonNode membersNode = jsonNode.path("members");
            for (JsonNode memberNode : membersNode) {
                String member = memberNode.path("member").asText(); // Extract the "member" field
                members.add(member); // Add the member to the list
            }
            if (!members.contains(username)) {
                members.add(username);
            }
            ArrayList<ArrayList<String>> chatData = server.getMessages(members);
            Map<String, Object> responseMap = new HashMap<>();
            if (chatData != null) {
                List<Map<String, String>> formattedMessages = new ArrayList<>();
                for (ArrayList<String> messageData : chatData) {
                    Map<String, String> messageMap = new HashMap<>();
                    messageMap.put("sender", messageData.get(0));  // sender: [0]
                    messageMap.put("message", messageData.get(1)); // message: [1]
                    messageMap.put("time", messageData.get(2));    // time: [2]
                    formattedMessages.add(messageMap);
                }

                // Add the success and chat data to the response
                responseMap.put("success", true);
                responseMap.put("chat", formattedMessages);
            } else {
                // If no chat data found, set success: false
                responseMap.put("success", false);
                responseMap.put("chat", new ArrayList<>());
            }
            String responseJson = objectMapper.writeValueAsString(responseMap);
            out.println(responseJson);

        } catch (Exception e) {
            // Handle any exceptions by sending success: false
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("chat", new ArrayList<>());
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                out.println(errorJson);
            } catch (JsonProcessingException jsonException) {
                throw new RuntimeException(jsonException);
            }
        }
    }

    public void handleGetConversations(String username, PrintWriter out) {
        try {

            ObjectMapper objectMapper = new ObjectMapper();

            ArrayList<ArrayList<String>> chats = server.getExistedChats(username);
            List<Map<String, List<Map<String, String>>>> formattedChats = new ArrayList<>();

            for (ArrayList<String> chat : chats) {
                List<Map<String, String>> usersInChat = new ArrayList<>();

                // Create a map for each user in the chat
                for (String user : chat) {
                    Map<String, String> userMap = new HashMap<>();
                    userMap.put("user", user);
                    usersInChat.add(userMap);
                }

                // Add the chat to the list with key "chat"
                Map<String, List<Map<String, String>>> chatMap = new HashMap<>();
                chatMap.put("chat", usersInChat);
                formattedChats.add(chatMap);
            }

            // Create the response map with success status and the formatted chats
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);  // Assuming the operation was successful
            responseMap.put("chats", formattedChats);   // Include the chats in the response

            // Convert the map to a JSON string
            String response = objectMapper.writeValueAsString(responseMap);

            // Send the response
            out.println(response);

        } catch (Exception e) {
            // Handle the exception and return an error message
            e.printStackTrace();

            // Prepare the error response with success false
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error occurred: " + e.getMessage());

            // Send the error response
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String errorJson = objectMapper.writeValueAsString(errorResponse);
                out.println(errorJson);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


}
