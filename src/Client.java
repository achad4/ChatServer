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

    public Client(String address, int portNumber){
        this.portNumber = portNumber;
        this.address = address;
        users = new ArrayList<User>();
        this.populateUsers();
    }

    public void populateUsers(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("credentials"));
            String line;
            while((line = br.readLine()) != null) {
                String[] userInfo = line.split(",");
                users.add(new User(userInfo[0], userInfo[1]));
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void writeMessage(Message message){
        try {
            out.writeObject(message);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }


    public void runClient(){

        try {

            sock = new Socket(address, portNumber);
            System.out.println(address);
            Scanner scan = new Scanner(System.in);
            in = new ObjectInputStream(sock.getInputStream());
            out = new ObjectOutputStream(sock.getOutputStream());
            //new ClientThread().listen();
            System.out.print("Username: ");
            String username = scan.next();
            System.out.print("Password: ");
            String password = scan.next();
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
                        if(message.getType() == message.DIRECT_MESSAGE){
                            String[] info = command.split(" ");
                            message.setRecipient(findRecipient(info[1]));
                        }
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
    private User findRecipient(String username){
        for(User u : this.users){
            if(u.getUserName().equals(username))
                return u;
        }
        return null;
    }

    public User loginUser(String username, String password){
        for(User u : users){
            if(u.verifyPassword(password) && u.verifyUsername(username))
                return u;

        }
        return null;
    }

    class ClientThread extends Thread{
        public void run(){
            for(;;){
                try{
                    String message = (String) in.readObject();
                    System.out.println(message+"\n");
                } catch(IOException e) {
                    e.printStackTrace();
                } catch(ClassNotFoundException e) {
                   e.printStackTrace();
                }

            }
        }
    }
}
