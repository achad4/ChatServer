import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

/**
 * Created by Avi on 3/7/15.
 */
public class UserSession implements Serializable{
    private User user;
    private InetAddress iP;
    private int portNumber;
    private Date lastHeartBeat;

    public UserSession(InetAddress iP, int portNumber){
        this.iP = iP;
        this.portNumber = portNumber;
        setLastHeartBeat();
    }

    public void setiP(InetAddress iP){
        this.iP = iP;
    }

    public InetAddress getiP(){
        return this.iP;
    }

    public void setPortNumber(int portNumber){
        this.portNumber = portNumber;
    }

    public int getPortNumber(){
        return this.portNumber;
    }

    public void setUser(User user){
        this.user = user;
    }

    public User getUser(){
        return this.user;
    }

    public void setLastHeartBeat(){
        this.lastHeartBeat = new Date();
    }

    public Date getLastHeartBeat(){
        return this.lastHeartBeat;
    }
}
