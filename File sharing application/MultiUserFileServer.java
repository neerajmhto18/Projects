import java.io.*; 
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MultiUserFileServer {
    private static final int PORT = 7777;
    private static final String FILES_DIRECTORY = "files/";

    private static final Lock lock = new ReentrantLock();

    // Other parts of the server code remain the same

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
            ) {
                // Read the operation type from the client (UPLOAD or DOWNLOAD)
                String operationType = (String) in.readObject();

                if ("UPLOAD".equals(operationType)) {
                    // Receive the uploaded file from the client
                    receiveFileFromClient(in);
                } else if ("DOWNLOAD".equals(operationType)) {
                    // Read the requested file name from the client
                    String fileName = (String) in.readObject();

                    // Send the file to the client
                    sendFileToClient(out, fileName);
                }

                System.out.println("Operation completed successfully for: " + clientSocket.getInetAddress());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void receiveFileFromClient(ObjectInputStream in) throws IOException {
            try {
                lock.lock(); // Acquire the lock to ensure synchronization

                // Read the file name from the client
                String fileName = (String) in.readObject();

                // Receive the file size from the client
                long fileSize = in.readLong();

                File file = new File(FILES_DIRECTORY + File.separator + fileName);

                // Create the directory if it doesn't exist
                File directory = file.getParentFile();
                if (!directory.exists()) {
                    if (directory.mkdirs()) {
                        System.out.println("Directory created: " + directory.getAbsolutePath());
                    } else {
                        System.err.println("Failed to create directory: " + directory.getAbsolutePath());
                    }
                }

                // Receive the file data from the client
                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        // Simulate uploading progress (in a real application, update a progress bar)
                        System.out.println("Uploaded: " + (totalBytesRead * 100 / fileSize) + "%");
                    }

                    System.out.println("File saved to: " + file.getAbsolutePath());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                lock.unlock(); // Release the lock
            }
        }


        private void sendFileToClient(ObjectOutputStream out, String fileName) throws IOException {
            try {
                lock.lock(); // Acquire the lock to ensure synchronization

                File file = new File(FILES_DIRECTORY + fileName);

                if (!file.exists()) {
                    out.writeObject(false);
                    System.out.println("File does not exist on the server.");
                    return;
                }

                out.writeObject(true);
                System.out.println("File exists on the server.");

                // Send the file size to the client
                out.writeLong(file.length());
                out.flush();
                System.out.println("Sent file size: " + file.length() + " bytes");

                // Send the file data to the client
                try (FileInputStream fileIn = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("File sent to client.");
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("IOException during file transfer: " + e.getMessage());
                throw e; // Re-throw the exception after logging
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        // Close the ObjectOutputStream only if it was not closed by an exception
                        if (!out.equals(new ObjectOutputStream(new ByteArrayOutputStream()))) {
                            out.close();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("IOException during ObjectOutputStream cleanup: " + e.getMessage());
                } finally {
                    lock.unlock(); // Release the lock in the finally block
                }
            }
        }
    }


    public static void main(String[] args) {
    	try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    	    System.out.println("Server is listening on port " + PORT);

    	    // Use a thread pool for handling multiple clients
    	    ExecutorService executorService = Executors.newCachedThreadPool();

    	    while (true) {
    	        Socket clientSocket = serverSocket.accept();
    	        System.out.println("Accepted connection from: " + clientSocket.getInetAddress());

    	        // Handle each client in a separate thread
    	        executorService.execute(new ClientHandler(clientSocket));
    	    }
    	} catch (SocketException e) {
    		if (e.getMessage().equals("Connection reset")) {
    	        // Handle connection reset by peer
    	        System.out.println("Connection reset by peer");
    	    } else {
    	        // Handle other socket exceptions
    	        e.printStackTrace();
    	    }
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}

    }
}