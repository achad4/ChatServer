/**
 * Created by Avi on 2/22/15.
 */
import java.io.*;
import java.lang.Exception;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Server {
    public int portNumber;
    private ArrayList<User> users;

    private HashMap<String, UserSession> sessions;
    private HashMap<String, LinkedList<Message>> messageQueues;
    private HashMap<String, LinkedList<String>> blackLists;
    public static final int LOGGED_IN = 0, LOGGED_OUT = 1, TIMED_OUT = 2;

    public Server(int portNumber){
        this.portNumber = portNumber;
        this.users = new ArrayList<User>();
        this.messageQueues = new HashMap<String, LinkedList<Message>>();
        this.sessions = new HashMap<String, UserSession>();
        this.blackLists = new HashMap<String, LinkedList<String>>();
        populateUsers();
    }

    public void runServer(){
        try {
            new ClientMonitor().start();
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

    private Boolean canContact(User a, User b){
        LinkedList<String> blockedList;
        if((blockedList = blackLists.get(b.getUserName())) != null) {
            if (blockedList.contains(a.getUserName()))
                return false;
        }
        return true;
    }


    //thread to handle general user requests
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

        private void connect(InetAddress add, int portNumber) throws IOException{
            clntSock = new Socket(add, portNumber);
            toClnt = new ObjectOutputStream(clntSock.getOutputStream());
            toClnt.flush();
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

        private void handleMissedMessages() throws IOException{
            if((messageQueues.get(this.user.getUserName()) == null)){
                return;
            }
            writeToClient("Missed Messages:", sessions.get(this.user));
            while (!messageQueues.get(this.user.getUserName()).isEmpty()) {
                Message message = messageQueues.get(this.user.getUserName()).remove();
                this.handleDirectMessage(message);
            }
        }
        public void run() {
            try{
                this.portNumber = (Integer) in.readObject();
                //first determine whether this is client is logged in
                if((this.user = (User) in.readObject()) == null) {
                    System.out.println("login");
                    UserSession session = new UserSession(socket.getInetAddress(), this.portNumber);
                    //check if the user is logged on with another IP address
                    if((user = handleLogin(session)) != null) {
                        if((sessions.get(user.getUserName())) != null){
                            writeToClient("Another user is logging in with your credentials", session);
                            writeToClient(LOGGED_OUT, session);
                            sessions.remove(user.getUserName());
                        }
                        else {
                            session.setUser(user);
                            sessions.put(user.getUserName(), session);
                            writeToClient(user, session);
                            handleMissedMessages();
                        }
                    }
                }else{ //check if the user has been timed out erroneously
                    if((sessions.get(user.getUserName())) == null){
                        UserSession session = new UserSession(socket.getInetAddress(), this.portNumber);
                        session.setUser(user);
                        sessions.put(user.getUserName(), session);
                    }
                }
            } catch (EOFException e) {
                System.out.println("socket closed");
            } catch (IOException e){
                e.printStackTrace();
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
            //Boolean handleClient = true;
            //while(handleClient) {
                try {
                    Message message;
                    message = (Message) in.readObject();
                    message.setSender(this.user);
                    switch (message.getType()) {
                        case Message.DIRECT_MESSAGE:
                            this.handleDirectMessage(message);
                            break;
                        case Message.BROADCAST:
                            handleBroadcast(message);
                            break;
                        case Message.LOGOUT:
                            handleLogout();
                            //handleClient = false;
                            break;
                        case Message.GET_ADDRESS:
                            handlePrivateChat(message);
                            //handleClient = false;
                            break;
                        case Message.BLOCK:
                            handleBlock(message);
                            //handleClient = false;
                            break;
                        case Message.UNBLOCK:
                            handleUnblock(message);
                            //handleClient = false;
                            break;
                        case Message.HEART_BEAT:
                            handleHeartBeat();
                            break;
                    }
                } catch (EOFException e){
                    System.out.println("EOF");
                    closeIn();
                    //handleClient = false;
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }
            //}
        }

        private void handleLogout() throws IOException{
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
        public void writeToClient(Object object, UserSession session) throws IOException{
            connect(session.getiP(), session.getPortNumber());
            toClnt.writeObject(object);
            closeOut();
        }

        //handle a message request to a user
        private void handleDirectMessage(Message message) throws IOException{
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                if(canContact(this.user, u)) {
                    message.setRecipient(u);
                    String text = message.getSender().getUserName() + ": " + message.getText();
                    UserSession session;
                    if ((session = sessions.get(u.getUserName())) != null) {
                        System.out.println("ip: " + sessions.get(u.getUserName()).getiP());
                        //if connection was lost unexpectedly, remove the session
                        try {
                            writeToClient(text, session);
                        } catch (ConnectException e) {
                            System.out.println("Lost connection to recipient");
                            sessions.remove(u.getUserName());
                            handleDirectMessage(message);
                        }
                    } else { //user is offline, store for later
                        LinkedList<Message> queue;
                        if ((queue = messageQueues.get(u.getUserName())) != null) {
                            queue.add(message);
                        } else {
                            queue = new LinkedList<Message>();
                            queue.add(message);
                            messageQueues.put(u.getUserName(), queue);
                        }
                    }
                }else{
                    writeToClient("You cannot contact this user", sessions.get(this.user.getUserName()));
                }
            }
        }

        //sends message to all onling users
        private void handleBroadcast(Message message) throws IOException{
            String text = message.getSender().getUserName() + ": " + message.getText();
            for(UserSession session : sessions.values()){
                if(canContact(this.user, session.getUser()))
                    writeToClient(text, session);
            }
        }

        private void handlePrivateChat(Message message) throws IOException{
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                if(canContact(this.user, u)) {
                    UserSession session;
                    if ((session = sessions.get(u.getUserName())) != null) {
                        AbstractMap.SimpleEntry<String, UserSession> pair;
                        pair = new AbstractMap.SimpleEntry<String, UserSession>(u.getUserName(), session);
                        System.out.println(sessions.get(this.user.getUserName()).getiP());
                        writeToClient(pair, sessions.get(this.user.getUserName()));
                    } else {
                        writeToClient("Not online", sessions.get(this.user.getUserName()));
                    }
                }else{
                    System.out.println("can't contact");
                    writeToClient("You cannot contact this user", sessions.get(this.user.getUserName()));
                }
            }
        }

        private void handleBlock(Message message){
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                LinkedList<String> blockedList;
                if((blockedList = blackLists.get(this.user)) != null) {
                    blockedList.add(u.getUserName());
                }else{
                    blockedList = new LinkedList<String>();
                    blockedList.add(u.getUserName());
                    blackLists.put(this.user.getUserName(), blockedList);
                }
            }
        }

        private void handleUnblock(Message message){
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                LinkedList<String> blockedList;
                if((blockedList = blackLists.get(this.user.getUserName())) != null) {
                    blockedList.remove(u.getUserName());
                }
            }
        }

        private void handleHeartBeat(){
            UserSession session = sessions.get(this.user.getUserName());
            session.setLastHeartBeat();
        }
    }


    //Thread to handle heart beat
    class ClientMonitor extends Thread{
        //object to timeout expired clients
        Socket clntSock;
        ObjectOutputStream toClnt;

        class HeartBeatChecker extends TimerTask{
            public void run(){
                for(UserSession session : sessions.values()) {
                    long diff = getDiff(session.getLastHeartBeat(), new Date(), TimeUnit.SECONDS);
                    if (diff > 30) {
                        User user = session.getUser();
                        System.out.println("Timed out "+user.getUserName());
                        sessions.remove(user.getUserName());
                    }
                }
            }

            public long getDiff(Date date1, Date date2, TimeUnit timeUnit){
                long diff = date2.getTime() - date1.getTime();
                return timeUnit.convert(diff,TimeUnit.MILLISECONDS);
            }
        }

        public void run(){
            //run heart beat monitor every 5 seconds
            Timer timer = new Timer();
            timer.schedule(new HeartBeatChecker(), 0, 5000);
        }
    }

}