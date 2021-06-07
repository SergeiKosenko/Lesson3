package ru.kosenko.chat_server;

import ru.kosenko.chat_server.server.ChatServer;
import ru.kosenko.common.ChatMessage;
import ru.kosenko.common.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler {
    private static final long AUTH_TIMEOUT = 120_000;
    private Socket socket;
    private ChatServer chatServer;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private String currentUsername;

    public ClientHandler(Socket socket, ChatServer chatServer) {
        try {
            this.chatServer = chatServer;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Client handler created!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle() {
        new Thread(() -> {
            try {
//                while(!Thread.currentThread().isInterrupted() || socket.isConnected()) {
                authenticate();
                readMessages();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void readMessages() throws IOException {
        try {
            while (!Thread.currentThread().isInterrupted() || socket.isConnected()) {
                String msg = inputStream.readUTF();
                ChatMessage message = ChatMessage.unmarshall(msg);
                message.setFrom(this.currentUsername);
                switch (message.getMessageType()) {
                    case PUBLIC:
                        chatServer.sendBroadcastMessage(message);
                        break;
                    case PRIVATE:
                        chatServer.sendPrivateMessage(message);
                        break;
                    case CHANGE_USERNAME:
                        System.out.printf("Got change un f: %s n %s", this.currentUsername, message.getBody());
                        String newName = chatServer.getAuthService().changeUsername(this.currentUsername, message.getBody());
                        ChatMessage response = new ChatMessage();
                        if (newName == null && newName.isEmpty()) {
                            response.setMessageType(MessageType.ERROR);
                            response.setBody("Something went wrong!");
                        } else {
                            response.setMessageType(MessageType.CHANGE_USERNAME_CONFIRM);
                            response.setBody(newName);
                        }
                        sendMessage(response);
                        break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeHandler();
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            outputStream.writeUTF(message.marshall());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentName() {
        return this.currentUsername;
    }

    private void authenticate() {

        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        if (currentUsername == null) {
                            ChatMessage response = new ChatMessage();
                            response.setMessageType(MessageType.ERROR);
                            response.setBody("Превышено время входа!\nПопробуйте войти заново.");
                            sendMessage(response);
                            Thread.sleep(50);
                            socket.close();
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    e.getStackTrace();
                }
            }
        }, AUTH_TIMEOUT);

        try {
            while (true) {
                String authMessage = inputStream.readUTF();
                System.out.println("Auth received");
                ChatMessage msg = ChatMessage.unmarshall(authMessage);
                String username = chatServer.getAuthService().getUsernameByLoginAndPassword(msg.getLogin(), msg.getPassword());
                ChatMessage response = new ChatMessage();

                if (username.isEmpty()) {
                    response.setMessageType(MessageType.ERROR);
                    response.setBody("Не правильный логин или пароль!");
                    System.out.println("Ошибка авторизации");
                } else if (chatServer.isUserOnline(username)) {
                    response.setMessageType(MessageType.ERROR);
                    response.setBody("Double auth!");
                    System.out.println("Double auth!");
                } else {
                    response.setMessageType(MessageType.AUTH_CONFIRM);
                    response.setBody(username);
                    currentUsername = username;
                    chatServer.subscribe(this);
                    System.out.println("Subscribed");
                    sendMessage(response);
                    break;
                }
                sendMessage(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeHandler() {
        try {
            chatServer.unsubscribe(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
}
