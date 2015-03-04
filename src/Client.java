/**
 * Created by Avi on 2/23/15.
 */
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    public ObjectInputStream in;
    public ObjectOutputStream out;
    public int portNumber;
    public String address;
    public Socket sock;
    public ArrayList<User> users;
    private Boolean loggedIn;

    public Client(String address, int portNumber){
        this.portNumber = portNumber;
        this.address = address;
        users = new ArrayList<User>();
        this.populateUsers();
        this.loggedIn = false;
    }

    public void writeMessage(Message message){
        try {
            out.writeObject(message);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    publiv Boolean start(){
        sock = new Socket(address, portNumber);
        System.out.println(address);
        Scanner scan = new Scanner(System.in);
        in = new ObjectInputStream(sock.getInputStream());
        out = new ObjectOutputStream(sock.getOutputStream());
        new ClientThread().start();
        while(!loggedIn){
            System.out.print("Username: ");
            String username = scan.next();
            out.writeObject(username);
            System.out.print("Password: ");
            String password = scan.next();
            out.writeObject(password);
        }

    }

    public void runClient(){

        try {
            User user;
            if((user = loginUser(username, password)) != null){
                //send user to the server
                out.writeObject(user);
                //wait for commands from the user
                for(;;){
                    System.out.print(">");
                    String command = scan.nextLine();
                    Message message = new Message(command, user);
                    if(message.parseMessage()){
                        out.writeObject(message);
                    }
                    else {
                        System.out.print(">Invalid command");
                    }
                }
            }
        }
        catch(UnknownHostException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }


    public void setLoggedIn(Boolean loggedIn){
        this.loggedIn = loggedIn;
    }


    class ClientThread extends Thread{
        public void run(){
            for(;;){
                try{
                    String message = (String) in.readObject();
                    System.out.println(message+"\n");
                } catch(IOException e) {
                    e.printStackTrace();
                    break;
                } catch(ClassNotFoundException e) {
                   e.printStackTrace();
                }
            }
        }
    }
}
