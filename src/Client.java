import java.io.IOException;
import java.net.*;

public class Client {
    private InetAddress address;
    DatagramSocket socket;
    public Client() throws UnknownHostException, SocketException {
        address = InetAddress.getByName("localhost");
        socket = new DatagramSocket();
    }
    public void send(String text) throws IOException {
        byte[] bytes = text.getBytes();

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 1234);
        socket.send(packet);

        System.out.println("Hello, world!");
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.send("GET foto1.png");
    }
}
