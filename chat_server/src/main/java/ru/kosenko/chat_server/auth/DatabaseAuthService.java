package ru.kosenko.chat_server.auth;

import ru.kosenko.chat_server.db.ClientsDatabaseService;

import java.sql.SQLException;


public class DatabaseAuthService implements AuthService {
    private ClientsDatabaseService dbService;

    @Override
    public void start() {
        dbService = ClientsDatabaseService.getInstance();
    }

    @Override
    public void stop() {
        dbService.closeConnection();
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String pass) {
        return dbService.getClientNameByLoginPass(login, pass);
    }

    @Override
    public String changeUsername(String oldName, String newName) {
        try {
            return dbService.changeUsername(oldName, newName);
        } catch (SQLException e) {
            throw new RuntimeException("Username change unsuccessful");
        }
    }

    @Override
    public String changePassword(String username, String oldPassword, String newPassword) {
        return null;
    }
}

