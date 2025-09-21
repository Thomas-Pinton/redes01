import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

public class Server
{
    DatagramSocket socket;
    private InetAddress address;

    Server() throws SocketException, UnknownHostException {
        socket = new DatagramSocket(1234); // Listens on 1234
        address = InetAddress.getByName("localhost");
    }

    public void receive() throws IOException {
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

    private void get(String text)
    {
        System.out.println("Getting " + text);
        splitImage(text);
    }

    private void splitImage(String name)
    {
        File fi = new File("../media/foto1.png");
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(fi.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int length = fileContent.length;
        System.out.println("Length: " + length);
        int datagramSize = 1400;
        int i = 0, j = datagramSize;
        int steps = (int) Math.ceil((double) length / 1400);

        byte[] dataToSend = null;

        for (int k = 0; k < 1; k++)
        {
            dataToSend = Arrays.copyOfRange(fileContent, i, j);

            send(dataToSend);

            i = j;
            j += datagramSize;
            if (j > fileContent.length)
                j = fileContent.length;
        }
    }

    private void send(byte[] dataToSend)
    {
//        int i = 0;
//        File file = new File("../media/split/img" + i + ".jpg");
//        byte[] fileData = new byte[(int) file.length()];
//        try (FileInputStream fis = new FileInputStream(file)) {
//            fis.read(fileData);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length, address, 1235);

        try {
            socket.send(packet);
//            System.out.println("Packet sent: id ");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.receive();
    }
}
