package ru.kosenko.chat_server.auth;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveInMemoryAuthService implements AuthService {

    private List<User> users;

    public PrimitiveInMemoryAuthService() {
        this.users = List.of(new User("user1", "log1", "pass"),
                new User("user2", "log2", "pass"),
                new User("user3", "log3", "pass")
        );

    }

    @Override
    public void start() {
        System.out.println("Auth service started");
    }

    @Override
    public void stop() {
        System.out.println("Auth service stopped");
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) return user.getUsername();
        }
        return null;
    }

    @Override
    public String changeUsername(String oldName, String newName) {
        return null;
    }

    @Override
    public String changePassword(String username, String oldPassword, String newPassword) {
        return null;
    }
}
