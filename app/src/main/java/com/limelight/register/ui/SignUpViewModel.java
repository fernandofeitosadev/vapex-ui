package com.limelight.register.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.limelight.register.data.repository.SignUpRepository;
import com.limelight.register.model.SignUpResponse;

public class SignUpViewModel extends ViewModel {

    private final SignUpRepository repository =
            new SignUpRepository();

    public LiveData<SignUpResponse> register(
            String username,
            String email,
            String password,
            String googleAuthCode) {

        return repository.register(
                username,
                email,
                password,
                googleAuthCode
        );
    }

    public LiveData<com.limelight.login.model.LoginResponse> loginWithGoogle(String email, String username, String googleAuthCode, String avatarUrl) {
        return new com.limelight.login.data.repository.LoginRepository().loginWithGoogle(email, username, googleAuthCode, avatarUrl);
    }
}
