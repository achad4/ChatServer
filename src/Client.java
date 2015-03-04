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
    public ObjectInputStream in;
    public ObjectOutputStream out;
    public int portNumber;
    public String address;
    public Socket sock;
    //public ArrayList<User> users;
    public Boolean loggedIn;
    private User user;
    public Lock aLock = new ReentrantLock();
    public Condition condVar = aLock.newCondition();

    public Client(String address, int portNumber){
        this.portNumber = portNumber;
        this.address = address;
        //users = new ArrayList<User>();
        this.loggedIn = false;
    }

    public void writeMessage(Message message){
        try {
            out.writeObject(message);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void setUp(){
        try{
            aLock.lock();
            sock = new Socket(address, portNumber);
            System.out.println(address);
            Scanner scan = new Scanner(System.in);
            in = new ObjectInputStream(sock.getInputStream());
            out = new ObjectOutputStream(sock.getOutputStream());
            new ClientThread(Thread.currentThread()).start();
            while(!loggedIn){
                System.out.print("Username: ");
                String username = scan.next();
                out.writeObject(username);
                System.out.print("Password: ");
                String password = scan.next();
                out.writeObject(password);
                try {
                    System.out.println("waiting");
                    condVar.await();
                    System.out.println("past");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            aLock.unlock();
        }
    }

    public void runClient(){

        try {
            setUp();
            //send user to the server
            //out.writeObject(user);
            //wait for commands from the user
            Scanner scan = new Scanner(System.in);
            for(;;){
                System.out.print(">");
                String command = scan.nextLine();
                Message message = new Message(command, user);
                if(message.parseMessage()){
                    out.writeObject(message);
                }
                else {
                    System.out.print(">Invalid command");
                }
            }
        }
        catch(UnknownHostException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }


    public void setLoggedInUser(User user){
        this.loggedIn = true;
        this.user = user;
    }


    class ClientThread extends Thread{
        private Thread parent;
        public ClientThread(Thread parent){
            this.parent = parent;
        }
        public void run(){
            for(;;){
                try{
                    Object object = in.readObject();
                    if(object instanceof String){
                        System.out.println(object+"\n");
                    }
                    else if(object instanceof Boolean){
                        try {
                            aLock.lock();
                            loggedIn = (Boolean) object;
                            synchronized (condVar) {
                                System.out.println("notify");
                                condVar.signalAll();
                            }
                        }finally {
                            aLock.unlock();
                        }
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                    break;
                } catch(ClassNotFoundException e) {
                   e.printStackTrace();
                }
            }
        }
    }
}
