package com.limelight.login.data.api;

import com.limelight.login.model.GoogleLoginRequest;
import com.limelight.login.model.LoginRequest;
import com.limelight.login.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/api/auth/signin")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("/api/auth/google")
    Call<LoginResponse> loginWithGoogle(@Body GoogleLoginRequest googleLoginRequest);
}
