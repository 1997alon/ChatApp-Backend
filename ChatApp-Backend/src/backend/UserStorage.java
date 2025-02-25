package backend;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserStorage {
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/chatApp";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1997Alon";

    public UserStorage() {
    }

    public static Map<String, User> loadUsers() {
        Map<String, User> users = new HashMap<>();
        String query = "SELECT * FROM user";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                String appName = resultSet.getString("appName");
                String email = resultSet.getString("email");
                User user = new User(username, password, email, appName);
                users.put(username, user);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return users;
    }

    public static void saveUser(User user) {
        String query = "INSERT INTO user (username, password, email, appName) VALUES (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getAppName());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User added successfully!");
            } else {
                System.out.println("Failed to add user.");
            }
        } catch (SQLException e) {
            System.out.println("Error saving user to MySQL: " + e.getMessage());
        }
    }

    public static boolean userExists(Map<String, User> users, String username) {
        String query = "SELECT 1 FROM user WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.out.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }


    public static Integer createConversation(ArrayList<User> users) {
        if (users == null || users.isEmpty() || users.size() == 1) {
            System.out.println("No users provided.");
            return null;
        }

        Connection conn = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;

        try {
            // 1. Establish connection
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
            conn.setAutoCommit(false); // Enable transaction control

            // 2. Insert into conversations table
            String createConversationSQL = "INSERT INTO conversations (is_group, created_at) VALUES (?, NOW())";
            stmt1 = conn.prepareStatement(createConversationSQL, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt1.setBoolean(1, users.size() > 1); // If more than 1 user, it's a group
            stmt1.executeUpdate();

            // 3. Get generated conversation_id
            rs = stmt1.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("Failed to retrieve conversation ID.");
            }
            Integer conversationId = rs.getInt(1);

            // 4. Insert users into conversation_members table
            String addMemberSQL = "INSERT INTO conversation_members (conversation_id, username, joined_at) VALUES (?, ?, NOW())";
            stmt2 = conn.prepareStatement(addMemberSQL);

            for (User user : users) {
                if (user.getUsername() == null || user.getUsername().isEmpty()) {
                    throw new SQLException("User has no valid username.");
                }
                stmt2.setInt(1, conversationId);
                stmt2.setString(2, user.getUsername());
                stmt2.addBatch();
            }
            stmt2.executeBatch();

            // 5. Commit transaction
            conn.commit();
            System.out.println("Conversation created successfully with ID: " + conversationId);
            return conversationId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback in case of failure
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return null;
        } finally {
            // 6. Close resources
            try {
                if (rs != null) rs.close();
                if (stmt1 != null) stmt1.close();
                if (stmt2 != null) stmt2.close();
                if (conn != null) conn.close();
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }


    public static boolean addMessageToConversation(Integer conversationId, String sender, String message) {
        if (conversationId == null || sender == null || sender.isEmpty() || message == null || message.isEmpty()) {
            System.out.println("Invalid input: conversationId, sender, and message are required.");
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Establish connection
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);

            // SQL query to insert a message into conversation_data
            String sql = "INSERT INTO conversation_data (conversation_id, sender, message) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, conversationId);
            stmt.setString(2, sender);
            stmt.setString(3, message);
            stmt.executeUpdate();

            System.out.println("Message added successfully to conversation ID: " + conversationId);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();

            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
        return true;
    }

    public static String getTimeOfMessage(String username, Integer conversationID, String message) {
        String time = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
            String query = "SELECT sent_at FROM conversation_data WHERE sender = ? AND conversation_id = ? AND message = ? ORDER BY sent_at DESC LIMIT 1";
            stmt = conn.prepareStatement(query);

            stmt.setString(1, username);
            stmt.setInt(2, conversationID);
            stmt.setString(3, message);

            rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp sentAt = rs.getTimestamp("sent_at");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                time = sdf.format(sentAt);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Log the error in real applications
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return time;
    }

    public static Integer findConversationID(ArrayList<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            System.out.println("No usernames provided.");
            return null;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 1. Establish connection
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);

            // 2. Create SQL query to find the conversation where all users are members
            StringBuilder query = new StringBuilder("SELECT conversation_id FROM conversation_members WHERE username IN (");
            for (int i = 0; i < usernames.size(); i++) {
                query.append("?");
                if (i < usernames.size() - 1) {
                    query.append(", ");
                }
            }
            query.append(") GROUP BY conversation_id HAVING COUNT(DISTINCT username) = ?");

            // Prepare the statement
            stmt = conn.prepareStatement(query.toString());

            // Set values for the usernames
            for (int i = 0; i < usernames.size(); i++) {
                stmt.setString(i + 1, usernames.get(i));
            }

            // Set the number of usernames (this should match the number of rows for the conversation)
            stmt.setInt(usernames.size() + 1, usernames.size());

            // 3. Execute query
            rs = stmt.executeQuery();

            // 4. If we get a result, we need to ensure that no extra users are in the conversation
            if (rs.next()) {
                int conversationId = rs.getInt("conversation_id");

                // Now, check that the conversation has no more users than the ones in the usernames list
                String countQuery = "SELECT COUNT(DISTINCT username) FROM conversation_members WHERE conversation_id = ?";
                PreparedStatement countStmt = conn.prepareStatement(countQuery);
                countStmt.setInt(1, conversationId);
                ResultSet countRs = countStmt.executeQuery();

                if (countRs.next() && countRs.getInt(1) == usernames.size()) {
                    // If the number of distinct users matches exactly, return the conversation_id
                    return conversationId;
                }
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 5. Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }


    public static ArrayList<ArrayList<String>> getConversationData(Integer conversationID) {
        if (conversationID == null) {
            System.out.println("Invalid conversation ID.");
            return null;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<ArrayList<String>> conversationData = new ArrayList<>();

        try {
            // 1. Establish connection
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);

            // 2. Create SQL query to fetch all messages in the conversation
            String getMessagesSQL = "SELECT sender, message, sent_at FROM conversation_data WHERE conversation_id = ? ORDER BY sent_at ASC";
            stmt = conn.prepareStatement(getMessagesSQL);
            stmt.setInt(1, conversationID); // Set the conversation ID in the query

            // 3. Execute the query
            rs = stmt.executeQuery();

            // 4. Process the result set and store the messages in the map
            while (rs.next()) {
                String sender = rs.getString("sender");
                String message = rs.getString("message");
                Timestamp sentAt = rs.getTimestamp("sent_at");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = sdf.format(sentAt);
                ArrayList<String> toAdd = new ArrayList<>();
                toAdd.add(sender);
                toAdd.add(message);
                toAdd.add(time);
                conversationData.add(toAdd);
            }

            // 5. Return the map containing sender-message pairs
            return conversationData;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 6. Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }

    public static Map<Integer, ArrayList<String>> getConversationMembers() {
        Map<Integer, ArrayList<String>> conversationMembers = new HashMap<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 1. Establish connection
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);

            // 2. SQL query to get conversation_id and associated usernames from conversation_members table
            String sql = "SELECT conversation_id, username FROM conversation_members";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // 3. Process the results
            while (rs.next()) {
                int conversationId = rs.getInt("conversation_id");
                String username = rs.getString("username");
                System.out.println("Conversation ID: " + conversationId + ", Username: " + username);  // Debugging line
                // 4. Add the username to the corresponding conversation ID list
                conversationMembers.putIfAbsent(conversationId, new ArrayList<>()); // Initialize list if it doesn't exist
                conversationMembers.get(conversationId).add(username);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 5. Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // 6. Return the map of conversation members
        return conversationMembers;
    }

    public static void deleteSingleMemberConversations() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // 1. Establish connection
            conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);

            // 2. Disable auto-commit to manage transactions manually
            conn.setAutoCommit(false);

            // 3. SQL query to find conversation_ids with only one member
            String sql = "SELECT conversation_id FROM conversation_members GROUP BY conversation_id HAVING COUNT(username) = 1";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // 4. Iterate through the result set and delete related records
            while (rs.next()) {
                int conversationId = rs.getInt("conversation_id");

                // 5. Delete data from conversation_members, conversation_data, and conversations tables
                String deleteMembersSql = "DELETE FROM conversation_members WHERE conversation_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteMembersSql)) {
                    deleteStmt.setInt(1, conversationId);
                    deleteStmt.executeUpdate();
                }

                String deleteDataSql = "DELETE FROM conversation_data WHERE conversation_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteDataSql)) {
                    deleteStmt.setInt(1, conversationId);
                    deleteStmt.executeUpdate();
                }

                String deleteConversationSql = "DELETE FROM conversations WHERE conversation_id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteConversationSql)) {
                    deleteStmt.setInt(1, conversationId);
                    deleteStmt.executeUpdate();
                }

                System.out.println("Deleted conversation with ID: " + conversationId);
            }

            // 6. Commit the transaction to make sure changes are saved
            conn.commit();

        } catch (SQLException e) {
            // 7. Rollback the transaction in case of error
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            // 8. Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Re-enable auto-commit
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }




}


