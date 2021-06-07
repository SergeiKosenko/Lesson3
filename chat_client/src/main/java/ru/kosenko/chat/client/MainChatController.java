package ru.kosenko.chat.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.kosenko.common.ChatMessage;
import ru.kosenko.common.MessageType;
import ru.kosenko.network.ChatMessageService;
import ru.kosenko.network.ChatMessageServiceImpl;
import ru.kosenko.network.MessageProcessor;

import java.awt.*;
import java.awt.Dialog;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainChatController implements Initializable, MessageProcessor {


    private static final String PUBLIC = "Онлайн:";
    public TextArea chatArea;
    public ListView onlineUsers;
    public TextField inputField;
    public Button btnSendMessage;
    public TextField loginField;
    public PasswordField passwordField;
    public Button btnSendAuth;
    private ChatMessageService messageService;
    private String currentName;
    public GridPane changePassPane;
    public GridPane changeNickPane;
    public PasswordField changeNickPass;
    public VBox chatPane;
    public TextField changeNickNewNick;
    public PasswordField oldPass;
    public PasswordField newPass;
    public PasswordField confirmNewPass;
    public GridPane loginPane;
    private HistoryMaker historyMaker;

    public void mockAction(ActionEvent actionEvent) {
        try {
            throw new RuntimeException("Проверка RuntimeException!!!");
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    public void exit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void showHelp(ActionEvent actionEvent) throws URISyntaxException, IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(new URI("https://docs.google.com/document/d/1wr0YEtIc5yZtKFu-KITqYnBtp8KC28v2FEYUANL0YAM/edit?usp=sharing"));
    }

    public void sendMessage(ActionEvent actionEvent) {
        String text = inputField.getText();
        if (text.isEmpty()) return;
        ChatMessage msg = new ChatMessage();
        String abonent = (String) this.onlineUsers.getSelectionModel().getSelectedItem();
        if(abonent.equals(PUBLIC)) msg.setMessageType(MessageType.PUBLIC);
        else {
            msg.setMessageType(MessageType.PRIVATE);
            msg.setTo(abonent);
        }
        msg.setFrom(currentName);
        msg.setBody(text);
        messageService.send(msg.marshall());
        chatArea.appendText(String.format("Я: %s\n", text));
        historyMaker.writeHistory(String.format("Я: %s\n", text));
        inputField.clear();
    }

    private void appendToFormChat(ChatMessage msg) {
        if(msg.getFrom().equals(this.currentName)) return;
        String modifier = msg.getMessageType().equals(MessageType.PUBLIC) ? "[Всем]" : "[Лично]";
        String text = String.format("%s [%s] %s\n", modifier, msg.getFrom(), msg.getBody());
        chatArea.appendText(text);
        historyMaker.writeHistory(text);
    }

    private void showError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong!");
        alert.setHeaderText(e.getMessage());
        VBox dialog = new VBox();
        Label label = new Label("Trace:");
        TextArea textArea = new TextArea();
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement el : e.getStackTrace()) {
            builder.append(el).append(System.lineSeparator());
        }
        textArea.setText(builder.toString());
        dialog.getChildren().addAll(label, textArea);
        alert.getDialogPane().setContent(dialog);
        alert.showAndWait();
    }

    private void showError(ChatMessage msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong!");
        alert.setHeaderText(msg.getMessageType().toString());
        VBox dialog = new VBox();
        Label label = new Label("ERROR:");
        TextArea textArea = new TextArea();
        textArea.setText(msg.getBody());
        dialog.getChildren().addAll(label, textArea);
        alert.getDialogPane().setContent(dialog);
        alert.showAndWait();
    }

    public void showAbout(ActionEvent event) {
        Label secondLabel = new Label("Сетевой чат\nJava 2\ngb.ru\nМай 2021");
        StackPane secondaryLayout = new StackPane();
        secondaryLayout.getChildren().add(secondLabel);
        Scene aboutScene = new Scene(secondaryLayout, 230, 100);
        Stage aboutWindow = new Stage();
        aboutWindow.setTitle("About");
        aboutWindow.setScene(aboutScene);
        Stage aboutStage = new Stage();
        aboutWindow.setX(aboutStage.getX() + 200);
        aboutWindow.setY(aboutStage.getY() + 100);
        aboutWindow.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.messageService = new ChatMessageServiceImpl("localhost", 8090, this);

    }

    @Override
    public void processMessage(String msg) {
        Platform.runLater(() -> {
                    ChatMessage message = ChatMessage.unmarshall(msg);
                    System.out.println("Received message");

                    switch (message.getMessageType()) {
                        case PRIVATE:
                        case PUBLIC:
                            appendToFormChat(message);
                            break;
                        case CLIENT_LIST:
                            refreshOnlineUsers(message);
                            break;
                        case AUTH_CONFIRM: {
                            this.currentName = message.getBody();
                            App.stage1.setTitle(currentName);
                            loginPane.setVisible(false);
                            chatPane.setVisible(true);
                            this.historyMaker = new HistoryMaker(message.getBody());
                            List<String> history = historyMaker.readHistory();
                            for (String s : history) {
                                chatArea.appendText(s + System.lineSeparator());
                            }
                            break;
                        }
                        case CHANGE_USERNAME_CONFIRM:

                            changeNickPane.setVisible(false);
                            chatPane.setVisible(true);
                            currentName = message.getBody();
                            App.stage1.setTitle(currentName);
                            break;
                        case ERROR:
                            showError(message);
                            break;
                    }
                }
        );
    }


    private void refreshOnlineUsers(ChatMessage message) {
        message.getOnlineUsers().add(0, PUBLIC);
        this.onlineUsers.setItems(FXCollections.observableArrayList(message.getOnlineUsers()));
        this.onlineUsers.getSelectionModel().selectFirst();
    }

    public void sendAuth(ActionEvent actionEvent) {

        try {
            if(!messageService.isConnected()) messageService.connect();
        } catch (Exception e) {
            showError(e);
        }
        String log = loginField.getText();
        String pass = passwordField.getText();
        if (log.isEmpty() || pass.isEmpty()) return;
        ChatMessage msg = new ChatMessage();
        msg.setMessageType(MessageType.SEND_AUTH);
        msg.setLogin(log);
        msg.setPassword(pass);
        messageService.send(msg.marshall());
    }

    public void pressChangeNick(ActionEvent event) {
        chatPane.setVisible(false);
        changeNickPane.setVisible(true);

    }

    public void pressChangePassword(ActionEvent event) {
        chatPane.setVisible(false);
        changePassPane.setVisible(true);
    }

    public void sendChangeUsername(ActionEvent event) {
        ChatMessage message = new ChatMessage();
        message.setMessageType(MessageType.CHANGE_USERNAME);
        message.setBody(changeNickNewNick.getText());
        message.setFrom(this.currentName);
        message.setPassword(changeNickPass.getText());

        messageService.send(message.marshall());
    }

    public void sendChangePass(ActionEvent event) {
        String password = oldPass.getText();
        String newPassword = newPass.getText();
        String confirmPass = confirmNewPass.getText();

        if (newPassword.equals(confirmPass)) {
            ChatMessage message = new ChatMessage();
            message.setMessageType(MessageType.CHANGE_PASSWORD);
            message.setPassword(password);
            message.setFrom(this.currentName);
            messageService.send(message.marshall());
        } else {
            oldPass.clear();
            newPass.clear();
            confirmNewPass.clear();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Changing your password is failed");
            alert.setContentText("Entered passwords are not equal");
            alert.showAndWait();
        }
    }

    public void pressBack(ActionEvent event) {
        changePassPane.setVisible(false);
        changeNickPane.setVisible(false);
        chatPane.setVisible(true);
    }
}
