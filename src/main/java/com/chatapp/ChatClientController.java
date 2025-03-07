package com.chatapp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClientController {

    @FXML
    private ScrollPane messageScrollPane;

    @FXML
    private TextFlow messageDisplay;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    private String username;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    public void initData(String username) {
        this.username = username;
    }

    public void connectToServer() {
        new Thread(() -> {
            try {
                // Connect to server
                socket = new Socket("localhost", 1234);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send initial connection message
                out.println("User connected: " + username);
                connected = true;

                // Add welcome message to chat
                Platform.runLater(() -> {
                    addMessage("System: Connected to server as " + username);
                    addMessage("System: Type your message and press Send or Enter to chat");
                });

                // Start receiving messages
                String message;
                while ((message = in.readLine()) != null) {
                    final String receivedMessage = message;
                    Platform.runLater(() -> addMessage(receivedMessage));
                }
            } catch (IOException e) {
                Platform.runLater(() -> addMessage("Error: " + e.getMessage()));
                e.printStackTrace();
            } finally {
                connected = false;
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> addMessage("System: Disconnected from server"));
            }
        }).start();
    }

    @FXML
    public void handleSendButtonAction(ActionEvent event) {
        sendMessage();
    }

    @FXML
    public void initialize() {
        // Set up enter key to send messages
        messageInput.setOnAction(event -> sendMessage());

        // Set up auto-scrolling
        messageDisplay.heightProperty().addListener((observable, oldValue, newValue) -> {
            messageScrollPane.setVvalue(1.0);
        });
    }

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty() || !connected)
            return;

        // Send the message
        out.println(username + ": " + message);

        // Clear the input field
        messageInput.clear();
        messageInput.requestFocus();
    }

    private void addMessage(String message) {
        Text text = new Text(message + "\n");
        messageDisplay.getChildren().add(text);
    }
}
