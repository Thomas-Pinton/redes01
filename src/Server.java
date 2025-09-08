import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Objects;

public class Server
{
    DatagramSocket socket;

    Server() throws SocketException {
        socket = new DatagramSocket(1234);
    }

    public void receive() throws IOException {
        while (true)
        {
            byte[] text = new byte[256];

            DatagramPacket packet = new DatagramPacket(text, text.length);

            socket.receive(packet);

            String received = "";
            received = new String(packet.getData(), 0, packet.getLength());

            if (received.length() > 0)
            {
                System.out.println("Received: " + received);
                String[] parts = received.split(" ");
                if (Objects.equals(parts[0], "GET"))
                    get(parts[1]);
            }
        }
    }

    private void get(String text)
    {
        System.out.println("Getting " + text);
    }


    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.receive();
    }
}
