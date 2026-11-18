package com.limelight;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.limelight.utils.UiHelper;

/**
 * Activity displaying detailed information for a specific game,
 * including media hero banner, poster, genres/tags, notice board,
 * ad banner, reviews, and a floating launch ("Jogar") button.
 */
public class GameDetailActivity extends AppCompatActivity {

    private String gameId;
    private String gameName;
    private String gameImage;

    private ImageView imgBanner;
    private ImageView imgPoster;
    private TextView txtTitle;
    private TextView txtWatermark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        UiHelper.setLocale(this);
        setContentView(R.layout.activity_game_detail);

        setupWindowInsets();
        initViews();
        handleIntentData();
    }

    private void setupWindowInsets() {
        UiHelper.notifyNewRootView(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> insets);

        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void initViews() {
        imgBanner = findViewById(R.id.imgBanner);
        imgPoster = findViewById(R.id.imgPoster);
        txtTitle = findViewById(R.id.txtTitle);
        txtWatermark = findViewById(R.id.txtGameWatermark);

        // Voltar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Botão principal Jogar
        findViewById(R.id.btnPlay).setOnClickListener(v -> launchGame());

        // Botão secundário (layers/menu)
        findViewById(R.id.btnLayers).setOnClickListener(v ->
                Toast.makeText(this, "Opções de jogo", Toast.LENGTH_SHORT).show());

        // Botões de topo
        findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Confira " + (gameName != null ? gameName : "este jogo") + " no Artemis!");
            startActivity(Intent.createChooser(shareIntent, "Compartilhar jogo"));
        });

        findViewById(R.id.btnFavorite).setOnClickListener(v ->
                Toast.makeText(this, "Adicionado aos favoritos!", Toast.LENGTH_SHORT).show());

        // Botão Avaliações
        findViewById(R.id.btnEscreverAvaliacao).setOnClickListener(v ->
                Toast.makeText(this, "Escrever avaliação", Toast.LENGTH_SHORT).show());
    }

    private void handleIntentData() {
        gameId = getIntent().getStringExtra("game_id");
        gameName = getIntent().getStringExtra("game_name");
        gameImage = getIntent().getStringExtra("game_image");

        if (gameName != null && !gameName.isEmpty()) {
            txtTitle.setText(gameName);
            txtWatermark.setText(gameName.toUpperCase());
        }

        if (gameImage != null && !gameImage.isEmpty()) {
            Glide.with(this).load(gameImage).into(imgBanner);
            Glide.with(this).load(gameImage).into(imgPoster);
        }
    }

    private void launchGame() {
        Intent intent = new Intent(this, LaunchGameActivity.class);
        if (gameId != null) intent.putExtra("game_id", gameId);
        if (gameName != null) intent.putExtra("game_name", gameName);
        if (gameImage != null) intent.putExtra("game_image", gameImage);
        startActivity(intent);
    }
}
