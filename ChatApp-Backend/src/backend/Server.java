package backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;

public class Server {
    private static final int HTTP_PORT = 8080;
    private static final int SOCKET_PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private final ExecutorService threadPool;
    private final GlobalVar globalVar = GlobalVar.getInstance();
    private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();


    public Server() {
        this.threadPool = new ThreadPoolExecutor(10, 50, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        try {
            // Start HTTP Server
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
            httpServer.createContext("/login", new LoginHandler());
            httpServer.createContext("/signUp", new SignUpHandler());
            httpServer.createContext("/chatWithServer", new ChatWithServerHandler());
            httpServer.createContext("/menuUser", new MenuHandler());
            httpServer.createContext("/sendMessage", new sendMessageHandler());
            httpServer.createContext("/getMessages", new getMessagesHandler());
            httpServer.createContext("/getConversations", new getConversationsHandler());
            httpServer.setExecutor(threadPool);
            httpServer.start();
            System.out.println("HTTP Server is running on port " + HTTP_PORT);

            // Start Raw Socket Server
            ServerSocket serverSocket = new ServerSocket(SOCKET_PORT);
            System.out.println("Socket Server is running on port " + SOCKET_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }

    private void processTasks() {
        while (true) {
            Runnable task = taskQueue.poll();
            if (task != null) {
                threadPool.execute(task);
            }
        }
    }




    public synchronized boolean checkLogin(String username, String password) {
        return globalVar.checkLogin(username, password);
    }

    public synchronized boolean checkExistUser(String username) {
        return globalVar.isUserExist(username);
    }

    public synchronized ArrayList<ArrayList<String>> fetchChatData(ArrayList<String> members, String username) {
        return globalVar.fetchChat(members, username);
    }

    public synchronized void addUser(String username, String password, String email, String appName){
        globalVar.addUser(username, password, email, appName);
    }

    public synchronized String getMessageFromServer(String username, String msg) {
        return "I received a message from " + username + " and the message is: " + msg;
    }

    public synchronized void addActiveUser(String username) {
        globalVar.addActiveUser(username);
    }

    public synchronized boolean removeActiveUser(String username) {
        return globalVar.removeActiveUser(username);
    }

    public synchronized ArrayList<String> getUsers(String username) {
        return globalVar.getRegisteredUsers(username);
    }

    public synchronized boolean createConversation(String username, ArrayList<String> otherUsers) {
        otherUsers.add(username); // add them together
        return globalVar.createConversation(otherUsers);
    }

    public synchronized boolean sendMessage(String username, String message, ArrayList<String> otherUsers) {
        return globalVar.sendMessageInConversation(username, otherUsers, message);
    }

    public synchronized ArrayList<ArrayList<String>> getMessages(ArrayList<String> otherUsers){
        return globalVar.getMessages(otherUsers);
    }

    public synchronized ArrayList<ArrayList<String>> getExistedChats(String username){
        return globalVar.getExistedChats(username);
    }

    public synchronized String getTimeMessage(String username, ArrayList<String> members, String message) {
        return globalVar.getTimeMessage(username, members, message);
    }

    static class LoginHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();

                // Send request body to socket server
                String socketResponse = "false"; // Default value if no response from socket server

                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("LOGIN-" + requestBody.toString()); // Send login data to socket server

                    // Read response from socket server (true or false)
                    socketResponse = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = "{" + socketResponse + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }

    static class SignUpHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                String socketResponse = "false";
                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("SIGNUP-" + requestBody.toString());
                    socketResponse = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = "{" + socketResponse + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }






    static class ChatWithServerHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                String socketResponse = "false";
                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("CHAT WITH SERVER-" + requestBody.toString());
                    socketResponse = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = "{ " + socketResponse + " }";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }


    static class MenuHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                String socketResponse = "false";
                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    System.out.println(requestBody.toString());
                    out.println("MENU-" + requestBody.toString());
                    socketResponse = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = socketResponse;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }

    static class sendMessageHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                System.out.println("sendMessageHandlerServer");
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                String socketResponse = "false";
                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("SEND MESSAGE-" + requestBody.toString());
                    socketResponse = in.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = socketResponse;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }

    static class getMessagesHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                String socketResponse = "false";
                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    System.out.println(requestBody.toString());
                    out.println("GET MESSAGES-" + requestBody.toString());
                    socketResponse = in.readLine();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = socketResponse;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }

    static class getConversationsHandler implements HttpHandler {
        public synchronized void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                br.close();
                String socketResponse = "false";
                try (Socket socket = new Socket("localhost", SOCKET_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    System.out.println(requestBody.toString());
                    out.println("GET CONVERSATIONS-" + requestBody.toString());
                    socketResponse = in.readLine();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Prepare response based on socket server result
                String response = socketResponse;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }




}