package com.limelight.home.ui.model;

public class GameModel {
    private int id;
    private String title;
    private String subtitle;
    private String image;
    private String badge;

    public GameModel(int id, String title, String subtitle, String image) {
        this(id, title, subtitle, image, null);
    }

    public GameModel(int id, String title, String subtitle, String image, String badge) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.image = image;
        this.badge = badge;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImage() {
        return image;
    }

    public String getBadge() {
        return badge;
    }
}
