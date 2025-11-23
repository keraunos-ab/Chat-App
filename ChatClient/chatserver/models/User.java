package ChatClient.chatserver.models;
import java.io.Serializable;

public class User implements Serializable
{
    private int Id;
    private String firstName;
    private String lastName;
    private String UserName;
    private String password;
    private boolean isOnline;

    public User(int id,String firstName, String lastName, String UserName, String password)
    {
        this.Id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.UserName = UserName;
        this.password = password;
        this.isOnline = false;
    }
    
    public int getId() { return Id; }
    public String getFirstName() {return firstName;}
    public String getLastName() {return lastName;}
    public String getUserName() {return UserName;}
    public String getPassword() {return password;}
    public boolean isOnline() {return isOnline;}
    public void setOnline(boolean online) {isOnline = online;}

    @Override
    public String toString()
    {
        return "User{" +"Id=" + Id + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + ", UserName='" + UserName + '\'' + ", isOnline=" + isOnline + '}';
    }
}
