package com.limelight.register.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.limelight.register.data.api.ApiService;
import com.limelight.register.data.api.RetrofitClient;
import com.limelight.register.model.SignUpRequest;
import com.limelight.register.model.SignUpResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpRepository {


    private final ApiService apiService;


    public SignUpRepository() {
        this.apiService = RetrofitClient.getInstance().create(ApiService.class);
    }


    public LiveData<SignUpResponse> register(String username, String email, String password, String googleAuthCode){
        MutableLiveData<SignUpResponse> data = new MutableLiveData<>();

        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername(username);
        signUpRequest.setEmail(email);
        signUpRequest.setPassword(password);
        signUpRequest.setGoogleAuthCode(googleAuthCode);

        apiService.register(signUpRequest).enqueue(new Callback<SignUpResponse>() {
            @Override
            public void onResponse(Call<SignUpResponse> call, Response<SignUpResponse> response) {
                if(response.isSuccessful()){
                    data.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<SignUpResponse> call, Throwable t) {
                data.postValue(null);
            }
        });

        return data;
    }

}
