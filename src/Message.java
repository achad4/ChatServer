/**
 * Avi Chad-Friedman
 * ajc2212
 * Message class to encapsulate information sent in various commands
 */
import java.io.Serializable;
public class Message implements Serializable{

    public static final int DIRECT_MESSAGE = 0, BROADCAST = 1, LOGOUT = 2, PRIVATE = 3, GET_ADDRESS = 4,
                            BLOCK = 5, UNBLOCK = 6, HEART_BEAT = 7, ONLINE = 8, FRIEND = 9, UNFRIEND = 10;
    private int type;
    private String text;
    private String command;
    private User sender;
    private User recipient;


    //construct message from the client end
    public Message(String command, User sender){
        this.command = command;
        this.sender = sender;
        this.text = "";
    }

    public Message(){
        this.type = HEART_BEAT;
    }

    //construct message from server end
    public Message(String text, int type){
        this.text = text;
        this.type = type;
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
        else if(info[0].equals("private")){
            this.type = PRIVATE;
            for(int i = 2; i < info.length; i++){
                this.text += " " + info[i];
            }
            return true;
        }
        else if(info[0].equals("getaddress")){
            this.type = GET_ADDRESS;
            return true;
        }
        else if(info[0].equals("block")){
            this.type = BLOCK;
            return true;
        }
        else if(info[0].equals("unblock")){
            this.type = UNBLOCK;
            return true;
        }
        else if(info[0].equals("online")){
            this.type = ONLINE;
            return true;
        }
        else if(info[0].equals("friend")){
            this.type = FRIEND;
            return true;
        }
        else if(info[0].equals("unfriend")){
            this.type = UNFRIEND;
            return true;
        }
        return false;
    }

    public int getType(){
        return this.type;
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

