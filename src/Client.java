import javax.annotation.processing.SupportedSourceVersion;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Client {
    private InetAddress address;
    DatagramSocket socket;
    int totalPackets;
    int checksumErrors = 0;
    int packetsMissed = 0;
    byte[] hash;
    int DATA_SIZE = 1394;

    public Client() throws UnknownHostException, SocketException {
        address = InetAddress.getByName("localhost");
        socket = new DatagramSocket(1235); // listens on 1235
    }

    public void send(String text) throws IOException {
        byte[] bytes = text.getBytes();

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 1234);
        socket.send(packet);
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

    private byte[] createHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(fileContent);
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    public void checkHash(byte[] imageBytes)
    {
        if (Arrays.equals(createHash(imageBytes), hash))
        {
            System.out.println("Hash validated!");
        }
        else
        {
            System.out.println("Hash error!");
        }
    }

    public ArrayList<byte[]> receive(ArrayList<byte[]> fileContent) throws IOException {
        System.out.println("Receiving");
        int totalReceived = 0;

//        socket.setSoTimeout(1000);
//        long lastReceivedTime = System.currentTimeMillis();

        while (true)
        {
            byte[] text = new byte[1400];

            DatagramPacket packet = new DatagramPacket(text, text.length);

//            try {
                socket.receive(packet);
//                lastReceivedTime = System.currentTimeMillis();
//            } catch (SocketTimeoutException e) {
//            }

//            long now = System.currentTimeMillis();
//
//            if (now - lastReceivedTime > 5000) {
//                System.out.println("Timeout");
//                break;
//            }

            byte[] received = packet.getData();

            // filtering received data
            if (received[0] == (byte) 1)
            {
                System.out.println("Received hash");
                hash = Arrays.copyOfRange(received, 1, 33);
                break;
            }
            else if (received[0] == (byte) 2) // error message
                handleError(received);

            byte[] packetIDBytes = Arrays.copyOfRange(received, 1, 3);
            int packetID = convertByteToInt(packetIDBytes);
            byte[] totalPacketsBytes = Arrays.copyOfRange(received, 3, 5);
            this.totalPackets = Math.max(convertByteToInt(totalPacketsBytes), this.totalPackets);
            System.out.println(this.totalPackets);
            byte checksum = received[5];

            // check checksum
            int total = 0;
            byte[] data = Arrays.copyOfRange(received, 6, received.length);
            for (byte b : data)
            {
                total += b;
            }
            if ((total % 127) == (int) checksum) {
                // only add the data if the checksum is correct

                if (fileContent.size() <= packetID) {
                    // Grow the list with null placeholders
                    fileContent.add(null);
                    while (fileContent.size() <= packetID) {
                        fileContent.add(null);
                        packetsMissed++;
                    }
                }

                fileContent.set(packetID, Arrays.copyOfRange(received, 1, received.length));

//            if (received.length() > 0)
//            {
                System.out.println("Received id: " + (packetID));
                totalReceived++;
//            }
//            System.out.println("Total Received: " + totalReceived);

//                if (packetID == this.totalPackets)
//                    break;
            }
            else {
                System.out.println("Checksum Error!");
                checksumErrors++;
            }
        }

        return fileContent;
    }

    private void handleError(byte[] data)
    {
        System.out.println("Error Message");
        String message = new String(Arrays.copyOfRange(data, 1, data.length), StandardCharsets.UTF_8);
        System.out.println(message);

    }

    private void reassembleImage(ArrayList<byte[]> fileContent, String imageName)
    {
        byte[] imageBytes = new byte[fileContent.size() * DATA_SIZE];
        for (int i = 0; i < fileContent.size(); i++)
        {
            for (int j = 0; j < fileContent.get(i).length-5; j++)
            {
                imageBytes[i * DATA_SIZE + j] = fileContent.get(i)[j+5];
            }
        }

//        checkHash(imageBytes);

        Path outputPath = Path.of("../media/split/" + imageName);

        try {
            Files.write(outputPath, imageBytes);
            System.out.println("Image written to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.totalPackets = 0;

        String imageName = "foto1.jpg";
        if (args.length != 2) {
            client.send("GET " + imageName);
            // example
        } else {
            client.send(String.join(" ", args));
            imageName = args[1];
            System.out.println(imageName);
        }


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
            System.out.println("Total packets: " + client.totalPackets);
            for (int i = 0; i < client.totalPackets && totalPacketsMissing < 10; i++)
            {
//                if (fileContent.size() <= i)
//                    fileContent.add(null);
                if (fileContent.get(i) == null)
                {
                    packetsMissing += i + " ";
                    totalPacketsMissing++;
                    System.out.println("Packets Missing: " + i);
                }
            }
            if (totalPacketsMissing > 0)
                client.send("NAC " + packetsMissing);
            System.out.println("Size: " + fileContent.size());
        }

        System.out.println("Checksum Errors: " + client.checksumErrors);
        System.out.println("Packets Missed:  " + client.packetsMissed);
        client.reassembleImage(fileContent, imageName);


    }
}
