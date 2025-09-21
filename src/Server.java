import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
        splitImage(text);
        send();
    }

    private void splitImage(String name)
    {
        File file = null;
        BufferedImage image = null;
        try {
            file = new File("../media/" + name); // I have bear.jpg in my working directory
            FileInputStream fis = new FileInputStream(file);
            image = ImageIO.read(fis); //reading the image file
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }

        int imagesAmount = Math.toIntExact(file.length() / 1400);

        System.out.println("Divisons: " + ++imagesAmount);

        int rows = (int) Math.ceil(Math.sqrt(imagesAmount)); //You should decide the values for rows and cols variables
        int cols = rows;
        int chunks = rows * cols;

        int chunkWidth = image.getWidth() / cols; // determines the chunk width and height
        int chunkHeight = image.getHeight() / rows;
        int count = 0;
        BufferedImage imgs[] = new BufferedImage[chunks]; //Image array to hold image chunks
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                //Initialize the image array with image chunks
                imgs[count] = new BufferedImage(chunkWidth, chunkHeight, image.getType());

                // draws the image chunk
                Graphics2D gr = imgs[count++].createGraphics();
                gr.drawImage(image, 0, 0, chunkWidth, chunkHeight, chunkWidth * y, chunkHeight * x, chunkWidth * y + chunkWidth, chunkHeight * x + chunkHeight, null);
                gr.dispose();
            }
        }
        System.out.println("Splitting done");

        //writing mini images into image files
        for (int i = 0; i < imgs.length; i++) {
            try {
                ImageIO.write(imgs[i], "jpg", new File("../media/split/img" + i + ".jpg"));
            } catch (IOException e) {
                System.err.println("Runtime Exception: " + e.getMessage());
            }
        }
        System.out.println("Mini images created");
    }

//    private void send()
//    {
//        DatagramPacket packet = new DatagramPacket()
//    }
//

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.receive();
    }
}
