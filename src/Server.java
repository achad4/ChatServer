/**
 * Avi Chad-Friedman
 * ajc2212
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
    private HashMap<String, LinkedList<String>> privateLists;
    public static final int LOGGED_IN = 0, LOGGED_OUT = 1, TIMED_OUT = 2, ATTEMPTING = 3, USERNAME_CORRECT = 4;
    private static final int TIMEOUT = 45;

    public Server(int portNumber){
        this.portNumber = portNumber;
        this.users = new ArrayList<User>();
        this.messageQueues = new HashMap<String, LinkedList<Message>>();
        this.sessions = new HashMap<String, UserSession>();
        this.blackLists = new HashMap<String, LinkedList<String>>();
        this.privateLists = new HashMap<String, LinkedList<String>>();
        populateUsers();
    }

    public void runServer(){
        try {
            new ClientMonitor().start();
            ServerSocket serverSock = new ServerSocket(this.portNumber);
            for(;;){
                Socket clientSock = serverSock.accept();
                UserThread thread = new UserThread(clientSock);
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
                String[] userInfo = line.split(" ");
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

    private Boolean isFriend(User a, User b){
        LinkedList<String> friends;
        if((friends = privateLists.get(b.getUserName())) != null) {
            if (friends.contains(a.getUserName()))
                return true;
        }
        return false;
    }

    //thread to handle general user requests
    class UserThread extends Thread {
        private Socket socket;
        private Socket clntSock;
        private ObjectInputStream in;
        private ObjectOutputStream toClnt;
        User user;
        //info the client passes to log in a user
        private Integer portNumber;

        public UserThread(Socket socket){
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
                User tempUser = new User("","");
                String username = "";
                Boolean validUserName = false;
                while(!validUserName){
                    username = (String) in.readObject();
                    if((tempUser = findUser(username)) != null){
                        validUserName = true;
                        writeToClient(USERNAME_CORRECT, session);
                    }
                }
                int attempts = 1;
                while (this.user == null) {
                    String password = (String) in.readObject();
                    User u;
                    if ((u = loginUser(username, password)) != null) {
                        return u;
                    }
                    if(attempts >= 2){
                        writeToClient(TIMED_OUT, session);
                        tempUser.block();
                        attempts = 0;
                    }
                    else {
                        writeToClient(ATTEMPTING, session);
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
            writeToClient("Missed Messages:", sessions.get(this.user.getUserName()));
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
                    UserSession session = new UserSession(socket.getInetAddress(), this.portNumber);
                    //check if the user is logged on with another IP address
                    if((user = handleLogin(session)) != null) {
                        if(user.isBlocked()){
                            writeToClient("Due to multiple login failures your account has been temporarily blocked.",
                                          session);
                            writeToClient(LOGGED_OUT, session);
                            return;
                        }
                        if((sessions.get(user.getUserName())) != null){
                            writeToClient("Another user is logging in with your credentials", session);
                            writeToClient(LOGGED_OUT, session);
                            sessions.remove(user.getUserName());
                            session.setUser(user);
                            sessions.put(this.user.getUserName(), session);
                        }
                        else {
                            session.setUser(user);
                            sessions.put(user.getUserName(), session);
                            writeToClient(user, session);
                            handleMissedMessages();
                        }
                        String notification = user.getUserName() + " has logged on";
                        Message broadCast = new Message(notification, Message.BROADCAST);
                        handleBroadcast(broadCast);
                    }
                }else{
                    //check if the user has been timed out erroneously
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
                    case Message.ONLINE:
                        handleOnline();
                        break;
                    case Message.FRIEND:
                        handleFriend(message);
                        break;
                    case Message.UNFRIEND:
                        handleUnfriend(message);
                        break;
                }
            } catch (EOFException e){
                closeIn();
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        private void handleLogout() throws IOException{
            //writeToClient(LOGGED_OUT, sessions.get(this.user.getUserName()));
            sessions.remove(this.user.getUserName());
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
                        //if connection was lost unexpectedly, remove the session
                        try {
                            writeToClient(text, session);
                        } catch (ConnectException e) {
                            System.out.println("Lost connection to recipient");
                            sessions.remove(u.getUserName());
                            handleDirectMessage(message);
                        }
                    } else { //user is offline, store for later
                        writeToClient("User is offline: sent as offline message", sessions.get(this.user.getUserName()));
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
            String text;
            if(message.getSender() != null) {
                text = message.getSender().getUserName() + ": " + message.getText();
            }
            else {
                text = message.getText();
            }
            for(UserSession session : sessions.values()){
                if(canContact(this.user, session.getUser()) &&
                        !session.getUser().getUserName().equals(this.user.getUserName())) {
                    writeToClient(text, session);
                }
            }
        }

        private void handlePrivateChat(Message message) throws IOException{
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                if(canContact(this.user, u)) {
                    if(isFriend(this.user, u)) {
                        UserSession session;
                        if ((session = sessions.get(u.getUserName())) != null) {
                            AbstractMap.SimpleEntry<String, UserSession> pair;
                            pair = new AbstractMap.SimpleEntry<String, UserSession>(u.getUserName(), session);
                            writeToClient(pair, sessions.get(this.user.getUserName()));
                        } else {
                            writeToClient("Not online", sessions.get(this.user.getUserName()));
                        }
                    }else{
                        writeToClient("Waiting for user's permission.  You'll be notified when you can contact " +
                                u.getUserName(), sessions.get(this.user.getUserName()));
                        writeToClient(this.user.getUserName() + " is requesting your IP address.  To allow P2P chat " +
                                "use the command \"friend <username>\"", sessions.get(u.getUserName()));
                    }
                }else{
                    writeToClient("You cannot contact this user", sessions.get(this.user.getUserName()));
                }
            }
        }

        private void handleFriend(Message message) throws IOException{
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null) {
                UserSession session;
                if ((session = sessions.get(this.user.getUserName())) != null) {
                    AbstractMap.SimpleEntry<String, UserSession> pair;
                    pair = new AbstractMap.SimpleEntry<String, UserSession>(this.user.getUserName(), session);
                    writeToClient(pair, sessions.get(u.getUserName()));
                    writeToClient("You can now chat privately with "+this.user.getUserName(), sessions.get(u.getUserName()));
                } else {
                    writeToClient("Not online", sessions.get(this.user.getUserName()));
                }
                LinkedList<String> friends;
                if((friends = privateLists.get(this.user)) != null) {
                    friends.add(u.getUserName());
                }else{
                    friends = new LinkedList<String>();
                    friends.add(u.getUserName());
                    privateLists.put(this.user.getUserName(), friends);
                }
            }
        }

        private void handleUnfriend(Message message) throws IOException{
            message.setSender(this.user);
            String[] info = message.getCommand().split(" ");
            User u;
            if((u = findUser(info[1])) != null){
                LinkedList<String> friends;
                if((friends = privateLists.get(this.user.getUserName())) != null) {
                    friends.remove(u.getUserName());
                    writeToClient(message, sessions.get(u.getUserName()));
                    writeToClient("You can no longer privately chat with"+this.user.getUserName(),
                            sessions.get(u.getUserName()));
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

        private void handleOnline() throws IOException{
            UserSession currentSession = sessions.get(this.user.getUserName());
            for(UserSession session : sessions.values()){
                if(!this.user.getUserName().equals(session.getUser().getUserName()))
                    writeToClient(session.getUser().getUserName(), currentSession);
            }
        }
    }


    //Thread to handle heart beat
    class ClientMonitor extends Thread{

        //object to timeout expired clients
        class HeartBeatChecker extends TimerTask{
            public void run(){
                for(UserSession session : sessions.values()) {
                    long diff = getDiff(session.getLastHeartBeat(), new Date(), TimeUnit.SECONDS);
                    if (diff > TIMEOUT) {
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