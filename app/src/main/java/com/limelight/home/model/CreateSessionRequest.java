package com.limelight.home.model;

import com.google.gson.annotations.SerializedName;

public class CreateSessionRequest {

    @SerializedName("game_id")
    private int gameId;

    @SerializedName("computer_uuid")
    private String computerUuid;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getComputerUuid() {
        return computerUuid;
    }

    public void setComputerUuid(String computerUuid) {
        this.computerUuid = computerUuid;
    }
}
