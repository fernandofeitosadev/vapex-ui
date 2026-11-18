package com.limelight.login.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.limelight.login.data.repository.LoginRepository;
import com.limelight.login.model.LoginResponse;

public class LoginViewModel extends ViewModel {

    private LoginRepository loginRepository;

    public LoginViewModel()
    {
        this.loginRepository = new LoginRepository();
    }


    public LiveData<LoginResponse> login(String email, String password, String googleAuthCode)
    {
        return this.loginRepository.login(email, password, googleAuthCode);
    }

    public LiveData<LoginResponse> loginWithGoogle(String email, String username, String googleAuthCode, String avatarUrl) {
        return this.loginRepository.loginWithGoogle(email, username, googleAuthCode, avatarUrl);
    }
}
