/**
 * Created by Avi on 2/22/15.
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    public int portNumber;
    public ArrayList<UserThread> clients;

    public Server(int portNumber){
        this.portNumber = portNumber;
        this.clients = new ArrayList<UserThread>();
    }

    public void runServer(){
        Boolean runServer = true;
        try {
            ServerSocket serverSock = new ServerSocket(this.portNumber);
            while(runServer){
                Socket clientSock = serverSock.accept();
                System.out.println("new thread");
                //create a new thread to handle the user
                UserThread thread = new UserThread(clientSock, this);
                this.clients.add(thread);
                thread.start();
                System.out.println("user handled");
            }
            try{
                serverSock.close();
                for(UserThread cThread : this.clients){
                    cThread.in.close();
                    cThread.out.close();
                    cThread.socket.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //search for the thread with the right user and send the text
    public synchronized Boolean writeDirectMessage(String text, User recipient){
        System.out.println("handling DM");
        for(UserThread t : this.clients){
            System.out.println(recipient.getUserName());
            if(t.user.getUserName().equals(recipient.getUserName())){
                System.out.print("user:" + t.user.getUserName());
                return t.writeMessage(text);
            }
        }
        return true;
    }

    class UserThread extends Thread {
        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;
        Server server;
        User user;

        public UserThread(Socket socket, Server server){
            this.socket = socket;
            //this.server = server;
            try {
                this.out = new ObjectOutputStream(socket.getOutputStream());
                //flush output to unblock client side input stream
                this.out.flush();
                this.in = new ObjectInputStream(socket.getInputStream());

                //Message message = (Message) in.readObject();
                //this.user = message.sender;
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            */
        }

        public void run() {
            for(;;){
                try {
                    if(this.user != null) {
                        //obtain the message object from the input stream
                        Message message = (Message) in.readObject();
                        if (message.getType() == message.DIRECT_MESSAGE) {
                            this.handleDirectMessage(message);
                        }
                    }
                    else {
                        this.user = (User) in.readObject();
                        System.out.println(this.user.getUserName());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }


        //write a message to the user
        public Boolean writeMessage(String text){
            try {
                System.out.println("sending DM");
                this.out.writeObject(text);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        //handle a message request to a user
        public void handleDirectMessage(Message message){
            writeDirectMessage(message.text, message.getRecipient());
        }


    }
}

