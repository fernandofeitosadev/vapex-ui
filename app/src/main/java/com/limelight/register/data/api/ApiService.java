package com.limelight.register.data.api;

import com.limelight.register.model.SignUpRequest;
import com.limelight.register.model.SignUpResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/api/auth/signup")
    Call<SignUpResponse> register(@Body SignUpRequest signUpRequest);

}
