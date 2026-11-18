package com.limelight.login.ui;


import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.Scopes;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.limelight.R;
import com.limelight.SocketManager;
import com.limelight.binding.PlatformBinding;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.computers.ComputerManagerService;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.ui.Home;
import com.limelight.utils.Dialog;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.UiHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class Login extends AppCompatActivity {


    private static final Logger log = LoggerFactory.getLogger(Login.class);
    private static final String WEB_CLIENT_ID = "459829365552-7sf5pu5bojrm3k0l39fcggiaedf1l2bf.apps.googleusercontent.com";

    private EditText editTextEmail;
    private EditText editTextSenha;
    private Button buttonEntrar;
    private Button buttonGoogle;
    private ProgressBar loading;
    private LoginViewModel loginViewModel;
    private String serverAuthCode;


    private boolean inForeground;

    private ComputerDetails computer;
    private String uuidString;
    private ComputerManagerService.ComputerManagerBinder managerBinder;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    ((ComputerManagerService.ComputerManagerBinder)binder);

            // Wait in a separate thread to avoid stalling the UI
            new Thread() {
                @Override
                public void run() {
                    // Wait for the binder to be ready
                    localBinder.waitForReady();



                    // Now make the binder visible. We must do this after appGridAdapter
                    // is set to prevent us from reaching updateUiWithServerinfo() and
                    // touching the appGridAdapter prior to initialization.
                    managerBinder = localBinder;



                    // Force a keypair to be generated early to avoid discovery delays
                    new AndroidCryptoProvider(getBaseContext()).getClientCertificate();

                }
            }.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;



        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextSenha = (EditText) findViewById(R.id.editTextSenha);
        buttonEntrar  = (Button) findViewById(R.id.buttonEntrar);
        buttonGoogle  = (Button) findViewById(R.id.buttonGoogle);
        loading       = (ProgressBar) findViewById(R.id.bttLoading);


        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        buttonEntrar.setOnClickListener(this::onClickListenLogin);
        buttonGoogle.setOnClickListener(v -> requestDrivePermission());

        // Bind to the computer manager service
        bindService(new Intent(this, ComputerManagerService.class), serviceConnection,
                Service.BIND_AUTO_CREATE);

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

    public void requestDrivePermission() {
        List<Scope> requestedScopes = Arrays.asList(
                new Scope(Scopes.PROFILE),
                new Scope(Scopes.EMAIL),
                new Scope(Scopes.DRIVE_FILE)
        );

        com.google.android.gms.auth.api.identity.AuthorizationRequest authorizationRequest =
                com.google.android.gms.auth.api.identity.AuthorizationRequest.builder()
                        .setRequestedScopes(requestedScopes)
                        .requestOfflineAccess(WEB_CLIENT_ID)
                        .build();

        com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this)
                .authorize(authorizationRequest)
                .addOnSuccessListener(authorizationResult -> {
                    if (authorizationResult.hasResolution()) {
                        try {
                            startAuthorizationIntent.launch(
                                    new IntentSenderRequest.Builder(
                                            authorizationResult.getPendingIntent().getIntentSender()
                                    ).build()
                            );
                        } catch (Exception e) {
                            Log.e("DRIVE_AUTH", "Erro ao lançar intent", e);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DRIVE_AUTH", "Falha no authorize", e));
    }

    private ActivityResultLauncher<IntentSenderRequest> startAuthorizationIntent =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    activityResult -> {
                        try {
                            com.google.android.gms.auth.api.identity.AuthorizationResult result =
                                    com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this)
                                            .getAuthorizationResultFromIntent(activityResult.getData());

                            String accessToken = result.getAccessToken();
                            serverAuthCode = result.getServerAuthCode();

                            // Login imediato com Google
                            loginWithGoogle(accessToken, serverAuthCode);

                        } catch (com.google.android.gms.common.api.ApiException e) {
                            Log.e("DRIVE_AUTH", "Erro: " + e.getStatusCode());
                            // Limpar estado em caso de erro na autorização
                            Identity.getSignInClient(this).signOut();
                            Toast.makeText(this, "Erro na autorização: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                        }
                    });

    private void loginWithGoogle(String accessToken, String code) {
        loading.setVisibility(View.VISIBLE);
        buttonGoogle.setEnabled(false);
        buttonEntrar.setEnabled(false);

        // Otimização: Não buscamos mais e-mail/nome no Android para evitar Erro 401.
        // O seu backend Node.js agora é responsável por extrair esses dados do 'code' (googleAuthCode).
        loginViewModel.loginWithGoogle(null, null, code, null).observe(Login.this, loginResponse -> {
            if (loginResponse != null) {
                loading.setVisibility(View.GONE);
                buttonGoogle.setEnabled(true);
                buttonEntrar.setEnabled(true);
                handleSuccessfulLogin(loginResponse);
            } else {
                // ERRO NO SERVIDOR: Limpar sessão do Google para permitir nova tentativa
                Identity.getSignInClient(this).signOut().addOnCompleteListener(task -> {
                    loading.setVisibility(View.GONE);
                    buttonGoogle.setEnabled(true);
                    buttonEntrar.setEnabled(true);
                    Toast.makeText(Login.this, "Erro no servidor. Tente novamente.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleSuccessfulLogin(com.limelight.login.model.LoginResponse response) {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        prefs.edit().putString("jwt_token", response.getToken()).apply();

        SocketManager.getInstance(this).connect();


        new MaterialAlertDialogBuilder(this)
                .setTitle("Login realizado")
                .setMessage("Bem-vindo de volta.")
                .setPositiveButton("Continuar", (dialog, which) -> {
                    startActivity(new Intent(this, Home.class));
                    finish();
                })
                .show();
    }

    public void onClickListenLogin(View view)
    {
        buttonEntrar.setEnabled(false);
        buttonEntrar.setText("");
        loading.setVisibility(View.VISIBLE);

        loginViewModel.login(editTextEmail.getText().toString(), editTextSenha.getText().toString(), serverAuthCode).observe(this, response ->
        {

            if(response != null)
            {


                buttonEntrar.setEnabled(true);
                buttonEntrar.setText("Entrar");
                loading.setVisibility(View.GONE);

                SharedPreferences prefs =
                        getSharedPreferences("auth", MODE_PRIVATE);

                prefs.edit()
                        .putString("jwt_token", response.getToken())
                        .apply();

                // Conecta ao Socket.IO após o login
                SocketManager.getInstance(this).connect();


                new MaterialAlertDialogBuilder(this)
                        .setTitle("Login realizado")
                        .setMessage("Bem-vindo de volta.")
                        .setPositiveButton("Continuar", (dialog, which) -> {
                            startActivity(new Intent(this, Home.class));
                            finish();
                        })
                        .show();

            }
        });

    }




}