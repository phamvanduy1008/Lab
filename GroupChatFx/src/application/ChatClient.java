package application;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient extends Application {
    private Socket socket;
    private PrintWriter out;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showInitialScreen();
    }

    // Hiển thị giao diện nhập tên người dùng
    private void showInitialScreen() {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Nhập tên của bạn...");

        Button continueButton = new Button("Tiếp tục");
        continueButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String userName = usernameField.getText();
                if (!userName.isEmpty()) {
                    showChatScreen(userName);
                }
            }
        });

        VBox initialRoot = new VBox(10, usernameField, continueButton);

        Scene initialScene = new Scene(initialRoot, 400, 200);


        primaryStage.setTitle("Client - Nhập Tên");
        primaryStage.setScene(initialScene);
        primaryStage.show();
    }

    // Hiển thị giao diện chat sau khi nhập tên
    private void showChatScreen(String userName) {
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);

        TextField messageField = new TextField();
        messageField.setPromptText("Nhập tin nhắn...");

        Button sendButton = new Button("Gửi");
        sendButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                sendMessage(messageField, chatArea);
            }
        });

        Label userLabel = new Label("Bạn: " + userName);

        VBox chatRoot = new VBox(10, userLabel, chatArea, messageField, sendButton);
        Scene chatScene = new Scene(chatRoot, 400, 300);

        primaryStage.setTitle("GroupChat");
        primaryStage.setScene(chatScene);
        primaryStage.show();

        connectToServer(userName, chatArea);
    }

    // Kết nối đến server
    private void connectToServer(String userName, TextArea chatArea) {
        try {
            socket = new Socket("192.168.28.113", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("SET_NAME:" + userName);
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listenForMessages(chatArea);
                }
            }).start();
        } catch (IOException e) {
            chatArea.appendText("Nhóm chat chưa hoạt động. Vui lòng thử lại sau.\n");
        }
    }

    // Lắng nghe tin nhắn từ server và cập nhật vào giao diện
    private void listenForMessages(TextArea chatArea) {
        try (Scanner in = new Scanner(socket.getInputStream())) {
            String message;
            while ((message = in.nextLine()) != null) {
                final String msg = message;
                javafx.application.Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        chatArea.appendText(msg + "\n");
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gửi tin nhắn từ client đến server
    private void sendMessage(TextField messageField, TextArea chatArea) {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            String timestampedMessage = formatMessage("Bạn: " + message);
            out.println(message); 
            chatArea.appendText(timestampedMessage + "\n"); 
            messageField.clear();
        }
    }

    private String formatMessage(String message) {
        return getCurrentTimeStamp() + "\n" + message;
    }

    private String getCurrentTimeStamp() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    // Đóng kết nối khi ứng dụng dừng
    @Override
    public void stop() throws Exception {
        super.stop();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
