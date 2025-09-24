import javax.annotation.processing.SupportedSourceVersion;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Client {
    private InetAddress address;
    DatagramSocket socket;
    int totalPackets;

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

    private int convertByteToInt(byte[] bytes)
    {
        int total = 0;
        for (int i = bytes.length-1; i >= 0 ; i--)
        {
           total += (int) bytes[i] * (int) ( Math.pow(127, bytes.length-1-i) );
        }
        return total;
    }

    public ArrayList<byte[]> receive(ArrayList<byte[]> fileContent) throws IOException {
        System.out.println("Receiving");
        int totalReceived = 0;

        while (true)
        {
            byte[] text = new byte[1400];

            DatagramPacket packet = new DatagramPacket(text, text.length);

            socket.receive(packet);

            byte[] received = packet.getData();
            if (received[0] == (byte) 1)
                break;

            byte[] packetIDBytes = Arrays.copyOfRange(received, 1, 3);
            int packetID = convertByteToInt(packetIDBytes);
            byte[] totalPacketsBytes = Arrays.copyOfRange(received, 3, 5);
            this.totalPackets = convertByteToInt(totalPacketsBytes);


            if (fileContent.size() <= packetID) {
                // Grow the list with null placeholders
                while (fileContent.size() <= packetID) {
                    fileContent.add(null);
                }
            }

            fileContent.set(packetID, Arrays.copyOfRange(received, 1, received.length));

//            if (received.length() > 0)
//            {
                System.out.println("Received id: " + ( packetID ) );
                totalReceived++;
//            }
//            System.out.println("Total Received: " + totalReceived);

            if (packetID == this.totalPackets - 1)
                break;
        }

        return fileContent;
    }

    private void reassembleImage(ArrayList<byte[]> fileContent)
    {
        byte[] imageBytes = new byte[fileContent.size() * 1395];
        for (int i = 0; i < fileContent.size(); i++)
        {
            for (int j = 0; j < fileContent.get(i).length-4; j++)
            {
                imageBytes[i * 1395 + j] = fileContent.get(i)[j+4];
            }
        }

        Path outputPath = Path.of("output.png");

        try {
            Files.write(outputPath, imageBytes);
            System.out.println("Image written to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.send("GET foto1.png");
        client.totalPackets = 0;

        ArrayList <byte[]> fileContent = new ArrayList<byte[]>();

        int totalPacketsMissing = 1;

        while (totalPacketsMissing > 0)
        {
            System.out.println("Packets missing");
            totalPacketsMissing = 0;
            fileContent = client.receive(fileContent);

            // verify packets
            String packetsMissing = "";
            System.out.println("Size: " + fileContent.size());
            for (int i = 0; i < client.totalPackets && totalPacketsMissing < 10; i++)
            {
                if (fileContent.get(i) == null)
                {
                    packetsMissing += i + " ";
                    totalPacketsMissing++;
                    System.out.println("Packets Missing: " + i);
                }
            }
            client.send("NAC " + packetsMissing);
            System.out.println("Size: " + fileContent.size());
        }

        client.reassembleImage(fileContent);


    }
}
