package ru.kosenko.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetworkService {
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final ChatMessageService messageService;

    public NetworkService(String host, int port, ChatMessageService messageService) throws IOException {
        this.socket = new Socket(host, port);
        this.inputStream = new DataInputStream(this.socket.getInputStream());
        this.outputStream = new DataOutputStream(this.socket.getOutputStream());
        this.messageService = messageService;
    }

    public void readMessages() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()){
                try {
                    String in = inputStream.readUTF();
                    messageService.receive(in);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void sendMessage(String msg) {
        try {
            outputStream.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return socket;
    }
}
