/**
 * Avi Chad-Friedman
 * ajc2212
 * User class represents a user with a username and password
 */
import java.io.Serializable;
public class User implements Serializable{
    private String userName;
    private String password;
    private Boolean blocked;

    public User(String userName, String password){
        this.userName = userName;
        this.password = password;
        this.blocked = false;
    }

    public Boolean verifyUsername(String userName){
        if(userName.equals(this.userName))
            return true;
        return false;
    }

    public Boolean verifyPassword(String password){
        if(password.equals(this.password))
            return true;
        return false;
    }

    public String getUserName(){
        return this.userName;
    }


    //time the user out for 60 seconds
    public void block(){
        this.blocked = true;
        try{
            Thread.sleep(60000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        this.blocked = false;
    }

    public Boolean isBlocked(){
        return this.blocked;
    }
}
