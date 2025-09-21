import javax.annotation.processing.SupportedSourceVersion;
import java.io.IOException;
import java.net.*;
import java.util.Objects;

public class Client {
    private InetAddress address;
    DatagramSocket socket;

    public Client() throws UnknownHostException, SocketException {
        address = InetAddress.getByName("localhost");
        socket = new DatagramSocket(1235); // listens on 1235
    }

    public void send(String text) throws IOException {
        byte[] bytes = text.getBytes();

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 1234);
        socket.send(packet);

        System.out.println("Hello, world!");
    }

    public void receive() throws IOException {
        System.out.println("Receiving");
        while (true)
        {
            byte[] text = new byte[1400];

            DatagramPacket packet = new DatagramPacket(text, text.length);

            socket.receive(packet);

            String received = "";
            received = new String(packet.getData(), 0, packet.getLength());

            if (received.length() > 0)
            {
                System.out.println("Received: " + received);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.send("GET foto1.png");
        client.receive();
    }
}
