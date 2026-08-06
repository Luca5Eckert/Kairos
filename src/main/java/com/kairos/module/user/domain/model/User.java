package com.kairos.module.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;

    private String name;

    private String username;

    private String email;

    private String hashPassword;

    private Role role;

    private boolean emailConfirmed;

    private String confirmationCodeHash;

    private Instant createdAt;

    protected User(UUID id, String name, String username, String email, String hashPassword, Role role, boolean emailConfirmed, String confirmationCodeHash, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.hashPassword = hashPassword;
        this.role = role;
        this.emailConfirmed = emailConfirmed;
        this.confirmationCodeHash = confirmationCodeHash;
        this.createdAt = createdAt;
    }

    public User(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.username = builder.username;
        this.email = builder.email;
        this.hashPassword = builder.hashPassword;
        this.role = builder.role;
        this.emailConfirmed = builder.emailConfirmed;
        this.confirmationCodeHash = builder.confirmationCodeHash;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEmailConfirmed() {
        return emailConfirmed;
    }

    public String getConfirmationCodeHash() {
        return confirmationCodeHash;
    }

    public void changePassword(String hashPassword) {
        this.hashPassword = hashPassword;
    }

    public void updateProfile(String name, String username) {
        if(name != null) {
            this.name = name;
        }
        if(username != null) {
            this.username = username;
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private UUID id;
        private String name;
        private String username;
        private String email;
        private String hashPassword;
        private Role role;
        private boolean emailConfirmed;
        private String confirmationCodeHash;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder hashPassword(String hashPassword) {
            this.hashPassword = hashPassword;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder emailConfirmed(boolean emailConfirmed) {
            this.emailConfirmed = emailConfirmed;
            return this;
        }

        public Builder confirmationCodeHash(String confirmationCodeHash) {
            this.confirmationCodeHash = confirmationCodeHash;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }


}
