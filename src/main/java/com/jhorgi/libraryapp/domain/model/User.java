package com.jhorgi.libraryapp.domain.model;

public class User {

    private final Long id;
    private final String fullname;
    private final String username;
    private final String email;
    private final String hashedPassword;
    private final Role role;

    public User(Long id, String fullname, String username, String email, String hashedPassword, Role role) {
        this.id = id;
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.role = role;
    }

    public static User newUser(String fullname, String username, String email, String hashedPassword, Role role) {
        return new User(null, fullname, username, email, hashedPassword, role);
    }

    /**
     * Profile edit. Keeps the password and the role, so a profile update can
     * never quietly escalate privileges or reset a credential.
     */
    public User withProfile(String fullname, String username, String email) {
        return new User(id, fullname, username, email, hashedPassword, role);
    }

    public User withHashedPassword(String newHashedPassword) {
        return new User(id, fullname, username, email, newHashedPassword, role);
    }

    /** Role change, and nothing else: the one path that alters privileges. */
    public User withRole(Role newRole) {
        return new User(id, fullname, username, email, hashedPassword, newRole);
    }

    public boolean hasId(Long otherId) {
        return id != null && id.equals(otherId);
    }

    public Long getId() {
        return id;
    }

    public String getFullname() {
        return fullname;
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
