package com.limelight.home.data.api;


import com.limelight.home.model.CreateSessionRequest;
import com.limelight.home.model.CreateSessionRequest;
import com.limelight.home.model.CreateSessionResponse;
import com.limelight.home.model.HomeResponse;
import com.limelight.home.model.PairSunshineResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface ApiService {
    @GET("/")
    Call<HomeResponse> getHome(@Header("Authorization") String token);

    @POST("/api/sessions/create")
    Call<CreateSessionResponse> createSession(@Body CreateSessionRequest createSessionRequest, @Header("Authorization") String token);

    @POST("/api/sunschine/parear")
    Call<PairSunshineResponse> pairSunshine(@Query("pincode") String pincode, @Header("Authorization") String token);
}
