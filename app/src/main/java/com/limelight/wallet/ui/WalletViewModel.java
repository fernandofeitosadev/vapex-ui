package com.limelight.wallet.ui;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.limelight.wallet.data.repository.WalletRepository;
import com.limelight.wallet.model.PixResponse;
import com.limelight.wallet.model.WalletResponse;

public class WalletViewModel extends ViewModel {

    private final WalletRepository walletRepository;

    public WalletViewModel(){
        this.walletRepository = new WalletRepository();
    }


    public LiveData<WalletResponse> getWallet(String token){
        return this.walletRepository.getWallet(token);
    }

    public LiveData<PixResponse> createPix(String token, double amount) {
        return this.walletRepository.createPix(token, amount);
    }

}
