/**
 * Avi Chad-Friedman
 * ajc2212
 * UserSession class is used by the server to store state of clients
 */

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
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

    public InetAddress getiP(){
        return this.iP;
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
