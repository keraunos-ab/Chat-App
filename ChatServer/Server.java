package ChatServer;
import java.io.*;
import java.net.*;
import java.util.*;

import com.google.gson.Gson;

import ChatClient.chatserver.models.Message;
import ChatClient.chatserver.models.User;

public class Server
{
    private static final int PORT = 51102;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static DatabaseManager dbManager;

    public static void main(String[] args)
    {
        dbManager = new DatabaseManager();
        dbManager.InitializeDatabase();

        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            System.out.println("Server started on port " + PORT);
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, dbManager);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void removeClient(ClientHandler clientHandler)
    {
        clients.remove(clientHandler);
        System.out.println("Client disconnected. Active clients: " + clients.size());
    }

    static class ClientHandler implements Runnable
    {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private DatabaseManager dbManager;
        private Integer userId;

        public ClientHandler(Socket socket, DatabaseManager dbManager)
        {
            this.socket = socket;
            this.dbManager = dbManager;
            try
            {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            try
            {
                System.out.println("Connected to ChatApp Server!");
                String message;
                while ((message = in.readLine()) != null)
                {
                    System.out.println("Received: " + message);
                    handleCommand(message);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (userId != null)
                {
                    dbManager.setUserStatus(userId, false);
                }
                Server.removeClient(this);
                try
                {
                    in.close();
                    out.close();
                    socket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        private void handleCommand(String command) throws IOException
        {
            switch (command)
            {
                case "SIGNUP":
                    String firstName = in.readLine();
                    String lastName = in.readLine();
                    String email = in.readLine();
                    String password = in.readLine();
                    boolean validSignup = dbManager.addUser(firstName, lastName, email, password);
                    System.out.println("Signup attempt for: " + email + " was" + (validSignup ? " successful." : " unsuccessful."));
                    out.println(validSignup ? "SUCCESS" : "FAILURE");
                    break;
                case "LOGIN":
                    String username = in.readLine();
                    String Password = in.readLine();
                    boolean validLogin = dbManager.validateLogIn(username, Password);
                    if (validLogin)
                    {
                        int id = dbManager.getUserId(username);
                        this.userId = id;
                        out.println("SUCCESS");
                        out.println(id);
                        dbManager.setUserStatus(id, true);
                    }
                    else
                    {
                        out.println("FAILURE");
                        out.println("-1");
                    }
                    break;
                case "MESSAGE":
                    int recipientId = Integer.parseInt(in.readLine());
                    String msgText = in.readLine();
                    if (this.userId == null)
                    {
                        System.out.println("ERROR: MESSAGE received but sender not logged in.");
                        break;
                    }
                    dbManager.sendMessage(msgText, this.userId, recipientId);
                    System.out.println("send message :"+msgText+" to "+recipientId);
                    break;
                case "LOGOUT":
                    dbManager.setUserStatus(userId, false);
                    break;
                case "BROADCAST":
                    String broadcastMsg = in.readLine();
                    dbManager.broadcastMessage(userId, broadcastMsg);
                    break;
                case "GET_ACTIVE_USERS":
                    List<User> activeUserIds = dbManager.getActiveUsers();
                    Gson gsonActive = new Gson();
                    String activeJson = gsonActive.toJson(activeUserIds);
                    out.println(activeJson);
                    break;
                case "GET_ALL_USERS":
                    List<User> allUsers = dbManager.getAllUsers();
                    Gson gson = new Gson();
                    String allJson = gson.toJson(allUsers);
                    out.println(allJson);
                    break;
                case "MULTICAST":
                    String multiMsg = in.readLine();
                    String json = in.readLine();
                    Gson gsonIds = new Gson();
                    Integer[] ids = gsonIds.fromJson(json, Integer[].class);
                    List<Integer> recipientIds = Arrays.asList(ids);
                    dbManager.multicastMessage(userId, multiMsg, recipientIds);
                    break;
                case "GET_CONVO":
                    int convoRecepient = Integer.parseInt(in.readLine());
                    Gson convoGson = new Gson();
                    List<Message> conversation = dbManager.loadConversation(userId, convoRecepient);
                    String convoJson = convoGson.toJson(conversation);
                    out.println(convoJson);
                    System.out.println("fetched convo for : "+userId+" and "+convoRecepient);
                    break;
                case "SET_SEEN":
                    int seenRecepient = Integer.parseInt(in.readLine());
                    dbManager.setMessagesSeen(userId, seenRecepient);
                    break;
                default:
                    out.println("Unknown command.");
                    break;
            }
        }
    }
}