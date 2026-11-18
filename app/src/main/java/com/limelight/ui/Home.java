package com.limelight.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.limelight.PcView;
import com.limelight.R;
import com.limelight.SessionManager;
import com.limelight.SocketManager;
import com.limelight.autoupdate.AutoUpdate;
import com.limelight.home.ui.HomeFragment;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.preferences.StreamSettings;
import com.limelight.sessoes.ui.SessoesFragment;
import com.limelight.utils.UiHelper;
import com.limelight.wallet.ui.WalletFragment;

public class Home extends AppCompatActivity{

    private BottomNavigationView bottomNav;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // ❌ REMOVIDO EdgeToEdge (conflita com fullscreen)
        setContentView(R.layout.activity_home);




       // startActivity(new Intent(this, PcView.class));

        // Inicializa o SocketManager automaticamente se o usuário já possuir um token
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            SocketManager.getInstance(this).connect();
        }

        bottomNav = findViewById(R.id.bottom_navigation);


        UiHelper.setLocale(this);

        // Chama a atualização automática de forma independente do login
        new AutoUpdate(this).check();


        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                Fragment fragment = null;

                if (item.getItemId() == R.id.home) {
                    fragment = new HomeFragment();
                } else if (item.getItemId() == R.id.wallet) {
                    fragment = new WalletFragment();
                } else if (item.getItemId() == R.id.cupoms) {
                    fragment = new CupomsFragment();
                } else if (item.getItemId() == R.id.sessoes) {
                    fragment = new SessoesFragment();
                } else if (item.getItemId() == R.id.perfil) {
                    fragment = new PerfilFragment();
                }

                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .commit();
                    return true;
                }

                return false;
            }
        });


        SocketManager.getInstance(this).addListener(new SocketManager.SocketEventListener() {
            @Override
            public void onEvent(String event, Object... args) {
                if (event.equals("session:created")) {

                }

                if (event.equals("session:ended")) {

                }
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {}
        });







        hideSystemUI();
    }





    // 🔥 FULLSCREEN IMMERSIVE MODE
    private void hideSystemUI() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(
                        WindowInsets.Type.statusBars() |
                                WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            View decorView = getWindow().getDecorView();

            int flags =
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

            decorView.setSystemUiVisibility(flags);
        }
    }

    // 🔄 garante que não volta barra quando muda foco
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            hideSystemUI();
        }
    }

}