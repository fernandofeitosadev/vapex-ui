package com.limelight.wallet.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.limelight.wallet.data.api.ApiService;
import com.limelight.wallet.data.api.RetrofitClient;
import com.limelight.wallet.model.PixRequest;
import com.limelight.wallet.model.PixResponse;
import com.limelight.wallet.model.WalletResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {

    private final ApiService apiService;


    public WalletRepository() {
        apiService = RetrofitClient.getInstance().create(ApiService.class);
    }


    public LiveData<WalletResponse> getWallet(String token){
        MutableLiveData<WalletResponse> data = new MutableLiveData<>();

        apiService.getWallet("Bearer " + token).enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(Call<WalletResponse> call, Response<WalletResponse> response) {

                Log.d("Wallet", "Response = " + response);
                if(response.isSuccessful()) data.postValue(response.body());
            }

            @Override
            public void onFailure(Call<WalletResponse> call, Throwable throwable) {
                Log.d("Wallet", "onFailure = " + throwable.getMessage());
                data.postValue(null);
            }
        });

        return data;
    }

    public LiveData<PixResponse> createPix(String token, double amount) {
        MutableLiveData<PixResponse> data = new MutableLiveData<>();
        String publicKey = "TEST-befad5cd-3d63-4851-a2e1-064b628a962a";

        PixRequest request = new PixRequest("Recarga de carteira", amount, publicKey);

        apiService.createPix("Bearer " + token, publicKey, request).enqueue(new Callback<PixResponse>() {
            @Override
            public void onResponse(Call<PixResponse> call, Response<PixResponse> response) {
                if (response.isSuccessful()) {
                    data.postValue(response.body());
                } else {
                    data.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<PixResponse> call, Throwable t) {
                data.postValue(null);
            }
        });

        return data;
    }
}
