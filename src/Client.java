/**
 * Created by Avi on 2/23/15.
 */
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.*;

public class Client {
    //public ObjectInputStream in;
    public ObjectOutputStream out;
    public int portNumber;
    public String address;
    public Socket sock;
    //public ArrayList<User> users;
    public Integer status;
    private User user;
    public Lock aLock = new ReentrantLock();
    public Condition condVar = aLock.newCondition();

    public Client(String address, int portNumber){
        this.portNumber = portNumber;
        this.address = address;
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
    private void connect(){
        try {
            sock = new Socket(address, portNumber);
            //in = new ObjectInputStream(sock.getInputStream());
            out = new ObjectOutputStream(sock.getOutputStream());
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
            ClientThread listenThread = new ClientThread();
            listenThread.start();
            System.out.print("here: " + listenThread.getPortNumber());
            out.writeObject(listenThread.getPortNumber());
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
            close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            aLock.unlock();
        }
    }

    public void runClient(){

        try {
            connect();
            setUp();
            close();
            //wait for commands from the user
            Scanner scan = new Scanner(System.in);
            for(;;){
                System.out.print(">");
                String command = scan.nextLine();
                Message message = new Message(command, user);
                if(message.parseMessage()){
                    connect();
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
                        System.out.println(object+"\n>");
                    }
                    else if(object instanceof Integer){
                        try {
                            aLock.lock();
                            status = (Integer) object;
                            synchronized (condVar) {
                                condVar.signalAll();
                            }
                        }finally {
                            aLock.unlock();
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
}
