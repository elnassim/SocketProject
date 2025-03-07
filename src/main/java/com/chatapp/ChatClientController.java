package com.chatapp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;

public class ChatClientController {

    @FXML
    private ScrollPane messageScrollPane;

    @FXML
    private TextFlow messageDisplay;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    private String userEmail;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private HashSet<String> contacts = new HashSet<>();
    private static final String CONTACTS_FILE_PREFIX = "contacts_";

    @FXML
    public void initialize() {
        // Set up enter key to send messages
        messageInput.setOnAction(event -> sendMessage());

        // Set up auto-scrolling
        messageDisplay.heightProperty().addListener((observable, oldValue, newValue) -> {
            messageScrollPane.setVvalue(1.0);
        });
    }

    public void initChatSession(String email, Socket socket, BufferedReader in, PrintWriter out) {
        this.userEmail = email;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.connected = true;
        
        // Load contacts
        loadContacts();
        
        // Add welcome message
        addSystemMessage("Connected as " + userEmail);
        addSystemMessage("Your contacts: " + contacts);
        addSystemMessage("Commands:\n" +
                        "/add email@example.com - Add a contact\n" +
                        "/list - List your contacts\n" +
                        "@email@example.com message - Send a private message");
        
        // Start listening for messages
        startMessageListener();
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    final String receivedMsg = message;
                    
                    // Try to parse as JSON
                    try {
                        JSONObject msgJson = new JSONObject(receivedMsg);
                        if (msgJson.has("type") && msgJson.has("content")) {
                            String type = msgJson.getString("type");
                            String content = msgJson.getString("content");
                            String sender = msgJson.optString("sender", "Server");
                            
                            if ("private".equals(type)) {
                                Platform.runLater(() -> addPrivateMessage(sender, content));
                            } else {
                                Platform.runLater(() -> addMessage(sender, content));
                            }
                        } else {
                            Platform.runLater(() -> addMessage("", receivedMsg));
                        }
                    } catch (JSONException e) {
                        // Not JSON, display as plain text
                        Platform.runLater(() -> addMessage("", receivedMsg));
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    Platform.runLater(() -> addSystemMessage("Connection lost: " + e.getMessage()));
                }
            }
        }).start();
    }

    @FXML
    public void handleSendButtonAction(ActionEvent event) {
        sendMessage();
    }

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty() || !connected)
            return;
        
        try {
            if (message.startsWith("/add")) {
                // Add contact command
                if (message.split(" ").length > 1) {
                    String contact = message.split(" ", 2)[1];
                    if (!contacts.contains(contact)) {
                        contacts.add(contact);
                        saveContacts();
                        Platform.runLater(() -> addSystemMessage(contact + " added to contacts."));
                    } else {
                        Platform.runLater(() -> addSystemMessage(contact + " is already in your contacts."));
                    }
                } else {
                    Platform.runLater(() -> addSystemMessage("Usage: /add email@example.com"));
                }
            } else if (message.startsWith("/list")) {
                // List contacts command
                Platform.runLater(() -> addSystemMessage("Your contacts: " + contacts));
            } else if (message.startsWith("@")) {
                // Private message
                int spaceIndex = message.indexOf(" ");
                if (spaceIndex > 1) {
                    String recipient = message.substring(1, spaceIndex);
                    if (contacts.contains(recipient)) {
                        // Create JSON for private message
                        JSONObject privateMsg = new JSONObject();
                        privateMsg.put("type", "private");
                        privateMsg.put("to", recipient);
                        privateMsg.put("content", message.substring(spaceIndex + 1));
                        privateMsg.put("from", userEmail);
                        out.println(privateMsg.toString());
                        
                        // Display outgoing message
                        String content = message.substring(spaceIndex + 1);
                        Platform.runLater(() -> addOutgoingPrivateMessage(recipient, content));
                    } else {
                        Platform.runLater(() -> addSystemMessage("User not in contacts. Add them first using /add " + recipient));
                    }
                } else {
                    Platform.runLater(() -> addSystemMessage("Usage: @email@example.com message"));
                }
            } else {
                // Regular broadcast message
                JSONObject broadcastMsg = new JSONObject();
                broadcastMsg.put("type", "broadcast");
                broadcastMsg.put("content", message);
                broadcastMsg.put("from", userEmail);
                out.println(broadcastMsg.toString());
                
                // Display outgoing message
                Platform.runLater(() -> addOutgoingMessage(message));
            }
        } catch (Exception e) {
            Platform.runLater(() -> addSystemMessage("Error sending message: " + e.getMessage()));
        }
        
        // Clear input field
        messageInput.clear();
        messageInput.requestFocus();
    }
    
    private void addSystemMessage(String message) {
        Text text = new Text(message + "\n");
        text.setFill(Color.GRAY);
        text.setFont(Font.font("System", 12));
        messageDisplay.getChildren().add(text);
    }
    
    private void addMessage(String sender, String message) {
        Text text = new Text(sender.isEmpty() ? message + "\n" : sender + ": " + message + "\n");
        text.setFill(Color.BLACK);
        messageDisplay.getChildren().add(text);
    }
    
    private void addPrivateMessage(String sender, String message) {
        Text prefix = new Text("[PRIVATE] ");
        prefix.setFill(Color.PURPLE);
        
        Text content = new Text(sender + ": " + message + "\n");
        content.setFill(Color.DARKBLUE);
        
        messageDisplay.getChildren().addAll(prefix, content);
    }
    
    private void addOutgoingMessage(String message) {
        Text text = new Text("You: " + message + "\n");
        text.setFill(Color.BLUE);
        messageDisplay.getChildren().add(text);
    }
    
    private void addOutgoingPrivateMessage(String recipient, String message) {
        Text prefix = new Text("[PRIVATE to " + recipient + "] ");
        prefix.setFill(Color.PURPLE);
        
        Text content = new Text("You: " + message + "\n");
        content.setFill(Color.BLUE);
        
        messageDisplay.getChildren().addAll(prefix, content);
    }
    
    private void loadContacts() {
        String filename = CONTACTS_FILE_PREFIX + userEmail.replace("@", "_at_").replace(".", "_dot_") + ".txt";
        File file = new File(filename);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        contacts.add(line.trim());
                    }
                }
                Platform.runLater(() -> addSystemMessage("Contacts loaded successfully."));
            } catch (IOException e) {
                Platform.runLater(() -> addSystemMessage("Error loading contacts: " + e.getMessage()));
            }
        } else {
            Platform.runLater(() -> addSystemMessage("No previous contacts found."));
        }
    }

    private void saveContacts() {
        String filename = CONTACTS_FILE_PREFIX + userEmail.replace("@", "_at_").replace(".", "_dot_") + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (String contact : contacts) {
                writer.println(contact);
            }
        } catch (IOException e) {
            Platform.runLater(() -> addSystemMessage("Error saving contacts: " + e.getMessage()));
        }
    }
}