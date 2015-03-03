/**
 * Created by Avi on 2/24/15.
 */
public class InitServer {
    public static void main(String[] args){
        //start server on specified port number
        Server server = new Server(Integer.parseInt(args[0]));
        server.runServer();
    }
}
