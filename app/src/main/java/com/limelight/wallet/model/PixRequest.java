package com.limelight.wallet.model;

import com.google.gson.annotations.SerializedName;

public class PixRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("unit_price")
    private double unitPrice;

    @SerializedName("public_key")
    private String publicKey;

    public PixRequest(String title, double unitPrice, String publicKey) {
        this.title = title;
        this.unitPrice = unitPrice;
        this.publicKey = publicKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
