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

    public synchronized void setUp(){
        try{
            aLock.lock();
            sock = new Socket(address, portNumber);
            Scanner scan = new Scanner(System.in);
            in = new ObjectInputStream(sock.getInputStream());
            out = new ObjectOutputStream(sock.getOutputStream());
            new ClientThread(Thread.currentThread()).start();
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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            aLock.unlock();
        }
    }

    public void runClient(){

        try {
            setUp();
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
                        System.out.println(object+"\n>");
                    }
                    else if(object instanceof Integer){
                        try {
                            aLock.lock();
                            status = (Integer) object;
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
