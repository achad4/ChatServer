/**
 * Avi Chad-Friedman
 * ajc2212
 * InitClient starts the Client
 */
public class InitClient {
    public static void main(String[] args){
        //start server on specified port number
        if(args.length != 2){
            System.out.println("Usage: \"java InitClient <IP> <portnumber>\"");
            System.exit(0);
        }
        Client client = new Client(args[0], Integer.parseInt(args[1]));
        client.runClient();
    }
}
