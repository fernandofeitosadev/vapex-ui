package com.limelight.wallet.model;

import com.google.gson.annotations.SerializedName;

public class PixResponse {
    @SerializedName("qrCode")
    private String qrCode;

    @SerializedName("qrCodeBase64")
    private String qrCodeBase64;

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }
}
