package com.limelight.home.ui;


import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.limelight.LaunchGameActivity;
import com.limelight.R;
import com.limelight.SocketManager;
import com.limelight.home.ui.adapter.BannerAdapter;
import com.limelight.home.ui.adapter.GamesAdapter;
import com.limelight.home.ui.model.BannerModel;
import com.limelight.home.ui.model.GameModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class HomeFragment extends Fragment {


    private static final Logger log = LoggerFactory.getLogger(HomeFragment.class);
    private SocketManager.SocketEventListener socketListener;

    private HomeViewModel viewModel;


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);


        SharedPreferences prefs = getActivity().getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        List<BannerModel> banners = new ArrayList<>();
        banners.add(new BannerModel("Warzone", "Seu legado começa no corredor da escola.", "NOVO", R.drawable.banne34));


        RecyclerView recyclerGames = view.findViewById(R.id.recyclerGames);




        recyclerGames.setLayoutManager(
                new LinearLayoutManager(
                        getContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                )
        );

        List<GameModel> games = new ArrayList<>();

        games.add(new GameModel(
                1,
                "Assassin's Creed",
                "Grátis para Jogar",
                "https://imgix.cosmicjs.com/5c968be0-deab-11f1-9283-01aa56c6a663-r2_64a7d223-69a1-4451-acf9-ba621f33c09c1.jpg",
                "ANIMAÇÃO"
        ));

        games.add(new GameModel(
                12,
                "Cyberpunk 2077",
                "RPG de Ação",
                "https://imgix.cosmicjs.com/01eeea50-dc82-11f1-a4a7-c1718c76fc2f-img31.jpg",
                "AÇÃO"
        ));

        games.add(new GameModel(
                62,
                "Red Dead Redemption 2",
                "Mundo Aberto",
                "https://imgix.cosmicjs.com/9236fd80-deaa-11f1-9283-01aa56c6a663-r2_68d26f81-2bc0-4631-9cbb-8246949cb6ed1.jpg",
                "AVENTURA"
        ));

        GamesAdapter adapter = new GamesAdapter(games, game -> {
            Intent intent = new Intent(getContext(), com.limelight.GameDetailActivity.class);
            intent.putExtra("game_id", String.valueOf(game.getId()));
            intent.putExtra("game_name", game.getTitle());
            intent.putExtra("game_image", game.getImage());
            startActivity(intent);
        });
        recyclerGames.setAdapter(adapter);

        SocketManager.getInstance(getContext()).addListener(socketListener = new SocketManager.SocketEventListener() {
            @Override
            public void onEvent(String event, Object... args) {
                if (event.equals("session:queue")) {

                }

                if (event.equals("session:created")) {

                }

                if (event.equals("wallet:update")) {

                }
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {}
        });

        // Configuração do Overlay VIP Flutuante no rodapé
        View overlayVipBanner = view.findViewById(R.id.overlayVipBanner);
        View btnCloseVipBanner = view.findViewById(R.id.btnCloseVipBanner);
        View btnVipBannerGo = view.findViewById(R.id.btnVipBannerGo);
        TextView tvVipPriceSub = view.findViewById(R.id.tvVipPriceSub);

        if (tvVipPriceSub != null) {
            String text = "partir de apenas <font color='#FFD700'><b>R$4.99</b></font>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                tvVipPriceSub.setText(android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY));
            } else {
                tvVipPriceSub.setText(android.text.Html.fromHtml(text));
            }
        }

        if (btnCloseVipBanner != null && overlayVipBanner != null) {
            btnCloseVipBanner.setOnClickListener(v -> overlayVipBanner.setVisibility(View.GONE));
        }

        if (btnVipBannerGo != null) {
            btnVipBannerGo.setOnClickListener(v -> showPromoPopup());
        }

        if (overlayVipBanner != null) {
            overlayVipBanner.setOnClickListener(v -> showPromoPopup());
        }

        // Exibe o popup VIP promocional ao abrir a tela principal do app
        view.post(this::showPromoPopup);

        return view;
    }

    private void showPromoPopup() {
        if (getContext() == null || !isAdded()) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_promo, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Dialog_NoActionBar)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Botão 'X' para fechar/remover o popup
        View btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Botão "Obtenha VIP agora"
        View btnAction = dialogView.findViewById(R.id.btnAction);
        if (btnAction != null) {
            btnAction.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(getContext(), "Obtendo assinatura VIP...", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }




    private String formatUsername(String name) {
        if (name == null || name.isEmpty()) return "";

        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1) {
            String firstName = parts[0];
            String lastName = parts[parts.length - 1];
            String initial = lastName.substring(0, 1).toUpperCase();

            // Capitaliza o primeiro nome
            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();

            return firstName + " " + initial + ".";
        }

        // Se for apenas um nome, apenas capitaliza
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socketListener != null) {
            SocketManager.getInstance(getContext()).removeListener(socketListener);
        }
    }


}
