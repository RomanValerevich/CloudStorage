package ru.netology.cloudstorage.service;

public interface AuthService {
    String login(String login, String password);
    void logout(String authToken);
    String validateTokenAndGetUsername(String authToken);
    void register(String username, String password, String email);
}
