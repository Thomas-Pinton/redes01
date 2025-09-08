import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    // Your program begins with a call to main()


    public static void main(String[] args) throws IOException {
        // Prints "Hello, World" to the terminal window.
        System.out.println("Hello, World");

        Server server = new Server();
        Client client = new Client();

        client.send();
    }
}
