package ChatClient.chatserver.models;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable
{
    private int Id;
    private int senderId;
    private int receiverId;
    private String content;
    private String timestamp;
    private boolean delivered;
    private boolean read;

    public Message(int id, int senderId, int receiverId, String content, LocalDateTime timestamp, boolean delivered, boolean seen)
    {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp.toString();
        this.delivered = delivered;
        this.read = seen;
    }

    public int getId() {return Id;}
    public int getSenderId() {return senderId;}
    public int getReceiverId() {return receiverId;}
    public String getContent() {return content;}
    public String getTimestamp() {return timestamp.toString();}
    public boolean isDelivered() {return delivered;}
    public boolean isSeen() {return read;}
    public void setDelivered(boolean delivered) {this.delivered = delivered;}
    public void setSeen(boolean read) {this.read = read;}

    @Override
    public String toString()
    {
        return "[" + timestamp + "] User " + senderId + " → " + receiverId + ": " + content;
    }
}