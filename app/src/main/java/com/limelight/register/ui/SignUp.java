package com.limelight.register.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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

import okhttp3.Request;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.limelight.R;
import com.limelight.login.ui.Login;
import com.google.android.gms.common.Scopes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SignUp extends AppCompatActivity {

    // No topo do SignUp.java ou como constante
    private static final String WEB_CLIENT_ID = "459829365552-7sf5pu5bojrm3k0l39fcggiaedf1l2bf.apps.googleusercontent.com";



    private SignUpViewModel viewModel;
    private EditText editUser;
    private EditText editEmail;
    private EditText editPass;
    private Button buttonregister;
    private Button buttonGoogle;
    private ProgressBar loading;
    private String serverAuthCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editUser = (EditText) findViewById(R.id.editUser);
        editEmail = (EditText) findViewById(R.id.editEmail);
        editPass = (EditText) findViewById(R.id.editPass);
        buttonregister = (Button) findViewById(R.id.buttonregister);
        buttonGoogle = (Button) findViewById(R.id.buttonGoogle);
        loading = (ProgressBar) findViewById(R.id.loading);

        viewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        buttonregister.setOnClickListener(this::signUp);
        buttonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestDrivePermission();
            }
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

    public void signUp(View view){
        buttonregister.setEnabled(false);
        buttonregister.setText("");
        loading.setVisibility(View.VISIBLE);

        viewModel.register(editUser.getText().toString(),editEmail.getText().toString(),editPass.getText().toString(), serverAuthCode).observe(this, response -> {
            buttonregister.setEnabled(true);
            buttonregister.setText("Cadastrar");
            loading.setVisibility(View.GONE);

            if(response != null && response.isSuccess()){
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Verifique seu e-mail")
                        .setMessage(
                                "Enviamos um link de confirmação para:\n\n" +
                                        editEmail.getText().toString() +
                                        "\n\nAbra sua caixa de entrada e clique no link para ativar sua conta."
                        )
                        .setPositiveButton("Já verifiquei", (dialog, which) -> {
                            startActivity(new Intent(this, Login.class));
                            finish();
                        })
                        .setNegativeButton("Fechar", null)
                        .show();
            } else {
                Toast.makeText(this, "Erro ao realizar cadastro. Tente novamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void requestDrivePermission() {
        // 1. Definimos os escopos necessários
        List<Scope> requestedScopes = Arrays.asList(
                new Scope(Scopes.PROFILE),
                new Scope(Scopes.EMAIL),
                new Scope(Scopes.DRIVE_FILE)
        );

        // 2. Criamos a requisição pedindo acesso Offline (isso gera o ServerAuthCode)
        com.google.android.gms.auth.api.identity.AuthorizationRequest authorizationRequest =
                com.google.android.gms.auth.api.identity.AuthorizationRequest.builder()
                        .setRequestedScopes(requestedScopes)
                        .requestOfflineAccess(WEB_CLIENT_ID) // <--- OBRIGATÓRIO PARA TER O AUTH CODE
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

    // 2. Registre o launcher para receber o resultado da permissão
    private ActivityResultLauncher<IntentSenderRequest> startAuthorizationIntent =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    activityResult -> {
                        try {
                            com.google.android.gms.auth.api.identity.AuthorizationResult result =
                                    com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this)
                                            .getAuthorizationResultFromIntent(activityResult.getData());

                            // 1. Token para uso temporário no Android
                            String accessToken = result.getAccessToken();

                            // 2. Código para enviar ao seu servidor Backend
                            serverAuthCode = result.getServerAuthCode();

                            // Cadastro/Login imediato com Google
                            signUpWithGoogle(accessToken, serverAuthCode);

                        } catch (com.google.android.gms.common.api.ApiException e) {
                            Log.e("DRIVE_AUTH", "Erro: " + e.getStatusCode());
                        }
    });


    private void signUpWithGoogle(String accessToken, String code) {
        loading.setVisibility(View.VISIBLE);
        buttonGoogle.setEnabled(false);
        buttonregister.setEnabled(false);

        // Otimização: Enviamos apenas o código para o backend.
        // O seu Node.js extrai e-mail, nome e foto do 'id_token' do Google.
        viewModel.loginWithGoogle(null, null, code, null).observe(SignUp.this, loginResponse -> {
            loading.setVisibility(View.GONE);
            buttonGoogle.setEnabled(true);
            buttonregister.setEnabled(true);

            if (loginResponse != null) {
                handleSuccessfulGoogleSignUp(loginResponse);
            } else {
                Toast.makeText(SignUp.this, "Erro ao realizar cadastro com Google no servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSuccessfulGoogleSignUp(com.limelight.login.model.LoginResponse response) {
        android.content.SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        prefs.edit().putString("jwt_token", response.getToken()).apply();

        // Se quiser conectar ao socket ou configurar PC aqui também:
        // com.limelight.SocketManager.getInstance(this).connect();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Conta criada e vinculada!")
                .setMessage("Sua conta foi criada com sucesso via Google.")
                .setPositiveButton("Continuar para o App", (dialog, which) -> {
                    startActivity(new Intent(this, com.limelight.ui.Home.class));
                    finish();
                })
                .show();
    }

    private void handleSuccessfulSignUp(String email) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Conta criada!")
                .setMessage(
                        "Sua conta foi vinculada ao Google com sucesso.\n\n" +
                                "E-mail: " + email +
                                "\n\nVocê já pode fazer login."
                )
                .setPositiveButton("Fazer Login", (dialog, which) -> {
                    startActivity(new Intent(this, Login.class));
                    finish();
                })
                .show();
    }

}