/**
 * Created by Avi on 2/22/15.
 */
import java.io.*;
import java.lang.Exception;
import java.net.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;

public class Server {
    public int portNumber;
    //private ArrayList<UserThread> clients;
    private ArrayList<User> users;
    //private ArrayList<User> loggedInUsers;

    private HashMap<User, UserSession> sessions;
    private HashMap<String, LinkedList<Message>> messageQueues;
    public static final int LOGGED_IN = 0, LOGGED_OUT = 1, TIMED_OUT = 2;

    public Server(int portNumber){
        this.portNumber = portNumber;
        //this.clients = new ArrayList<UserThread>();
        this.users = new ArrayList<User>();
        this.messageQueues = new HashMap<String, LinkedList<Message>>();
        this.sessions = new HashMap<User, UserSession>();
        //this.loggedInUsers = new ArrayList<User>();
        populateUsers();
    }

    public void runServer(){
        try {
            ServerSocket serverSock = new ServerSocket(this.portNumber);
            for(;;){
                Socket clientSock = serverSock.accept();
                UserThread thread = new UserThread(clientSock, this);
                //this.clients.add(thread);
                thread.start();
            }
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

    public synchronized User loginUser(String username, String password){
        for(User u : users){
            if(u.verifyPassword(password) && u.verifyUsername(username)) {
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
            try {
                this.in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void connect(InetAddress add, int portNumber){
            try {
                System.out.println("Connecting to the client");
                clntSock = new Socket(add, portNumber);
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

        private User handleLogin(UserSession session){
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
                        writeToClient(TIMED_OUT, session);
                        attempts = 0;
                    }
                    else {
                        writeToClient(LOGGED_OUT, session);
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

        private void handleMissedMessages(){
            if((messageQueues.get(this.user.getUserName()) == null)){
                return;
            }
            writeToClient("Missed Messages:", sessions.get(this.user));
            while (!messageQueues.get(this.user.getUserName()).isEmpty()) {
                System.out.println("messagequeue");
                Message message = messageQueues.get(this.user.getUserName()).remove();
                this.handleDirectMessage(message);
            }
        }
        public void run() {
            try{
                System.out.println("running server thread");
                this.portNumber = (Integer) in.readObject();
                System.out.println(this.portNumber);
                //first determine whether this is client is logged in
                if((this.user = (User) in.readObject()) == null) {
                    UserSession session = new UserSession(socket.getInetAddress(), this.portNumber);
                    //check if the user is logged on with another IP address
                    if(sessions.get(user) != null){
                        
                    }
                    if((user = handleLogin(session)) != null) {
                        System.out.println("logging in client");
                        sessions.put(user, session);
                        writeToClient(user, sessions.get(this.user));
                        handleMissedMessages();
                        //return;
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
                    //obtain the message object from the input stream
                    message = (Message) in.readObject();
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
                            handleLogout();
                            handleClient = false;
                            break;
                    }
                } catch (EOFException e){
                    System.out.println("EOF");
                    closeIn();
                    handleClient = false;
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        private void handleLogout(){
            //logoutUser(this.user);
            writeToClient(LOGGED_OUT, sessions.get(this.user));
            sessions.remove(this.user);
            closeIn();
        }


        //close sockets and remove the thread from the client list
        private void closeIn(){
            try {
                in.close();
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }catch (ConcurrentModificationException e){
                e.printStackTrace();
            }
        }

        //write a message to the user
        public Boolean writeToClient(Object object, UserSession session){
            try {
                connect(session.getiP(), session.getPortNumber());
                toClnt.writeObject(object);
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
                UserSession session;
                if((session = sessions.get(u)) != null) {
                    writeToClient(text, session);
                }else{ //user is offline, store for later
                    LinkedList<Message> queue;
                    if((queue = messageQueues.get(u.getUserName())) != null) {
                        queue.add(message);
                    }else{
                        System.out.println("making new quee");
                        queue = new LinkedList<Message>();
                        queue.add(message);
                        messageQueues.put(u.getUserName(), queue);
                    }
                }
            }
        }
    }
}

    /*
    //search for the thread with the right user and send the text
    public synchronized Boolean writeDirectMessage(String text, User recipient){
        for (UserThread t : this.clients) {
            if (t.user.getUserName().equals(recipient.getUserName())) {
                return t.writeToClient(text);
            }
        }
        return true;
    }

    public void removeThread(long threadId){
        for (UserThread t : this.clients) {
            if (t.getId() == threadId)
                this.clients.remove(t);
        }
    }
    */

    /*
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
    */