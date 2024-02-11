import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class MultiUserFileClient2 extends JFrame {
    private static final String SERVER_ADDRESS = "10.140.0.233";
    private static final int PORT = 7777;

    private final JButton downloadButton;
    private final JButton uploadButton;
    private final JTextField fileNameTextField;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public MultiUserFileClient2() {
        super("Multi-User File Transfer Client");
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        fileNameTextField = new JTextField(20);
        downloadButton = new JButton("Download");
        uploadButton = new JButton("Upload");

        initializeConnection();

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileName = fileNameTextField.getText().trim();
                if (!fileName.isEmpty()) {
                    downloadFile(fileName);
                } else {
                    JOptionPane.showMessageDialog(MultiUserFileClient2.this, "Please enter a file name.");
                }
            }
        });

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileName = fileNameTextField.getText().trim();
                if (!fileName.isEmpty()) {
                    uploadFile(fileName);
                } else {
                    JOptionPane.showMessageDialog(MultiUserFileClient2.this, "Please enter a file name.");
                }
            }
        });

        add(new JLabel("Enter File Name: "));
        add(fileNameTextField);
        add(downloadButton);
        add(uploadButton);

        setVisible(true);
    }

    private void initializeConnection() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error initializing connection");
            System.exit(1);
        }
    }

    private void downloadFile(String fileName) {
        try {
            // Send the operation type (DOWNLOAD) to the server
            out.writeObject("DOWNLOAD");
            out.flush();

            // Send the requested file name to the server
            out.writeObject(fileName);

            // Check if the file exists on the server
            boolean fileExists = (boolean) in.readObject();
            if (!fileExists) {
                JOptionPane.showMessageDialog(this, "File does not exist on the server.");
                return;
            }

            // Receive the file size from the server
            long fileSize = in.readLong();

            // Receive the file data from the server
            try (FileOutputStream fileOut = new FileOutputStream("downloaded_" + fileName)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Simulate downloading progress (in a real application, update a progress bar)
                    System.out.println("Downloaded: " + (totalBytesRead * 100 / fileSize) + "%");
                }

                JOptionPane.showMessageDialog(this, "Download complete!");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error downloading file");
        }
    }

    private void uploadFile(String fileName) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             FileInputStream fileIn = new FileInputStream(fileName)) {

            // Send the operation type (UPLOAD) to the server
            out.writeObject("UPLOAD");
            out.flush();

            // Send the file name to the server
            out.writeObject(fileName);

            // Send the file size to the server
            out.writeLong(new File(fileName).length());
            out.flush();

            // Send the file data to the server
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            // Flush and close the output stream
            out.flush();

            // Wait for acknowledgment or additional handling if needed
            // (e.g., server responding with success or failure)

            JOptionPane.showMessageDialog(this, "Upload complete!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error uploading file");
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MultiUserFileClient2();
            }
        });
    }
}