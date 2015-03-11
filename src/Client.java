/**
 * Avi Chad-Friedman
 * ajc2212
 */
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.*;
import java.util.concurrent.locks.*;

public class Client {

    private ObjectOutputStream out;
    private int portNumber;
    private int listenPortNumber;
    private String address;
    private Socket sock;
    private Socket privSock;
    private ObjectOutputStream privOut;
    private Integer status;
    private User user;
    private Lock aLock = new ReentrantLock();
    private Condition condVar = aLock.newCondition();
    private HashMap<String, UserSession> sessions;
    private static final int HEART_RATE = 30;

    public Client(String address, int portNumber){
        this.portNumber = portNumber;
        this.address = address;
        this.sessions = new HashMap<String, UserSession>();
        //users = new ArrayList<User>();
        this.status = Server.ATTEMPTING;
    }

    //connect to the server
    private void connect(){
        try {
            sock = new Socket(address, this.portNumber);
            out = new ObjectOutputStream(sock.getOutputStream());
            out.writeObject(listenPortNumber);
            out.writeObject(this.user);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //close the connection to the server
    private void close(){
        try{
            out.close();
            sock.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    //Connect to another client without the server's help
    private void connect(InetAddress add, int portNumber) throws IOException{
        privSock = new Socket(add, portNumber);
        privOut = new ObjectOutputStream(privSock.getOutputStream());
        privOut.flush();
    }

    //close the connection to another client
    private void closePrivSock(){
        try{
            privOut.close();
            privSock.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void login(){
            try {
                aLock.lock();
                Scanner scan = new Scanner(System.in);
                while (status == Server.ATTEMPTING) {
                    System.out.print("Username: ");
                    String username = scan.next();
                    out.writeObject(username);

                    try {
                        condVar.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (status != Server.LOGGED_IN) {
                    System.out.print("Password: ");
                    String password = scan.next();
                    out.writeObject(password);
                    if (status == Server.TIMED_OUT) {
                        System.out.println("Timed out for 60 seconds" + "\n");
                    }
                    try {
                        condVar.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                aLock.unlock();
            }
            new HeartBeat().start();
            System.out.println(">Welcome to the Message Center!");
    }

    public Boolean handlePrivateMessage(Message message) throws IOException{
        String[] info = message.getCommand().split(" ");
        UserSession session;
        if((session = sessions.get(info[1])) != null){
            privateMessage(this.user.getUserName()+": "+message.getText(), session);
            return true;
        }
        return false;
    }

    public void privateMessage(Object object, UserSession session) throws IOException{
        try {
            connect(session.getiP(), session.getPortNumber());
            privOut.writeObject(object);
            closePrivSock();
        }catch (ConnectException e){
            System.out.println(">User's address may have changed.  Please request again.");
        }
    }

    public void runClient(){

        try {
            ClientThread listenThread = new ClientThread();
            listenThread.start();
            listenPortNumber = listenThread.getPortNumber();
            connect();
            login();
            close();
            //wait for commands from the user
            //Scanner scan = new Scanner(System.in);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(System.in));
            while(status == Server.LOGGED_IN){
                System.out.print(">");
                //String command = scan.nextLine();
                String command = "";
                try {
                    // wait until we have data to complete a readLine()
                    while (!br.ready()) {
                        Thread.sleep(200);
                    }
                    command = br.readLine();
                } catch (InterruptedException e) {
                    System.out.println("ConsoleInputReadTask() cancelled");
                }
                Message message = new Message(command, user);
                if(message.parseMessage()){
                    if(message.getType() == Message.PRIVATE){
                        if(!handlePrivateMessage(message))
                            System.out.println(">You must request the user's address.");
                        continue;
                    }
                    connect();
                    out.writeObject(message);
                    close();
                    //restart the client on log off
                    if(message.getType() == Message.LOGOUT)
                        System.exit(0);

                }
                else {
                    System.out.print(">Invalid command"+"\n");
                }
            }
        }
        catch(UnknownHostException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }


    //Thread that listens for information from the server
    class ClientThread extends Thread{
        private ServerSocket servSock;
        private ObjectInputStream in;
        public ClientThread(){
            try {
                servSock = new ServerSocket(0);
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        public void run(){
            try{
                for(;;){
                    Socket clntSock = servSock.accept();
                    in = new ObjectInputStream(clntSock.getInputStream());
                    Object object = in.readObject();
                    if(object instanceof String){
                        System.out.print(object + "\n>");
                    }
                    else if(object instanceof Integer){
                        try {
                            aLock.lock();
                            status = (Integer) object;

                            if (status == Server.LOGGED_OUT)
                                System.exit(0);

                            synchronized (condVar) {
                                condVar.signalAll();
                            }
                        }finally {
                            aLock.unlock();
                        }
                    }else if(object instanceof User){
                        try {
                            aLock.lock();
                            status = Server.LOGGED_IN;
                            user = (User) object;
                            synchronized (condVar) {
                                condVar.signalAll();
                            }
                        }finally {
                            aLock.unlock();
                        }
                    }else if(object instanceof AbstractMap.SimpleEntry){
                        AbstractMap.SimpleEntry<String, UserSession> pair;
                        pair = (AbstractMap.SimpleEntry<String, UserSession>) object;
                        sessions.put(pair.getKey(), pair.getValue());
                    }else if(object instanceof Message){
                        Message message = (Message) object;
                        if(message.getType() == Message.UNFRIEND){
                            if((sessions.get(message.getSender().getUserName())) != null){
                                sessions.remove(message.getSender().getUserName());
                            }
                        }
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public int getPortNumber(){
            return servSock.getLocalPort();
        }
    }

    //Thread to let the server know the client is alive
    class HeartBeat extends Thread{

        class Pump extends TimerTask {
            public void run(){
                try {
                    Message heartBeat = new Message();
                    connect();
                    out.writeObject(heartBeat);
                    close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void run(){
            //send heart beat every HEART_RATE seconds
            Timer timer = new Timer();
            timer.schedule(new Pump(), 0, 1000*HEART_RATE);
        }
    }
}
