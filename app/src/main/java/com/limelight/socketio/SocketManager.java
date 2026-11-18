package com.limelight.socketio;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static Socket socket;
    private static String token;
    private static final Object LOCK = new Object();

    public static synchronized void init(String jwtToken) {

        if (socket != null) {
            disconnect();
        }

        token = jwtToken;
    }

    public static Socket getSocket() {

        synchronized (LOCK) {

            if (socket != null) return socket;

            try {

                if (token == null || token.isEmpty()) {
                    Log.e("SocketManager", "JWT não inicializado");
                    return null;
                }

                IO.Options options = new IO.Options();
                options.reconnection = true;
                options.reconnectionAttempts = Integer.MAX_VALUE;
                options.reconnectionDelay = 2000;

                Map<String, String> auth = new HashMap<>();
                auth.put("token", token);

                options.auth = auth;

                socket = IO.socket("http://187.86.201.28:3000", options);
                socket.connect();

            } catch (Exception e) {
                Log.e("SocketManager", "Erro ao criar socket", e);
                socket = null;
            }

            return socket;
        }
    }

    public static synchronized void updateToken(String newToken) {

        token = newToken;

        disconnect();

        getSocket();
    }

    public static synchronized void disconnect() {

        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }
}