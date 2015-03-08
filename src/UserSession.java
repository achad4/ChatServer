import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by Avi on 3/7/15.
 */
public class UserSession implements Serializable{
    private InetAddress iP;
    private int portNumber;

    public UserSession(InetAddress iP, int portNumber){
        this.iP = iP;
        this.portNumber = portNumber;
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

}
