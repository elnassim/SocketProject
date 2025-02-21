import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1234)) {
            System.out.println("Connected to server.");

            // Create input/output streams
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            String userInput;
            System.out.println("Type a message to send to the server (type 'exit' to quit):");

            // Continuously read from the console and send messages to the server
            while ((userInput = consoleInput.readLine()) != null) {
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
                out.println(userInput);
                String response = in.readLine();
                System.out.println("Received from server: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}