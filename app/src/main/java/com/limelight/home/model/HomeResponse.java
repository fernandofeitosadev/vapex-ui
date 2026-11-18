package com.limelight.home.model;

import java.util.List;

public class HomeResponse {

    private boolean success;
    private List<User> data;
    private int latestVersionCode;
    private String apkUrl;

    public boolean isSuccess() {
        return success;
    }

    public List<User> getData() {
        return data;
    }

    public int getLatestVersionCode() {
        return latestVersionCode;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public static class User {

        private String id;
        private String username;
        private String avatar;
        private Wallet wallet;

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatar() {
            return avatar;
        }

        public Wallet getWallet() {
            return wallet;
        }
    }

    public static class Wallet {

        private int id;
        private double  balance;
        private int user_id;

        public int getId() {
            return id;
        }

        public double  getBalance() {
            return balance;
        }

        public int getUser_id() {
            return user_id;
        }
    }
}