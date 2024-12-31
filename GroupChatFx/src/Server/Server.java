package Server;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Application {
	//khai báo các thuộc tính 
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Set<PrintWriter> clientWriters = new HashSet<>();
    private TextArea logArea;
    private List<String> chatHistory = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        Button startButton = new Button("Start Server");
        Button stopButton = new Button("Stop Server");
        stopButton.setDisable(true);

        startButton.setOnAction(event -> {
            startServer();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        });

        stopButton.setOnAction(event -> {
            stopServer();
            stopButton.setDisable(true);
            startButton.setDisable(false);
        });

        logArea = new TextArea();
        logArea.setEditable(false);

        HBox buttonBox = new HBox(10, startButton, stopButton);
        VBox root = new VBox(10, buttonBox, logArea);

        Scene scene = new Scene(root, 600, 400);
//        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());

        
        primaryStage.setTitle("Server Control");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(12345);
            isRunning = true;
            log("Server started on port 12345");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRunning) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            clientWriters.add(out);

                            sendChatHistory(out);

                            new Thread(new ClientHandler(clientSocket, out)).start();
                        } catch (IOException e) {
                            if (isRunning) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        if (isRunning) {
            try {
                isRunning = false;

                for (PrintWriter writer : clientWriters) {
                    writer.println("Server đã ngừng hoạt động. Ngắt kết nối.");
                    writer.close();
                }
                clientWriters.clear();

                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                log("Server stopped.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    // Lớp xử lý client
    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String userName;

        public ClientHandler(Socket socket, PrintWriter out) {
            this.socket = socket;
            this.out = out;
        }

        @Override
        public void run() {
            try (Scanner in = new Scanner(socket.getInputStream())) {
                String message;

                while ((message = in.nextLine()) != null) {
                    if (message.startsWith("SET_NAME:")) {
                        userName = message.substring("SET_NAME:".length());
                        String joinMessage = formatMessage(userName + " đã tham gia đoạn chat.");
                        log(joinMessage);
                        broadcastMessage(joinMessage, out);
                    } else {
                        String chatMessage = formatMessage(userName + ": " + message);
                        log(chatMessage);
                        broadcastMessage(chatMessage, out);
                        addToChatHistory(chatMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String leaveMessage = formatMessage(userName + " đã ngắt kết nối.");
                broadcastMessage(leaveMessage, out);
                clientWriters.remove(out);
            }
        }
        private void broadcastMessage(String message, PrintWriter excludeWriter) {
            for (PrintWriter writer : clientWriters) {
                if (writer != excludeWriter) {
                    writer.println(message);
                }
            }
        }

        // Method to add messages to the chat history
        private void addToChatHistory(String message) {
            chatHistory.add(message);
        }
    }

    // Method to send the chat history to a newly connected client
    private void sendChatHistory(PrintWriter out) {
        for (String message : chatHistory) {
            out.println(message);
        }
    }
    private String formatMessage(String message) {
        return getCurrentTimeStamp() + "\n" + message;
    }

    private String getCurrentTimeStamp() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    public static void main(String[] args) {
        launch(args);
    }
}
