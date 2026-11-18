package com.limelight.home.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.limelight.home.data.api.ApiService;
import com.limelight.home.data.api.RetrofitClient;
import com.limelight.home.model.CreateSessionRequest;
import com.limelight.home.model.CreateSessionResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSessionCreateRepository {

    private final ApiService apiService;
    private final Gson gson;

    public CreateSessionCreateRepository() {
        this.apiService = RetrofitClient.getInstance().create(ApiService.class);
        this.gson = new Gson();
    }


    public LiveData<CreateSessionResponse> createSession(String game_id, String token, String computer_uuid){
        MutableLiveData<CreateSessionResponse> data = new MutableLiveData<>();

        CreateSessionRequest createSessionRequest = new CreateSessionRequest();
        
        try {
            createSessionRequest.setGameId(Integer.parseInt(game_id));
        } catch (NumberFormatException e) {
            Log.e("CreateSessionRepo", "Invalid game_id: " + game_id);
            createSessionRequest.setGameId(0);
        }
        
        createSessionRequest.setComputerUuid(computer_uuid);

        apiService.createSession(createSessionRequest, "Bearer " + token).enqueue(new Callback<CreateSessionResponse>() {
            @Override
            public void onResponse(Call<CreateSessionResponse> call, Response<CreateSessionResponse> response) {
                if (response.isSuccessful()) {
                    data.postValue(response.body());
                } else {
                    CreateSessionResponse errorResponse = null;
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("CreateSessionRepo", "Error response: " + errorBody);
                            errorResponse = gson.fromJson(errorBody, CreateSessionResponse.class);
                        }
                    } catch (Exception e) {
                        Log.e("CreateSessionRepo", "Error parsing error body", e);
                    }
                    data.postValue(errorResponse);
                }
            }

            @Override
            public void onFailure(Call<CreateSessionResponse> call, Throwable t) {
                Log.e("CreateSessionRepo", "API call failed", t);
                data.postValue(null);
            }
        });

        return data;
    }
}
