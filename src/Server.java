/**
 * Created by Avi on 2/22/15.
 */
import java.io.*;
import java.lang.Exception;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Server {
    public int portNumber;
    private ArrayList<UserThread> clients;
    private ArrayList<User> users;
    private ArrayList<User> loggedInUsers;
    private HashMap<User, LinkedList<Message>> messageQueues;
    public static final int LOGGED_IN = 0, LOGGED_OUT = 1, TIMED_OUT = 2;

    public Server(int portNumber){
        this.portNumber = portNumber;
        this.clients = new ArrayList<UserThread>();
        this.users = new ArrayList<User>();
        this.messageQueues = new HashMap<User, LinkedList<Message>>();
        this.loggedInUsers = new ArrayList<User>();
        populateUsers();
    }

    public void runServer(){
        try {
            ServerSocket serverSock = new ServerSocket(this.portNumber);
            for(;;){
                Socket clientSock = serverSock.accept();
                System.out.print("accepted");
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

    public synchronized User loginUser(String username, String password){
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

    private synchronized User findUser(String username){
        for(User u : this.users){
            if(u.getUserName().equals(username))
                return u;
        }
        return null;
    }

    private synchronized void logoutUser(User user){
        for(User u : this.loggedInUsers){
            if(u == user)
                this.loggedInUsers.remove(u);
        }
    }

    private synchronized Boolean isLoggedIn(User user){
        for(User u : this.loggedInUsers){
            if(u == user)
                return true;
        }
        return false;
    }

    class UserThread extends Thread {
        Socket socket;
        Socket clntSock;
        ObjectInputStream in;
        //ObjectOutputStream out;
        ObjectOutputStream toClnt;
        Server server;
        User user;
        //info the client passes to log in a user
        private String userName;
        private String password;
        private Integer portNumber;

        public UserThread(Socket socket, Server server){
            this.socket = socket;
            //this.server = server;
            try {
                //this.out = new ObjectOutputStream(socket.getOutputStream());
                //flush output to unblock client side input stream
                //this.out.flush();
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

        private void connect(){
            try {
                System.out.println("Connecting to the client");
                clntSock = new Socket(Inet4Address.getLocalHost().getHostAddress(), portNumber);
                toClnt = new ObjectOutputStream(clntSock.getOutputStream());
                toClnt.flush();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

        private void closeOut(){
            try{
                toClnt.close();
                clntSock.close();
            }catch(IOException e){
                e.printStackTrace();
            }
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
                        connect();
                        toClnt.writeObject(TIMED_OUT);
                        closeOut();
                        attempts = 0;
                    }
                    else {
                        connect();
                        toClnt.writeObject(LOGGED_OUT);
                        closeOut();
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
                System.out.println("running server thread");
                this.portNumber = (Integer) in.readObject();
                System.out.println(this.portNumber);
                //first determine whether this is client is logged in
                if((this.user = (User) in.readObject()) == null) {
                    if((this.user = handleLogin()) != null) {
                        connect();
                        System.out.println("logging in client");
                        toClnt.writeObject(this.user);
                        loggedInUsers.add(this.user);
                        closeOut();
                        return;
                    }
                    else{
                        toClnt.writeObject(false);
                    }
                }
            } catch (EOFException e) {
                System.out.println("socket closed");
            } catch (IOException e){
                e.printStackTrace();
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            Boolean handleClient = true;
            while(handleClient) {
                try {
                    Message message;
                    //obtain the message from the users pending message queue
                    if ((messageQueues.get(this.user) != null) && !messageQueues.get(this.user).isEmpty()) {
                        message = messageQueues.get(this.user).remove();
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
                            logoutUser(this.user);
                            this.closeIn();
                            break;
                    }
                } catch (EOFException e){
                    System.out.println("EOF");
                    break;
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }
                //make the user wait before attempting a fourth login

            }
        }



        //close sockets and remove the thread from the client list
        private void closeIn(){
            removeThread(this.getId());
            try {
                in.close();
                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //write a message to the user
        public Boolean writeMessage(String text){
            try {
                connect();
                toClnt.writeObject(text);
                closeOut();
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
                if(isLoggedIn(u)) {
                    writeDirectMessage(text, message.getRecipient());
                }else{ //user is offline, store for later
                    LinkedList<Message> queue = messageQueues.get(u);
                    queue.add(message);
                }
            }
        }
    }
}

