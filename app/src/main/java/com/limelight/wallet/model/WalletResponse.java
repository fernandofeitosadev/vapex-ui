package com.limelight.wallet.model;

public class WalletResponse {

    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {

        private String id;
        private String user_id;
        private String balance;
        private String status;
        private String created_at;
        private String updated_at;
        private String avatar;

        public String getId() {
            return id;
        }

        public String getUser_id() {
            return user_id;
        }

        public String getBalance() {
            return balance;
        }

        public String getStatus() {
            return status;
        }

        public String getAvatar() {
            return avatar;
        }

        public String getCreated_at() {
            return created_at;
        }

        public String getUpdated_at() {
            return updated_at;
        }
    }
}