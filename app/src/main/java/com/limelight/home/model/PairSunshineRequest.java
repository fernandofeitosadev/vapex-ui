package com.limelight.home.model;

public class PairSunshineRequest {
    private String pin;
    private String pcname;

    public PairSunshineRequest(String pin, String pcname) {
        this.pin = pin;
        this.pcname = pcname;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getPcname() {
        return pcname;
    }

    public void setPcname(String pcname) {
        this.pcname = pcname;
    }
}
