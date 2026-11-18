package com.limelight.wallet.ui;


import static android.content.Context.MODE_PRIVATE;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.limelight.R;
import com.limelight.SocketManager;
import com.limelight.wallet.model.PixResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;


public class WalletFragment extends Fragment {
    private TextView txtBalance;
    private View btnPix;
    private View loadingOverlay;
    private SocketManager.SocketEventListener socketListener;
    private WalletViewModel viewModel;

    private boolean isBalanceVisible = true;
    private double currentBalance = 0.0;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        txtBalance = view.findViewById(R.id.txtBalance);
        btnPix = view.findViewById(R.id.btnPix);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        prefs = getActivity().getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);

        isBalanceVisible = prefs.getBoolean("balance_visible", true);

        btnPix.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            showAmountDialog(token);
        });

        txtBalance.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            isBalanceVisible = !isBalanceVisible;
            prefs.edit().putBoolean("balance_visible", isBalanceVisible).apply();
            updateBalanceUI();
        });

        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);
        setLoading(true);
        viewModel.getWallet(token).observe(getViewLifecycleOwner(), walletResponse -> {
            setLoading(false);
            if (walletResponse != null && walletResponse.getData() != null) {
                try {
                    currentBalance = Double.parseDouble(walletResponse.getData().getBalance());
                    updateBalanceUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        setupSocketListener();

        return view;
    }

    private void setLoading(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void updateBalanceUI() {
        if (txtBalance == null) return;
        if (isBalanceVisible) {
            String saldoFormatado = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(currentBalance);
            txtBalance.setText(saldoFormatado);
        } else {
            txtBalance.setText("R$ ••••••");
        }
    }

    private void setupSocketListener() {
        socketListener = new SocketManager.SocketEventListener() {
            @Override
            public void onEvent(String event, Object... args) {
                if ("wallet:update".equals(event)) {
                    if (getActivity() == null || !isAdded() || args == null || args.length == 0) return;

                    getActivity().runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(args[0].toString());
                            currentBalance = json.optDouble("balance", 0.0);
                            updateBalanceUI();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {}
        };
        SocketManager.getInstance(getContext()).addListener(socketListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (socketListener != null) {
            SocketManager.getInstance(getContext()).removeListener(socketListener);
        }
    }

    private void showAmountDialog(String token) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_amount, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);
        builder.setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        AlertDialog dialog = builder.create();

        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        MaterialButton btn10 = dialogView.findViewById(R.id.btnAmount10);
        MaterialButton btn50 = dialogView.findViewById(R.id.btnAmount50);
        MaterialButton btn100 = dialogView.findViewById(R.id.btnAmount100);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        View.OnClickListener amountListener = v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            String amount = ((MaterialButton) v).getText().toString().replace("R$ ", "") + ".00";
            etAmount.setText(amount);
        };

        btn10.setOnClickListener(amountListener);
        btn50.setOnClickListener(amountListener);
        btn100.setOnClickListener(amountListener);

        btnConfirm.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            String amountStr = etAmount.getText().toString();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    dialog.dismiss();
                    setLoading(true);
                    viewModel.createPix(token, amount).observe(getViewLifecycleOwner(), pixResponse -> {
                        setLoading(false);
                        if (pixResponse != null) {
                            showPixDialog(pixResponse);
                        } else {
                            Toast.makeText(getContext(), "Erro ao gerar PIX", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Valor inválido", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showPixDialog(PixResponse data) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pix_payment, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);
        builder.setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        AlertDialog dialog = builder.create();

        ImageView imgQrCode = dialogView.findViewById(R.id.imgQrCode);
        TextView txtPixCode = dialogView.findViewById(R.id.txtPixCode);
        MaterialButton btnCopy = dialogView.findViewById(R.id.btnCopyCode);
        MaterialButton btnShare = dialogView.findViewById(R.id.btnShare);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        if (data.getQrCodeBase64() != null) {
            byte[] decodedString = Base64.decode(data.getQrCodeBase64(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imgQrCode.setImageBitmap(decodedByte);
        }

        txtPixCode.setText(data.getQrCode());

        btnCopy.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PIX Copy Paste", data.getQrCode());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Código copiado!", Toast.LENGTH_SHORT).show();
        });

        btnShare.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, data.getQrCode());
            startActivity(Intent.createChooser(intent, "Compartilhar Código PIX"));
        });

        btnClose.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dialog.dismiss();
        });

        dialog.show();
    }
}
