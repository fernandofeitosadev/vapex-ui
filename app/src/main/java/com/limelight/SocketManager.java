package com.limelight;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String SOCKET_URL = "http://187.86.201.28:3000/"; // Mesmo do Retrofit

    private static SocketManager instance;
    private Socket socket;
    private final Context context;
    private final SessionManager sessionManager;
    private boolean isConnecting = false;

    // Interface para escutar eventos globais
    public interface SocketEventListener {
        void onEvent(String event, Object... args);
        void onConnectionStatusChanged(boolean connected);
    }

    private final List<SocketEventListener> listeners = Collections.synchronizedList(new ArrayList<>());

    private SocketManager(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.context);
        setupSocket();
    }

    public static synchronized SocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    private void setupSocket() {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            
            // Se houver token, envia na conexão
            if (sessionManager.isLoggedIn()) {
                opts.auth = Collections.singletonMap("token", sessionManager.getToken());
            }

            socket = IO.socket(SOCKET_URL, opts);

            // Eventos padrão
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Conectado ao Socket.IO");
                isConnecting = false;
                notifyStatusChange(true);
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Desconectado do Socket.IO");
                notifyStatusChange(false);
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "Erro de conexão: " + args[0]);
                isConnecting = false;
            });

            // Eventos customizados solicitados
            setupCustomEvents();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Erro na URL do Socket: " + e.getMessage());
        }
    }

    private void setupCustomEvents() {
        String[] events = {
                "session:created", "wallet:update", "session:ended", "session:kicked",
                "session:expired", "session:queue", "queue:joined", "queue:position",
                "queue:state", "queue:left", "queue:failed"
        };
        for (String event : events) {
            socket.on(event, args -> {
                Log.d(TAG, "Evento recebido: " + event);
                notifyEventListeners(event, args);
            });
        }
    }

    public void connect() {
        if (socket != null && !socket.connected() && !isConnecting) {
            isConnecting = true;
            socket.connect();
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
        }
    }

    public void emit(String event, Object data) {
        if (socket != null && socket.connected()) {
            socket.emit(event, data);
        } else {
            Log.w(TAG, "Tentativa de emitir '" + event + "' sem conexão ativa.");
        }
    }

    // Métodos de atalho para os eventos solicitados
    public void createSession(JSONObject sessionData) {
        emit("session:create", sessionData);
    }

    public void endSession(JSONObject sessionData) {
        emit("session:end", sessionData);
    }

    public void sendHeartbeat() {
        emit("session:heartbeat", System.currentTimeMillis());
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    // Gerenciamento de Listeners
    public void addListener(SocketEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SocketEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyEventListeners(String event, Object... args) {
        synchronized (listeners) {
            for (SocketEventListener listener : listeners) {
                listener.onEvent(event, args);
            }
        }
    }

    private void notifyStatusChange(boolean connected) {
        synchronized (listeners) {
            for (SocketEventListener listener : listeners) {
                listener.onConnectionStatusChanged(connected);
            }
        }
    }
}
