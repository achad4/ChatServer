import java.io.Serializable;
/**
 * Created by Avi on 2/22/15.
 */
public class Message implements Serializable{

    public static final int DIRECT_MESSAGE = 0, BROADCAST = 1, LOGOUT = 2;
    private int type;
    private String text;
    private String command;
    private User sender;
    private User recipient;

    public Message(String command, User sender){
        this.command = command;
        this.sender = sender;
        this.text = "";
    }

    public Boolean parseMessage(){
        String[] info = this.command.split(" ");
        if(info[0].equals("message")){
            this.type = DIRECT_MESSAGE;
            //concatenate the body of the message to the text field
            for(int i = 2; i < info.length; i++){
                this.text += " " + info[i];
            }
            return true;
        }
        else if(info[0].equals("broadcast")){
            this.type = BROADCAST;
            for(int i = 1; i < info.length; i++){
                this.text += " " + info[i];
            }
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

    public User getSender(){
        return this.sender;
    }

    public String getText(){
        return this.text;
    }

    public String getCommand(){
        return this.command;
    }
    public void setRecipient(User recipient){
        this.recipient = recipient;
    }

    public void setSender(User sender){
        this.sender = sender;
    }
}

