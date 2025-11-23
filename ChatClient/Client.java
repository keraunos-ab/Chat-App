package ChatClient;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import com.google.gson.Gson;

import ChatClient.chatserver.models.Message;
import ChatClient.chatserver.models.User;

public class Client {
    @SuppressWarnings("unused")
    private static final int PORT = 51102;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    @SuppressWarnings("unused")
    private BufferedReader userInput;
    private Consumer<Message> incomingMessageListener;
    public int UserId;

    public void setIncomingMessageListener(Consumer<Message> listener)
    {
        this.incomingMessageListener = listener;
    }

    public Client(String address, int port) throws IOException
    {
        socket = new Socket(address, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        userInput = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start()
    {
        Thread listener = new Thread(() ->
        {
            try
            {
                String serverMsg;
                Gson gson = new Gson();
                
                while ((serverMsg = in.readLine()) != null)
                {
                    if (serverMsg.equals("INCOMING_MESSAGE"))
                    {
                        String json = in.readLine();
                        Message m = gson.fromJson(json, Message.class);
                        
                        if (incomingMessageListener != null)
                            incomingMessageListener.accept(m);
                    } else
                    {
                        System.out.println("Server Notification/Error: " + serverMsg);
                    }
                }
            }
            catch (IOException e)
            {
                System.out.println("Disconnected from server. Listener stopped.");
            }
        });
        listener.start();
    }

    private void close()
    {
        try
        {
            if (socket != null)
                socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    
    public boolean LogIn(String username, String password) throws IOException
    {
        synchronized (in)
        {
            out.println("LOGIN");
            out.println(username);
            out.println(password);
            
            String result = in.readLine();
            if (result != null && result.equals("SUCCESS"))
            {
                String idString = in.readLine();
                if (idString != null)
                {
                    try
                    {
                        UserId = Integer.parseInt(idString);
                        return true;
                    }
                    catch (NumberFormatException e)
                    {
                        System.err.println("Login: Invalid User ID received.");
                        return false;
                    }
                }
            }
            return false;
        }
    }

    public boolean SignUp(String FirstName, String LastName, String UserName, String password) throws IOException
    {
        synchronized (in)
        { 
            out.println("SIGNUP");
            out.println(FirstName);
            out.println(LastName);
            out.println(UserName);
            out.println(password);
            
            String result = in.readLine();
            return result != null && result.equals("SUCCESS");
        }
    }

    public List<Message> getConversation(int recipientId) throws IOException
    {
        System.out.println("loaded convo for "+UserId+" with "+recipientId);
        List<Message> conversation = List.of();
        synchronized (in)
        {
            out.println("GET_CONVO");
            out.println(recipientId);
            
            String json = in.readLine();
            
            if (json == null)
            {
                System.err.println("GET_CONVO: Received null response from server.");
                return List.of();
            }

            Gson Gson = new Gson();
            try
            {
                Message[] messageArray = Gson.fromJson(json, Message[].class);
                conversation = Arrays.asList(messageArray);
            }
            catch (Exception e)
            {
                System.err.println("GET_CONVO: Failed to deserialize JSON: " + json + ". Error: " + e.getMessage());
                return List.of();
            }
        }
        return conversation;
    }

    public void SendMessage(String message, int senderId, int recipientId)
    {
        out.println("MESSAGE");
        out.println(recipientId); 
        out.println(message);
        System.out.println("send message "+message+" to "+recipientId);
    }

    public void SetSeen(int recipientIds)
    {
        out.println("SET_SEEN");
        out.println(recipientIds);
    }

    public List<User> GetActiveUsers() throws IOException
    {
        List<User> activeUsers = List.of();
            out.println("GET_ACTIVE_USERS");
            String json = in.readLine();
            Gson gson = new Gson();
            User[] userArray = gson.fromJson(json, User[].class);
            activeUsers = Arrays.asList(userArray);
        return activeUsers;
    }

    public List<User> GetAllUsers() throws IOException
    {
        List<User> users = List.of();
        out.println("GET_ALL_USERS");
        String json = in.readLine();
        Gson gson = new Gson();
        User[] userArray = gson.fromJson(json, User[].class);
        users = Arrays.asList(userArray);
        return users;
    }
    
    public void LogOut()
    {
        out.println("LOGOUT");
        close();
    }
    
    public void BroadcastMessage(String message)
    {
        out.println("BROADCAST");
        out.println(message);
    }

    public void multiCastMessage(String message, List<Integer> recipientIds)
    {
        out.println("MULTICAST");
        out.println(message);
        Gson gson = new Gson();
        String ids = gson.toJson(recipientIds);
        out.println(ids);
    }
}