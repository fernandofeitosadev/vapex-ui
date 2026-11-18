package com.limelight.home.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.limelight.home.data.repository.CreateSessionCreateRepository;
import com.limelight.home.data.repository.HomeRepository;
import com.limelight.home.model.CreateSessionResponse;
import com.limelight.home.model.HomeResponse;


public class HomeViewModel extends ViewModel {

    private final HomeRepository homeRepository;
    private final CreateSessionCreateRepository createSessionCreateRepository;

    public HomeViewModel() {
        this.homeRepository = new HomeRepository();
        this.createSessionCreateRepository = new CreateSessionCreateRepository();
    }

    public LiveData<HomeResponse> getHome(String token) {
        return homeRepository.getHome(token);
    }

    public LiveData<CreateSessionResponse> createSession(String game_id, String token, String computer_uuid){
        return createSessionCreateRepository.createSession(game_id, token, computer_uuid);
    }
}