package com.limelight.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.limelight.socketio.SocketManager;

import org.json.JSONObject;

import io.socket.client.Socket;

public class WalletManagerService extends Service {

    private static final String TAG = "WALLET_SERVICE";

    private Socket socket;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public WalletManagerService getService() {
            return WalletManagerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String token = intent != null ? intent.getStringExtra("token") : "";

        // 🔐 inicializa JWT no SocketManager
        SocketManager.init(token);

        connectSocket();

        return START_STICKY;
    }

    private void connectSocket() {

        try {

            Log.d(TAG, "Connecting via SocketManager...");

            // 🔌 pega socket singleton
            socket = SocketManager.getSocket();

            if (socket == null) {
                Log.e(TAG, "Socket null (token não inicializado?)");
                return;
            }

            // evita duplicação de listeners
            socket.off();

            // ================================
            // CONNECT
            // ================================
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "CONNECTED ✔");
                sendBroadcast(new Intent("WALLET_CONNECTED"));
            });

            // ================================
            // CONNECT ERROR
            // ================================
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "CONNECT ERROR: " + args[0]);
            });

            // ================================
            // BALANCE
            // ================================
            socket.on("wallet.balance", args -> {
                try {

                    JSONObject obj = (JSONObject) args[0];

                    Intent i = new Intent("WALLET_BALANCE");
                    i.putExtra("balance", obj.optDouble("balance", 0));

                    sendBroadcast(i);

                } catch (Exception e) {
                    Log.e(TAG, "balance error", e);
                }
            });

            // ================================
            // UPDATE
            // ================================
            socket.on("wallet:update", args -> {
                JSONObject obj = (JSONObject) args[0];

                double balance =
                        obj.optDouble("balance", 0);

                sendBroadcast(
                        new Intent("WALLET_UPDATE")
                                .putExtra("balance", balance)
                );
            });

            // ================================
            // CREDIT
            // ================================
            socket.on("wallet.credit", args -> {
                sendBroadcast(new Intent("WALLET_CREDIT")
                        .putExtra("data", args[0].toString()));
            });

            // ================================
            // DEBIT
            // ================================
            socket.on("wallet.debit", args -> {
                sendBroadcast(new Intent("WALLET_DEBIT")
                        .putExtra("data", args[0].toString()));
            });

            // ================================
            // TRANSACTION
            // ================================
            socket.on("wallet.transaction", args -> {
                sendBroadcast(new Intent("WALLET_TRANSACTION")
                        .putExtra("data", args[0].toString()));
            });

            // ================================
            // DISCONNECT
            // ================================
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "DISCONNECTED");
                sendBroadcast(new Intent("WALLET_DISCONNECTED"));
            });

        } catch (Exception e) {
            Log.e(TAG, "Socket error", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        socket = null;
        super.onDestroy();
    }
}