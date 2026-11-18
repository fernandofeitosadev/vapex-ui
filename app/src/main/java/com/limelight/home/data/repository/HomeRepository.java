package com.limelight.home.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.limelight.home.data.api.ApiService;
import com.limelight.home.data.api.RetrofitClient;
import com.limelight.home.model.HomeResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRepository {

    private final ApiService apiService;


    public HomeRepository() {
        apiService = RetrofitClient.getInstance().create(ApiService.class);
    }


    public LiveData<HomeResponse> getHome(String token){
        MutableLiveData<HomeResponse> data = new MutableLiveData<>();

        apiService.getHome("Bearer " + token).enqueue(new Callback<HomeResponse>() {
            @Override
            public void onResponse(Call<HomeResponse> call, Response<HomeResponse> response) {

                Log.d("HOME", "Response = " + response);
                if(response.isSuccessful()) data.postValue(response.body());
            }

            @Override
            public void onFailure(Call<HomeResponse> call, Throwable throwable) {
                Log.d("HOME", "onFailure = " + throwable.getMessage());
                data.postValue(null);
            }
        });

        return data;
    }
}
