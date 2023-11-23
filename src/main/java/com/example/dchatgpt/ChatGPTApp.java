package com.example.dchatgpt;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatGPTApp extends Application {

    private final String apiKey = "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";
    private final String gpt3Endpoint = "https://api.openai.com/v1/chat/completions";

    private final TextArea chatArea = new TextArea();
    private final TextField inputField = new TextField();
    private final Button sendButton = new Button("Send");
    private final List<String> chatHistory = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DChatGPT");

        chatArea.setEditable(false);

        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToHeight(true);

        inputField.setPromptText("Type your message here...");

        sendButton.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setAlignment(Pos.CENTER);

        VBox chatVBox = new VBox(10, scrollPane, inputBox);
        chatVBox.setAlignment(Pos.CENTER);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(chatVBox);

        Scene scene = new Scene(borderPane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendMessage() {
        String userMessage = inputField.getText();
        if (!userMessage.isEmpty()) {
            chatHistory.add("User: " + userMessage);
            updateChatArea();

            // Send user message to GPT-3 API in a background thread
            executorService.submit(() -> {
                String gpt3Response = getGPT3ResponseWithRetry(userMessage);
                Platform.runLater(() -> {
                    chatHistory.add("ChatGPT: " + gpt3Response);
                    updateChatArea();
                });
            });

            // Clear the input field
            inputField.clear();
        }
    }

    private void updateChatArea() {
        StringBuilder chatContent = new StringBuilder();
        for (String message : chatHistory) {
            chatContent.append(message).append("\n");
        }
        Platform.runLater(() -> chatArea.setText(chatContent.toString()));
    }

    private String getGPT3ResponseWithRetry(String userMessage) {
        int maxRetries = 3;
        int retryDelayMillis = 1000; // 1 second delay

        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                String gpt3Response = getGPT3Response(userMessage);
                if (!gpt3Response.startsWith("Error")) {
                    return gpt3Response; // Successful response
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (retry < maxRetries - 1) {
                // Retry after a delay
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return "Error processing request. Check the console for details.";
    }

    private String getGPT3Response(String userMessage) throws InterruptedException {
        try {
            URL url = new URL(gpt3Endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            String payload = "{\"messages\": [{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},{\"role\": \"user\", \"content\": \"" + userMessage + "\"}]}";

            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (IOException e) {
            return "Error processing request.";
        }
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}