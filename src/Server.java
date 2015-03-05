/**
 * Created by Avi on 2/22/15.
 */
import java.io.*;
import java.lang.Exception;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class Server {
    public int portNumber;
    private ArrayList<UserThread> clients;
    private ArrayList<User> users;
    private LinkedList<Message> messageQueue;
    public static final int LOGGED_IN = 0, LOGGED_OUT = 1, TIMED_OUT = 2;

    public Server(int portNumber){
        this.portNumber = portNumber;
        this.clients = new ArrayList<UserThread>();
        this.users = new ArrayList<User>();
        this.messageQueue = new LinkedList<Message>();
        populateUsers();
    }

    public void runServer(){
        try {
            ServerSocket serverSock = new ServerSocket(this.portNumber);
            for(;;){
                Socket clientSock = serverSock.accept();
                //create a new thread to handle the user
                UserThread thread = new UserThread(clientSock, this);
                this.clients.add(thread);
                thread.start();
            }
            /*
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
            */
        } catch (IOException e) {
            e.printStackTrace();
        }
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



    //search for the thread with the right user and send the text
    public synchronized Boolean writeDirectMessage(String text, User recipient){
        for(UserThread t : this.clients){
            if(t.user.getUserName().equals(recipient.getUserName())){
                return t.writeMessage(text);
            }
        }
        return true;
    }

    public synchronized void removeThread(long threadId){
        for(UserThread t : this.clients){
            if(t.getId() == threadId)
                this.clients.remove(t);
        }
    }

    public User loginUser(String username, String password){
        for(User u : users){
            if(u.verifyPassword(password) && u.verifyUsername(username)) {
                /*
                for(UserThread t : this.clients){
                    if(t.user.getUserName().equals(u.getUserName())){
                        //user is already logged in
                        t.writeMessage("Someone attempted to log in with your credentials");
                        t.logout();
                        removeThread(t.getId());
                        return null;
                    }
                }
                */
                return u;
            }
        }
        return null;
    }

    private User findUser(String username){
        for(User u : this.users){
            if(u.getUserName().equals(username))
                return u;
        }
        return null;
    }

    class UserThread extends Thread {
        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;
        Server server;
        User user;
        //info the client passes to log in a user
        private String userName;
        private String password;

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

        private User handleLogin(){
            try {
                int attempts = 0;
                while (this.user == null) {
                    String username = (String) in.readObject();
                    String password = (String) in.readObject();
                    User u;
                    if ((u = loginUser(username, password)) != null) {
                        return u;
                    }
                    if(attempts >= 2){
                        out.writeObject(TIMED_OUT);
                        attempts = 0;
                    }
                    else {
                        out.writeObject(LOGGED_OUT);
                    }
                    attempts++;
                }

            } catch (IOException e){
                e.printStackTrace();
            }
            catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            return null;
        }

        public void run() {
            try{
                if(this.user == null) {
                    if((this.user = handleLogin()) != null) {
                        out.writeObject(LOGGED_IN);
                    }
                    else{
                        out.writeObject(false);
                    }
                }
                Boolean handleClient = true;
                while(handleClient) {
                    Message message;
                    //obtain the message from the users pending message queue
                    if(!messageQueue.isEmpty()){
                        message = messageQueue.remove();
                    }
                    //obtain the message object from the input stream
                    else {
                        message = (Message) in.readObject();
                    }
                    message.setSender(this.user);
                    switch (message.getType()) {
                        case Message.DIRECT_MESSAGE:
                            this.handleDirectMessage(message);
                            break;
                        /*
                        case message.BROADCAST:
                            this.handleBroadcast(message);
                            break;
                         */
                        case Message.LOGOUT:
                            this.logout();
                            break;
                    }
                    //make the user wait before attempting a fourth login
                }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            logout();
        }



        //close sockets and remove the thread from the client list
        private void logout(){
            removeThread(this.getId());
            try {
                in.close();
                out.close();
                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //write a message to the user
        public Boolean writeMessage(String text){
            try {
                this.out.writeObject(text);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        //handle a message request to a user
        public void handleDirectMessage(Message message){
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                message.setRecipient(u);
                String text = message.getSender().getUserName() + ": " + message.getText();
                writeDirectMessage(text, message.getRecipient());
            } else{ //user offline-- store for later
                messageQueue.add(message);
            }
        }
    }
}

