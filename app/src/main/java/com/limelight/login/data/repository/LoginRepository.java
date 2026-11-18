package com.limelight.login.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.limelight.login.data.api.ApiService;
import com.limelight.login.data.api.RetrofitClient;
import com.limelight.login.model.GoogleLoginRequest;
import com.limelight.login.model.LoginRequest;
import com.limelight.login.model.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginRepository {

    private final ApiService apiService;

    public LoginRepository(){
        this.apiService = RetrofitClient.getInstance().create(ApiService.class);
    }


    public LiveData<LoginResponse> login(String email, String password, String googleAuthCode)
    {
        MutableLiveData<LoginResponse> data = new MutableLiveData<>();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        loginRequest.setGoogleAuthCode(googleAuthCode);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if(response.isSuccessful())
                {
                    data.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                data.postValue(null);
            }
        });

        return data;
    }

    public LiveData<LoginResponse> loginWithGoogle(String email, String username, String googleAuthCode, String avatarUrl) {
        MutableLiveData<LoginResponse> data = new MutableLiveData<>();

        // Enviando uma string vazia "" ou "postmessage" conforme o padrão do Android Identity Services
        GoogleLoginRequest googleLoginRequest = new GoogleLoginRequest(email, username, googleAuthCode, "", avatarUrl);

        apiService.loginWithGoogle(googleLoginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if(response.isSuccessful()) {
                    data.postValue(response.body());
                } else {
                    data.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                data.postValue(null);
            }
        });

        return data;
    }

}
