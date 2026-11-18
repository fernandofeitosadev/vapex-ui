package com.limelight.wallet.data.api;

import com.limelight.wallet.model.PixRequest;
import com.limelight.wallet.model.PixResponse;
import com.limelight.wallet.model.WalletResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    @GET("/wallet")
    Call<WalletResponse> getWallet(@Header("Authorization") String token);

    @POST("/api/payments/create-preference")
    Call<PixResponse> createPix(
            @Header("Authorization") String token,
            @Header("X-Public-Key") String publicKey,
            @Body PixRequest request
    );
}
