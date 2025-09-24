import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Server
{
    DatagramSocket socket;
    private InetAddress address;
    ArrayList<byte[]> packages = new ArrayList<byte[]>();

    Server() throws SocketException, UnknownHostException {
        socket = new DatagramSocket(1234); // Listens on 1234
        address = InetAddress.getByName("localhost");
    }

    public void receive() throws IOException {
        while (true)
        {
            byte[] text = new byte[256];

            DatagramPacket packet = new DatagramPacket(text, text.length);

            socket.receive(packet);

            String received = "";
            received = new String(packet.getData(), 0, packet.getLength());

            if (received.length() > 0) {
                System.out.println("Received: " + received);
                String[] parts = received.split(" ", 2);
                if (Objects.equals(parts[0], "GET"))
                    get(parts[1]);
                else if (Objects.equals(parts[0], "NAC")) {
                    nack(parts[1]);
                }
            }
        }
    }

    private void get(String text)
    {
        System.out.println("Getting " + text);
        splitImage(text);
    }

    private void nack(String text)
    {
        String[] missingPackets = text.split(" ", -1);
        for (String missingPacket : missingPackets) {
            if (!Objects.equals(missingPacket, "")) {
                send(packages.get(Integer.parseInt(missingPacket)));
                System.out.println("Packets: " +Integer.parseInt(missingPacket));
            } else {
                sendEndMessage();
            }
        }
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
        int datagramSize = 1395;
        int i = 0, j = datagramSize;
        int steps = (int) Math.ceil((double) length / datagramSize);

        byte[] id = new byte[2];

        for (int k = 0; k < steps; k++)
        {
            byte[] dataToSend = new byte[1400];
            byte[] data = Arrays.copyOfRange(fileContent, i, j);

            for (int l = 5; l < data.length + 5; l++)
                dataToSend[l] = data[l-5];

            // Header pt0: type of message
            dataToSend[0] = (byte) 0;
                // 0 = sending data
                // 1 = message end

            // Header pt1: packet ID
            dataToSend[1] = id[0];
            dataToSend[2] = id[1];

            // Header pt2: totalPackets
            dataToSend[3] = (byte) ( steps / 127 );
            dataToSend[4] = (byte) ( steps % 127 );

            packages.add(dataToSend);
            send(dataToSend);

            // update Values
            i = j;
            j += datagramSize;
            if (j > fileContent.length)
                j = fileContent.length;

            // update Id
            id[1]++;
            if (id[1] == 127)
            {
                id[1] = 0;
                id[0] ++;
            }

        }
    }

    private void sendEndMessage()
    {
        byte[] dataToSend = new byte[1400];
        dataToSend[0] = (byte) 1;
        // 1 = message end
        send(dataToSend);
    }

    private void send(byte[] dataToSend)
    {
        DatagramPacket packet = new DatagramPacket(dataToSend, dataToSend.length, address, 1235);

        System.out.println("Sending sent: id " + ( dataToSend[1] * 127 + dataToSend[2] ));

        try {
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.receive();
    }
}
