package ru.kosenko.network;

public interface ChatMessageService {
    void send(String msg);
    void receive(String msg);
    void connect();
    boolean isConnected();
}
