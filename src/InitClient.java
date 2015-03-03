/**
 * Created by Avi on 2/24/15.
 */
public class InitClient {
    public static void main(String[] args){
        //start server on specified port number
        Client client = new Client(args[0], Integer.parseInt(args[1]));
        client.runClient();
    }
}
