/**
 * Created by Avi on 2/23/15.
 */
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.*;

public class Client {
    //public ObjectInputStream in;
    public ObjectOutputStream out;
    public int portNumber;
    public String address;
    private Socket sock;
    private Socket privSock;
    private ObjectOutputStream privOut;
    //public ArrayList<User> users;
    public Integer status;
    private User user;
    public Lock aLock = new ReentrantLock();
    public Condition condVar = aLock.newCondition();
    private HashMap<String, UserSession> sessions;

    public Client(String address, int portNumber){
        this.portNumber = portNumber;
        this.address = address;
        this.sessions = new HashMap<String, UserSession>();
        //users = new ArrayList<User>();
        this.status = 1;
    }

    public void writeMessage(Message message){
        try {
            out.writeObject(message);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    //opens TCP connection
    private void connect(int portNumber){
        try {
            sock = new Socket(address, this.portNumber);
            //in = new ObjectInputStream(sock.getInputStream());
            out = new ObjectOutputStream(sock.getOutputStream());
            System.out.println(portNumber);
            out.writeObject(portNumber);
            out.writeObject(this.user);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void close(){
        try{
            //in.close();
            out.close();
            sock.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void setUp(){
        try{
            aLock.lock();
            Scanner scan = new Scanner(System.in);
            while(status != Server.LOGGED_IN){
                System.out.print("Username: ");
                String username = scan.next();
                out.writeObject(username);
                System.out.print("Password: ");
                String password = scan.next();
                out.writeObject(password);
                try {
                    condVar.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(status == 2){
                    System.out.println("Timed out for 60 seconds" + "\n");
                    try{
                        Thread.sleep(6000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Welcome to the Message Center!");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            aLock.unlock();
        }
    }

    public Boolean handlePrivateMessage(Message message) throws IOException{
        String[] info = message.getCommand().split(" ");
        UserSession session;
        if((session = sessions.get(info[1])) != null){
            privateMessage(message.getText(), session);
            return true;
        }
        return false;
    }

    public void privateMessage(Object object, UserSession session) throws IOException{
        connect(session.getiP(), session.getPortNumber());
        privOut.writeObject(object);
        closePrivSock();
    }

    private void connect(InetAddress add, int portNumber) throws IOException{
        privSock = new Socket(add, portNumber);
        privOut = new ObjectOutputStream(privSock.getOutputStream());
        privOut.flush();
    }

    private void closePrivSock(){
        try{
            privOut.close();
            privSock.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void runClient(){

        try {
            ClientThread listenThread = new ClientThread();
            listenThread.start();
            connect(listenThread.getPortNumber());
            setUp();
            close();
            //wait for commands from the user
            Scanner scan = new Scanner(System.in);
            while(status == Server.LOGGED_IN){
                System.out.print(status);
                System.out.print(">");
                String command = scan.nextLine();
                Message message = new Message(command, user);
                if(message.parseMessage()){
                    if(message.getType() == Message.PRIVATE){
                        if(!handlePrivateMessage(message))
                            System.out.println("Message Failure");
                        continue;
                    }
                    connect(listenThread.getPortNumber());
                    out.writeObject(message);
                    close();
                }
                else {
                    System.out.print(">Invalid command"+"\n>");
                }
            }

        }
        catch(UnknownHostException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }


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
                        System.out.println(object);
                    }
                    else if(object instanceof Integer){
                        try {
                            aLock.lock();
                            System.out.println("status changed");
                            status = (Integer) object;
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
}
