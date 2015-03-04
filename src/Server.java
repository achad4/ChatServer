/**
 * Created by Avi on 2/22/15.
 */
import java.io.*;
import java.lang.Exception;
import java.net.*;
import java.util.ArrayList;

public class Server {
    public int portNumber;
    private ArrayList<UserThread> clients;
    private ArrayList<User> users;

    public Server(int portNumber){
        this.portNumber = portNumber;
        this.clients = new ArrayList<UserThread>();
    }

    public void runServer(){
        try {
            ServerSocket serverSock = new ServerSocket(this.portNumber);
            for(;;){
                Socket clientSock = serverSock.accept();
                System.out.println("new thread");
                //create a new thread to handle the user
                UserThread thread = new UserThread(clientSock, this);
                this.clients.add(thread);
                thread.start();
                System.out.println("user handled");
            }
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
        System.out.println("handling DM");
        for(UserThread t : this.clients){
            System.out.println(recipient.getUserName());
            if(t.user.getUserName().equals(recipient.getUserName())){
                System.out.print("user:" + t.user.getUserName());
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
                for(UserThread t : this.clients){
                    if(t.user.getUserName().equals(u.getUserName())){
                        //user is already logged in
                        t.writeMessage("Someone attempted to log in with your credentials");
                        t.logout();
                        removeThread(t.getId());
                        return null;
                    }
                }
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
            int attempts = 0;
            while(this.user == null && attempts < 4) {
                String username = (String) in.readObject();
                String password = (String) in.readObject();
                User u;
                if((u = loginUser(username, password)) != null){
                    return u;
                }
                out.writeObject("Invalid credentials.  Try again.");
                attempts++;
            }
            out.writeObject("Max attempts reached.  Timed out.");
            return null;
        }

        public void run() {
            Boolean handleClient = true;
            while(handleClient){
                try {
                    if(this.user == null) {
                        this.user = handleLogin();
                    }
                    else {
                        //obtain the message object from the input stream
                        Message message = (Message) in.readObject();
                        switch (message.getType()) {
                            case message.DIRECT_MESSAGE:
                                this.handleDirectMessage(message);
                                break;
                            /*
                            case message.BROADCAST:
                                this.handleBroadcast(message);
                                break;
                             */
                            case message.LOGOUT:
                                this.logout();
                                break;
                        }
                    }
                    //make the user wait before attempting a fourth login
                    sleep(6000);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
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
                System.out.println("sending DM");
                this.out.writeObject(text);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        //handle a message request to a user
        public void handleDirectMessage(Message message){
            String[] info = message.text.split(" ");
            message.setRecipient(findUser(info[1]));
            writeDirectMessage(message.text, message.getRecipient());
        }


    }
}

