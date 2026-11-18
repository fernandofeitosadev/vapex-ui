package com.limelight.autoupdate;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.limelight.BuildConfig;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AutoUpdate {

    // Centralize a URL base aqui para não precisar passar no construtor
    private static final String UPDATE_ENDPOINT = "http://187.86.201.28:3000/api/version";

    private final Activity activity;

    public AutoUpdate(Activity activity) {
        this.activity = activity;
    }

    public void check() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(UPDATE_ENDPOINT).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Silencioso em caso de erro de rede
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    int latestVersion = json.getInt("versionCode");
                    String apkUrl = json.getString("apkUrl");

                    if (latestVersion > BuildConfig.VERSION_CODE) {
                        activity.runOnUiThread(() -> showUpdateDialog(apkUrl));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void check(int latestVersion, String apkUrl) {
        if (latestVersion > BuildConfig.VERSION_CODE) {
            activity.runOnUiThread(() -> showUpdateDialog(apkUrl));
        }
    }

    private void showUpdateDialog(String apkUrl) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Atualização Obrigatória")
                .setMessage("Uma nova versão crítica está disponível. Para garantir sua segurança e novos recursos, você precisa atualizar para continuar.")
                .setPositiveButton("Atualizar Agora", (dialog, which) -> downloadAndInstallApk(apkUrl))
                .setNegativeButton("Sair do App", (dialog, which) -> activity.finishAffinity())
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        Toast.makeText(activity, "Iniciando download da atualização...", Toast.LENGTH_LONG).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Atualizando Artemis");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        
        File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
        if (file.exists()) {
            file.delete();
        }
        
        request.setDestinationUri(Uri.fromFile(file));
        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == id) {
                    activity.unregisterReceiver(this);
                    installApk(file);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void installApk(File file) {
        if (!file.exists()) return;

        // Verifica permissão para instalar apps (Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:" + activity.getPackageName())));
                Toast.makeText(activity, "Por favor, autorize o Artemis a instalar a atualização.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Erro ao abrir o instalador nativo.", Toast.LENGTH_LONG).show();
        }
    }
}
