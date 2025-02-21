import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        // Initialize input/output streams
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String message;
            // Continuously read messages from the client
            while ((message = in.readLine()) != null) {
                System.out.println("Received from client: " + message);
                // Send a response back to the client
                out.println("Server response: " + message);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + clientSocket.getInetAddress());
        } finally {
            // Cleanup: Close resources
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}