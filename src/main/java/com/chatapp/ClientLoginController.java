package com.chatapp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the client login interface
 */
public class ClientLoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button connectButton;

    @FXML
    private Label messageLabel;

    @FXML
    public void initialize() {
        // Set focus on email field when the form loads
        Platform.runLater(() -> emailField.requestFocus());
    }

    @FXML
    public void handleConnectButtonAction(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both email and password");
            messageLabel.getStyleClass().add("error-message");
            return;
        }

        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            messageLabel.setText("Please enter a valid email");
            messageLabel.getStyleClass().add("error-message");
            return;
        }

        // For now, we'll use a simple check - in a real app, you would verify against a
        // server
        if ((email.equals("admin@example.com") && password.equals("admin123")) ||
                (email.equals("user@example.com") && password.equals("user123"))) {

            // Extract username from email (part before @)
            String username = email.substring(0, email.indexOf("@"));

            messageLabel.setText("Login successful! Connecting...");
            messageLabel.getStyleClass().clear();
            messageLabel.getStyleClass().add("success-message");

            // Launch chat client UI instead of console client
            try {
                launchChatClientUI(username);
            } catch (Exception e) {
                e.printStackTrace();
                messageLabel.setText("Connection error: " + e.getMessage());
                messageLabel.getStyleClass().clear();
                messageLabel.getStyleClass().add("error-message");
            }
        } else {
            messageLabel.setText("Invalid email or password");
            messageLabel.getStyleClass().clear();
            messageLabel.getStyleClass().add("error-message");
        }
    }

    private void launchChatClientUI(String username) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/chatClient.fxml"));
        Parent chatView = loader.load();

        // Get the controller and initialize it with the username
        ChatClientController controller = loader.getController();
        controller.initData(username);

        // Create new scene
        Scene chatScene = new Scene(chatView, 600, 400);

        // Get current stage
        Stage stage = (Stage) connectButton.getScene().getWindow();

        // Configure and show the chat UI
        stage.setTitle("Chat Client - " + username);
        stage.setScene(chatScene);
        stage.setResizable(true);
        stage.show();

        // Connect to server
        controller.connectToServer();
    }
}
