package com.kairos.user.domain.model;

public class User {

    private final Long id;

    private String name;

    private String username;

    private String email;

    private String hashPassword;

    private Role role;

    private boolean emailConfirmed;

    private String confirmationCodeHash;

    protected User(Long id, String name, String username, String email, String hashPassword, Role role, boolean emailConfirmed, String confirmationCodeHash) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.hashPassword = hashPassword;
        this.role = role;
        this.emailConfirmed = emailConfirmed;
        this.confirmationCodeHash = confirmationCodeHash;
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
    }

    public Long getId() {
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

    public static class Builder {
        private Long id;
        private String name;
        private String username;
        private String email;
        private String hashPassword;
        private Role role;
        private boolean emailConfirmed;
        private String confirmationCodeHash;

        public Builder id(Long id) {
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

        public User build() {
            return new User(id, name, username, email, hashPassword, role, emailConfirmed, confirmationCodeHash);
        }
    }


}
