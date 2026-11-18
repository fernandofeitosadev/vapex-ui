package com.limelight.home.ui.model;

public class BannerModel {
    private String title;
    private String description;
    private String badge;
    private int imageRes;
    private String imageUrl;

    public BannerModel(String title, String description, String badge, int imageRes) {
        this.title = title;
        this.description = description;
        this.badge = badge;
        this.imageRes = imageRes;
    }

    public BannerModel(String title, String description, String badge, String imageUrl) {
        this.title = title;
        this.description = description;
        this.badge = badge;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getBadge() { return badge; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
}