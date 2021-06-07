package ru.kosenko.chat_server;

import ru.kosenko.chat_server.server.ChatServer;

public class ServerApp {
    public static void main(String[] args) {
        new ChatServer().start();
    }
}
