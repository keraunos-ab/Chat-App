package ChatServer;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ChatClient.chatserver.models.Message;
import ChatClient.chatserver.models.User;

public class DatabaseManager
{
    private static final String URL = "jdbc:postgresql://localhost:5432/ChatAppServer";
    private static final String USER = "postgres";
    private static final String PASSWORD = "blaze123";

    static
    {
        try
        {
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void InitializeDatabase()
    {
        String query = "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY," +
                "first_name VARCHAR(50) NOT NULL," +
                "last_name VARCHAR(50) NOT NULL," +
                "username VARCHAR(100) UNIQUE NOT NULL," +
                "password VARCHAR(100) NOT NULL," +
                "status BOOLEAN DEFAULT FALSE," +
                "profile_picture_path VARCHAR(255)" +
                ");" +
                "CREATE TABLE IF NOT EXISTS messages (" +
                "id SERIAL PRIMARY KEY," +
                "sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE," +
                "receiver_id INTEGER REFERENCES users(id) ON DELETE CASCADE," +
                "message TEXT NOT NULL," +
                "timestamp TIMESTAMP," +
                "delivered BOOLEAN DEFAULT FALSE," +
                "read BOOLEAN DEFAULT FALSE" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement())
        {
            stmt.executeUpdate(query);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public boolean addUser(String firstName, String lastName, String UserName, String password)
    {
        String lowerFirst = firstName.toLowerCase();
        String lowerLast = lastName.toLowerCase();
        String lowerUserName = UserName.toLowerCase();

        String checkSql = "SELECT 1 FROM users WHERE (first_name = ? AND last_name = ?) OR username = ?";
        String insertSql = "INSERT INTO users (first_name, last_name, username, password) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql))
        {

            checkStmt.setString(1, lowerFirst);
            checkStmt.setString(2, lowerLast);
            checkStmt.setString(3, lowerUserName);

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next())
            {
                System.out.println("returned false for adding user of name : " + lowerFirst);
                return false;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql))
            {
                insertStmt.setString(1, lowerFirst);
                insertStmt.setString(2, lowerLast);
                insertStmt.setString(3, lowerUserName);
                insertStmt.setString(4, password);
                insertStmt.executeUpdate();
                System.out.println("returned true to adding user of name : " + lowerFirst);
                return true;
            }
        }
        catch (SQLException e)
        {
            System.out.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    public int getUserId(String username)
    {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query))
        {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
            {
                return rs.getInt("id");
            }
            else 
            {
                
                return -1;
            }
        } 
        catch (SQLException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public List<Integer> getActiveUserIds()
    {
        String query = "SELECT id FROM users WHERE status = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery())
        {

            List<Integer> activeUserIds = new ArrayList<>();
            while (rs.next())
            {
                activeUserIds.add(rs.getInt("id"));
            }
            return activeUserIds;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<User> getActiveUsers()
    {
        String query = "SELECT * FROM users WHERE status = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery())
        {

            List<User> activeUsers = new ArrayList<>();
            while (rs.next())
            {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                activeUsers.add(user);
            }
            return activeUsers;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<User> getAllUsers()
    {
        String query = "SELECT * FROM users";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery())
        {

            List<User> allUsers = new ArrayList<>();
            while (rs.next())
            {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                allUsers.add(user);
            }
            return allUsers;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean validateLogIn(String UserName, String password)
    {
        String lowerUserName = UserName.toLowerCase();

        String sql = "SELECT 1 FROM users WHERE LOWER(username) = ? AND password = ?";
        try (Connection conn = getConnection();PreparedStatement stmt = conn.prepareStatement(sql))
        {
            stmt.setString(1, lowerUserName);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public void setMessagesSeen(int userId, int recepientID)
    {
        String query = "UPDATE messages SET read = TRUE WHERE sender_id = ? AND receiver_id = ? AND read = false AND delivered = true";
        System.out.println("set "+recepientID+" message's to seen for user"+userId);
        try (Connection conn = getConnection();PreparedStatement stmt = conn.prepareStatement(query))
        {
            stmt.setInt(1, recepientID);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void setMessageDelivered(int messageId)
    {
        String query = "UPDATE messages SET delivered = true WHERE id = ?";
        try (Connection conn = getConnection();PreparedStatement stmt = conn.prepareStatement(query))
        {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void setUserStatus(int id, boolean status)
    {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();PreparedStatement stmt = conn.prepareStatement(sql))
        {
            stmt.setBoolean(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public boolean userExists(int id)
    {
        String sql = "SELECT 1 FROM users WHERE id = ?";
        try (Connection conn = getConnection();PreparedStatement stmt = conn.prepareStatement(sql))
        {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public List<Message> loadConversation(int userId1, int userId2)
    {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY timestamp ASC";
        try (Connection conn = getConnection();PreparedStatement stmt = conn.prepareStatement(query))
        {

            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            stmt.setInt(3, userId2);
            stmt.setInt(4, userId1);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
            {
                Message message = new Message(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getString("message"),
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getBoolean("delivered"),
                        rs.getBoolean("read")
                );
                messages.add(message);
            }
            System.out.println("conversation between "+userId1+" and "+userId2+" leaded from the db");
            return messages;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return messages;
        }
        
    }

    public void sendMessage(String message, int senderId, int receiverId)
    {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message, timestamp, delivered, read) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
            {
            
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            stmt.setString(3, message);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(5, false);
            stmt.setBoolean(6, false);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0)
            {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys())
                {
                    if (generatedKeys.next())
                    {
                        int messageId = generatedKeys.getInt(1);
                        setMessageDelivered(messageId);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Message '" + message + "' sent to user " + receiverId + " saved to database.");
    }

    public void broadcastMessage(int senderId, String message)
    {
        for (Integer userId : getActiveUserIds())
        {
            if (!userId.equals(senderId))
            {
                sendMessage(message, senderId, userId);
            }
        }
    }

    public void multicastMessage(int senderId, String message, List<Integer> recipientIds)
    {
        for (Integer userId : recipientIds)
    {
            if (!userId.equals(senderId) && userExists(userId))
            {
                sendMessage(message, senderId, userId);
            }
        }
    }
}