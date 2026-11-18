package com.limelight.services;

import android.app.Service;
import android.content.Intent;
import android.util.Log;

import com.limelight.socketio.SocketManager;

import org.json.JSONObject;

import io.socket.client.Socket;

public class SessionManagerService extends Service {

    private static final String TAG = "SESSION_SERVICE";

    private Socket socket;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String token = intent != null ? intent.getStringExtra("token") : null;

        // 🔐 inicializa JWT no SocketManager
        if (token != null) {
            SocketManager.init(token);
        }

        Log.d(TAG, "Service iniciado");

        connectSocket();

        return START_STICKY;
    }

    private void connectSocket() {

        try {

            // 🔌 pega socket singleton já com JWT
            socket = SocketManager.getSocket();

            if (socket == null) {
                Log.e(TAG, "Socket null (JWT não inicializado)");
                return;
            }

            // evita duplicar listeners
            socket.off();

            // =========================
            // CONNECT
            // =========================
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "CONNECTED ✔");

                sendBroadcast(new Intent("SESSION_CONNECTED"));
            });

            // =========================
            // SESSION CREATED
            // =========================
            socket.on("session:created", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];

                    Log.d(TAG, "SESSION CREATED: " + data.toString());

                    sendBroadcast(new Intent("SESSION_CREATED")
                            .putExtra("data", data.toString()));

                } catch (Exception e) {
                    Log.e(TAG, "session created error", e);
                }
            });

            // =========================
            // SESSION UPDATE
            // =========================
            socket.on("session:update", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];

                    sendBroadcast(new Intent("SESSION_UPDATE")
                            .putExtra("data", data.toString()));

                } catch (Exception e) {
                    Log.e(TAG, "session update error", e);
                }
            });

            // =========================
            // SESSION ENDED
            // =========================
            socket.on("session:ended", args -> {
                Log.d(TAG, "SESSION ENDED");

                sendBroadcast(new Intent("SESSION_ENDED"));
            });

            // =========================
            // DISCONNECT
            // =========================
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "DISCONNECTED");

                sendBroadcast(new Intent("SESSION_DISCONNECTED"));
            });

            // =========================
            // ERROR
            // =========================
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "CONNECT ERROR: " + args[0]);
            });

        } catch (Exception e) {
            Log.e(TAG, "Socket error", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
}