import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        packages = new ArrayList<byte[]>();
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

    private byte[] createHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileContent);
            return hash;
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    private void splitImage(String name) {
        File fi = new File("../media/" + name);

        if (!fi.isFile())
        {
            sendErrorMessage("File '" + name + "' not found");
            System.out.println("Sending message is not file");
            return;
        }
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(fi.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int length = fileContent.length;
        System.out.println("Length: " + length);
        int datagramSize = 1394;
        int i = 0, j = datagramSize;
        int steps = (int) Math.ceil((double) length / datagramSize);

        byte[] id = new byte[2];

        byte[] fileContent2 = new byte[steps * datagramSize];
        for (int k = 0; k < fileContent.length; k++)
            fileContent2[k] = fileContent[k];
        // fileContent2 = fileContent com os zeros adicionais

        byte[] hash = createHash(fileContent2);

        for (int k = 0; k < steps; k++)
        {
            byte[] dataToSend = new byte[1400];
            byte[] data = Arrays.copyOfRange(fileContent, i, j);

            for (int l = 6; l < data.length + 6; l++)
                dataToSend[l] = data[l-6];

            // Header pt0: type of message
            dataToSend[0] = (byte) 0;
                // 0 = sending data
                // 1 = message end
                // 2 = error

            // Header pt1: packet ID
            dataToSend[1] = id[0];
            dataToSend[2] = id[1];

            // Header pt2: totalPackets
            dataToSend[3] = (byte) ( steps / 127 );
            dataToSend[4] = (byte) ( steps % 127 );

            int total = 0;
            for (byte b : data)
            {
                total += b;
            }
            dataToSend[5] = (byte) ( total % 127 );
            // checksum

            packages.add(dataToSend);

            if (k % 10 != 0)
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

        byte[] dataToSend = new byte[1400];
        dataToSend[0] = 1;
        System.out.println("Hash length: " + hash.length);
        for(int i2 = 0; i2 < hash.length; i2++)
        {
            dataToSend[i2+1] = hash[i2];
        }

        send(dataToSend);

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
        try {
            Thread.sleep(1);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void sendErrorMessage(String message)
    {
        byte[] bytesMessage = message.getBytes();
        byte[] bytesTotal = new byte[message.getBytes().length + 1];
        bytesTotal[0] = (byte) 2;
        for (int i = 1; i < bytesTotal.length; i++)
        {
            bytesTotal[i] = bytesMessage[i-1];
        }

        DatagramPacket packet = new DatagramPacket(bytesTotal, bytesTotal.length, address, 1235);
        
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
