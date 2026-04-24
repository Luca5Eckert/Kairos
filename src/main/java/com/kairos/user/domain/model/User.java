package com.kairos.user.domain.model;

public class User {

    private final Long id;

    private String name;

    private String username;

    private String email;

    private String hashPassword;

    private Role role;

    protected User(Long id, String name, String username, String email, String hashPassword, Role role) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.hashPassword = hashPassword;
        this.role = role;
    }

    public User(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.username = builder.username;
        this.email = builder.email;
        this.hashPassword = builder.hashPassword;
    }


    public static class Builder {
        private Long id;
        private String name;
        private String username;
        private String email;
        private String hashPassword;
        private Role role;

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withHashPassword(String hashPassword) {
            this.hashPassword = hashPassword;
            return this;
        }

        public Builder withRole(Role role) {
            this.role = role;
            return this;
        }

        public User build() {
            return new User(id, name, username, email, hashPassword, role);
        }
    }


}
