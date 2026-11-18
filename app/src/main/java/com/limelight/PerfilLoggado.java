package com.limelight;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.limelight.home.ui.HomeViewModel;

/**
 * Fragmento que exibe as opções do perfil quando o usuário está logado.
 */
public class PerfilLoggado extends Fragment {

    private SessionManager sessionManager;
    private HomeViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_loggado, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupUI(view);
    }

    private void setupUI(View view) {
        Button buttonLogout = view.findViewById(R.id.btnLogout);
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> showLogoutConfirmation());
        }

        View btnStreamSettings = view.findViewById(R.id.btnStreamSettings);
        if (btnStreamSettings != null) {
            btnStreamSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.limelight.preferences.StreamSettings.class);
                startActivity(intent);
            });
        }

        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtTag = view.findViewById(R.id.txtTag);
        ImageView imgProfile = view.findViewById(R.id.imgProfile);

        String token = sessionManager.getToken();
        if (token != null) {
            viewModel.getHome(token).observe(getViewLifecycleOwner(), response -> {
                if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                    String username = response.getData().get(0).getUsername();
                    String avatar = response.getData().get(0).getAvatar();

                    if (txtName != null) txtName.setText(username);
                    if (txtTag != null) txtTag.setText("@" + username.toLowerCase().replace(" ", ""));
                    
                    if (imgProfile != null && avatar != null && !avatar.isEmpty()) {
                        Glide.with(this)
                                .load(avatar)
                                .placeholder(R.drawable.ic_profile_ghost)
                                .error(R.drawable.ic_profile_ghost)
                                .into(imgProfile);
                    }
                }
            });
        }
    }

    /**
     * Exibe um diálogo de confirmação antes de realizar o logout.
     */
    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sair")
                .setMessage("Tem certeza que deseja encerrar sua sessão?")
                .setPositiveButton("Sair", (dialog, which) -> performLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Realiza o processo de logout, desconectando o socket e limpando a sessão.
     */
    private void performLogout() {
        if (sessionManager.isLoggedIn()) {
            // Sinaliza encerramento de qualquer sessão ativa antes de deslogar
            SocketManager.getInstance(requireContext()).emit("session:end", null);

            // Limpa os dados da sessão local (JWT)
            sessionManager.logout();

            // Realiza o Logout do Google para permitir trocar de conta na próxima vez
            com.google.android.gms.auth.api.identity.Identity.getSignInClient(requireContext())
                    .signOut()
                    .addOnCompleteListener(task -> {
                        // Desconecta o Socket ao sair
                        SocketManager.getInstance(requireContext()).disconnect();

                        // Retorna para a Home limpando a pilha de atividades
                        Intent intent = new Intent(requireContext(), com.limelight.ui.Home.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    });
        }
    }
}
