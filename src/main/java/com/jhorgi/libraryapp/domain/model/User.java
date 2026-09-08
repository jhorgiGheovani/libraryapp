package com.jhorgi.libraryapp.domain.model;

public class User {

    private final Long id;
    private final String username;
    private final String email;
    private final String hashedPassword;
    private final Role role;

    public User(Long id, String username, String email, String hashedPassword, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.role = role;
    }

    public static User newUser(String username, String email, String hashedPassword, Role role) {
        return new User(null, username, email, hashedPassword, role);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public Role getRole() {
        return role;
    }
}
