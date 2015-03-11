/**
 * Avi Chad-Friedman
 * ajc2212
 */
public class InitServer {
    public static void main(String[] args){
        if(args.length != 1){
            System.out.println("Usage: \"java InitServer <portnumber>\"");
            System.exit(0);
        }
        Server server = new Server(Integer.parseInt(args[0]));
        server.runServer();
    }
}
