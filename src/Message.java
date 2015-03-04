import java.io.Serializable;
/**
 * Created by Avi on 2/22/15.
 */
public class Message implements Serializable{

    public static final int DIRECT_MESSAGE = 0, BROADCAST = 1, LOGOUT = 2;
    private int type;
    public String text;
    public User sender;
    private User recipient;

    public Message(String text, User sender){
        this.text = text;
        this.sender = sender;
    }

    public Boolean parseMessage(){
        String[] info = this.text.split(" ");
        if(info[0].equals("message")){
            this.type = DIRECT_MESSAGE;
            return true;
        }
        else if(info[0].equals("broadcast")){
            this.type = BROADCAST;
            return true;
        }
        else if(info[0].equals("logout")){
            this.type = LOGOUT;
            return true;
        }
        return false;
    }
     public int getType(){
         return this.type;
     }

    public User getRecipient(){
        return this.recipient;
    }

    public void setRecipient(User recipient){
        this.recipient = recipient;
    }
}

