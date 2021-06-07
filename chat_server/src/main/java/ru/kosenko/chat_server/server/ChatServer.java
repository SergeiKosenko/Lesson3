package ru.kosenko.chat_server.server;

import ru.kosenko.chat_server.ClientHandler;
import ru.kosenko.chat_server.auth.DatabaseAuthService;
import ru.kosenko.common.ChatMessage;
import ru.kosenko.common.MessageType;
import ru.kosenko.chat_server.auth.AuthService;
import ru.kosenko.chat_server.auth.PrimitiveInMemoryAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private static final int PORT = 8090;
    private List<ru.kosenko.chat_server.ClientHandler> listOnlineUsers;
    private AuthService authService;

    public ChatServer() {
        this.listOnlineUsers = new ArrayList<>();
        this.authService = new DatabaseAuthService();
    }

    public void start() {
        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started");
            authService.start();

            while (true) {
                System.out.println("Waiting for connection");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                new ru.kosenko.chat_server.ClientHandler(socket, this).handle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    private synchronized void sendListOnlineUsers() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageType(MessageType.CLIENT_LIST);
        msg.setOnlineUsers(new ArrayList<>());
        for (ru.kosenko.chat_server.ClientHandler user : listOnlineUsers) {
            msg.getOnlineUsers().add(user.getCurrentName());
        }
        for (ru.kosenko.chat_server.ClientHandler user : listOnlineUsers) {
            user.sendMessage(msg);
        }
    }

    public synchronized void sendBroadcastMessage(ChatMessage message) {
        for (ru.kosenko.chat_server.ClientHandler user : listOnlineUsers) {
            user.sendMessage(message);
        }
    }

    public synchronized void sendPrivateMessage(ChatMessage message) {
        for (ru.kosenko.chat_server.ClientHandler user : listOnlineUsers) {
           if(user.getCurrentName().equals(message.getTo())) user.sendMessage(message);
        }
    }

    public synchronized boolean isUserOnline(String username) {
        for (ru.kosenko.chat_server.ClientHandler user : listOnlineUsers) {
            if (user.getCurrentName().equals(username)) return true;
        }
        return false;
    }

    public synchronized void subscribe(ru.kosenko.chat_server.ClientHandler clientHandler) {
        listOnlineUsers.add(clientHandler);
        sendListOnlineUsers();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        listOnlineUsers.remove(clientHandler);
        sendListOnlineUsers();
    }

    public AuthService getAuthService() {
        return authService;
    }


}
