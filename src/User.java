/**
 * Created by Avi on 2/22/15.
 */
import java.io.Serializable;
public class User implements Serializable{
    private String userName;
    private String password;

    public User(String userName, String password){
        this.userName = userName;
        this.password = password;
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
}
