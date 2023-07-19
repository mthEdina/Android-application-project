import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws IOException {

       Socket socket = new Socket("localhost", 1234);
       System.out.println("Connected to the server..");

        JFrame jFrame = new JFrame("Client");
        jFrame.setSize(400, 400);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon imageIcon = new ImageIcon("D:\\LineUs\\android-basics-edina\\Client\\img\\rose.png");

        JLabel jLabelPic = new JLabel(imageIcon);
        JButton jButton = new JButton("Send image to Server");

        jFrame.add(jLabelPic, BorderLayout.NORTH);
        jFrame.add(jButton, BorderLayout.SOUTH);

        jFrame.setVisible(true);

       jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                    Image image = imageIcon.getImage();
                    System.out.println(image);

                    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
                    System.out.println(bufferedImage);
                    Graphics graphics = bufferedImage.createGraphics();
                    graphics.drawImage(image, 0, 0, null);
                    graphics.dispose();

                    ImageIO.write(bufferedImage, "png", bufferedOutputStream);

                    System.out.println("cliens: " + bufferedOutputStream);
                    bufferedOutputStream.close();
                    socket.close();

                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}