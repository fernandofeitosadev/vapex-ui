package com.limelight;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.limelight.binding.PlatformBinding;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.computers.ComputerManagerService;
import com.limelight.home.model.CreateSessionResponse;
import com.limelight.home.ui.HomeViewModel;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.utils.Dialog;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.UiHelper;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Activity responsible for launching a game session.
 * It handles session creation via API, pairing with the remote computer,
 * and finally starting the stream.
 */
public class LaunchGameActivity extends AppCompatActivity {

    private static final String TAG = "LaunchGameActivity";
    private static final String DEFAULT_UUID = "44F9E7F2-CDB0-CF04-EE8D-46FA67A3DF66";
    private static final String FALLBACK_APP_UUID = "B823EF8D-23C7-512A-3093-958AAD5F8A85";
    private static final int DEFAULT_DESKTOP_ID = 881448767;

    private TextView txtStatus;
    private TextView txtSubStatus;
    private static LaunchGameActivity instance;
    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private String uuidString;
    private String gameName;
    private String gameId;
    private String token;
    private ComputerDetails computer;
    private boolean inForeground;
    private boolean runningPolling;
    private boolean isQueued;
    private CreateSessionResponse pendingSessionResponse;
    private final android.os.Handler pollingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollingRunnable = this::requestSession;
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (inForeground && isQueued) {
                SocketManager.getInstance(LaunchGameActivity.this).emit("queue:heartbeat", null);
                pollingHandler.postDelayed(this, 10000);
            }
        }
    };

    /**
     * Listener for real-time queue updates via Socket.IO.
     */
    private final SocketManager.SocketEventListener socketListener = new SocketManager.SocketEventListener() {
        @Override
        public void onEvent(String event, Object... args) {
            if (!inForeground) return;

            runOnUiThread(() -> {
                try {
                    JSONObject data = (args != null && args.length > 0 && args[0] instanceof JSONObject) ? (JSONObject) args[0] : new JSONObject();
                    Log.d(TAG, "Socket event: " + event + " Data: " + data.toString());

                    switch (event) {
                        case "queue:joined":
                        case "queue:position":
                        case "queue:state":
                            setQueued(true);
                            int position = data.optInt("position", 0);
                            int total = data.optInt("total", 0);
                            int estWait = data.optInt("estimatedWaitSeconds", 0);

                            handleQueueUpdate(position, total, estWait);
                            break;

                        case "queue:left":
                            setQueued(false);
                            updateStatus("Sessão cancelada", "Você saiu da fila de espera.");
                            break;

                        case "queue:failed":
                            setQueued(false);
                            String errorMsg = data.optString("message", "Falha ao processar fila.");
                            updateStatus("Não foi possível entrar na fila", errorMsg);
                            break;

                        case "session:created":
                            Log.d(TAG, "Session created via Socket.IO. Refreshing...");
                            setQueued(false);
                            // Clean queue messages before proceeding
                            updateStatus("Sessão Pronta!", "Iniciando sua experiência...");
                            requestSession();
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling socket event: " + event, e);
                }
            });
        }

        @Override
        public void onConnectionStatusChanged(boolean connected) {
            Log.d(TAG, "Socket connection status: " + connected);
            if (connected && isQueued) {
                // Ao reconectar, solicita o estado atual da fila para sincronizar a UI
                SocketManager.getInstance(LaunchGameActivity.this).emit("queue:status", null);
            }
        }
    };

    /**
     * Updates the UI with current queue information.
     */
    private void handleQueueUpdate(int position, int total, int estWait) {
        int ahead = Math.max(0, position - 1);
        int mins = (estWait + 59) / 60;
        String timeText = mins <= 1 ? "menos de 1 min" : mins + " min";

        String statusTitle = "Na fila de espera";
        String subStatus;

        if (ahead == 0) {
            subStatus = "Você é o próximo! • Est. " + timeText;
        } else {
            if (total > 0) {
                subStatus = String.format(java.util.Locale.getDefault(),
                        "%d de %d na fila • Est. %s", position, total, timeText);
            } else {
                subStatus = String.format(java.util.Locale.getDefault(),
                        "%d pessoas à sua frente • Est. %s", ahead, timeText);
            }
        }

        updateStatus(statusTitle, subStatus);
    }

    /**
     * Service connection to manage communication with ComputerManagerService.
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            managerBinder = (ComputerManagerService.ComputerManagerBinder) binder;

            new Thread(() -> {
                managerBinder.waitForReady();

                String targetUuid = uuidString != null ? uuidString : DEFAULT_UUID;
                computer = managerBinder.getComputer(targetUuid);

                if (computer == null) {
                    computer = new ComputerDetails();
                    computer.uuid = targetUuid;
                    computer.name = "Cloud PC";
                }

                startComputerUpdates();
                runOnUiThread(LaunchGameActivity.this::tryStartStreaming);

                // Warm up crypto provider to speed up connection
                new AndroidCryptoProvider(LaunchGameActivity.this).getClientCertificate();
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    /**
     * Gets the current instance of LaunchGameActivity.
     * @return The current instance.
     */
    public static LaunchGameActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inForeground = true;
        instance = this;
        EdgeToEdge.enable(this);

        // Adjust for display cutouts (notches)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        UiHelper.setLocale(this);
        setContentView(R.layout.activity_launch_game);

        setupWindowInsets();
        initViews();
        handleIntentData();

        // Listen for socket events
        SocketManager.getInstance(this).addListener(socketListener);

        // Bind to ComputerManagerService to manage PC states and pairing
        bindService(new Intent(this, ComputerManagerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Configures window insets and system bar behavior.
     */
    private void setupWindowInsets() {
        UiHelper.notifyNewRootView(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> insets);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> insets);

        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    /**
     * Initializes UI components.
     */
    private void initViews() {
        txtStatus = findViewById(R.id.txtStatus);
        txtSubStatus = findViewById(R.id.txtSubStatus);
        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            SocketManager.getInstance(this).emit("queue:leave", null);
            setQueued(false);
            finish();
        });
    }

    /**
     * Extracts data from the intent and handles game session creation.
     */
    private void handleIntentData() {
        uuidString = getSharedPreferences("auth", MODE_PRIVATE).getString("computer_uuid", null);
        gameName = getIntent().getStringExtra("game_name");
        String gameImage = getIntent().getStringExtra("game_image");
        gameId = getIntent().getStringExtra("game_id");
        token = getSharedPreferences("auth", MODE_PRIVATE).getString("jwt_token", null);

        Log.d(TAG, "Intent Data - game_id: " + gameId + ", game_name: " + gameName + ", token: " + (token != null));

        TextView txtGameName = findViewById(R.id.txtGameName);
        if (gameName != null) txtGameName.setText(gameName);

        if (gameImage != null) {
            ImageView imgBackground = findViewById(R.id.imgGameBackground);
            ImageView imgThumb = findViewById(R.id.imgGameThumb);
            Glide.with(this).load(gameImage).into(imgBackground);
            Glide.with(this).load(gameImage).into(imgThumb);
        }

        if (gameId != null && token != null) {
            updateStatus("Iniciando...", null);
            requestSession();
        }

        // Apply external status updates if provided
        if (getIntent().hasExtra("status")) updateStatus(getIntent().getStringExtra("status"), null);
        if (getIntent().hasExtra("subStatus")) updateStatus(null, getIntent().getStringExtra("subStatus"));
    }

    /**
     * Updates status and sub-status text views.
     */
    private void updateStatus(@Nullable String status, @Nullable String subStatus) {
        if (status != null) {
            Log.d(TAG, "Status update: " + status);
            txtStatus.setText(status);
        }
        if (subStatus != null) {
            Log.d(TAG, "SubStatus update: " + subStatus);
            txtSubStatus.setText(subStatus);
        }
    }

    /**
     * Updates the queue state and manages heartbeat.
     */
    private void setQueued(boolean queued) {
        if (this.isQueued == queued) return;
        Log.d(TAG, "setQueued: " + queued);
        this.isQueued = queued;
        pollingHandler.removeCallbacks(heartbeatRunnable);
        if (queued && inForeground) {
            pollingHandler.post(heartbeatRunnable);
        }
    }

    /**
     * Requests a game session from the API, handling queueing if necessary.
     */
    private void requestSession() {
        if (gameId == null || token == null || !inForeground || isQueued) {
            Log.w(TAG, "Aborting requestSession: gameId=" + gameId + ", token=" + (token != null) + ", inForeground=" + inForeground + ", isQueued=" + isQueued);
            return;
        }

        Log.d(TAG, "Requesting session for gameId: " + gameId);
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        androidx.lifecycle.LiveData<CreateSessionResponse> sessionLiveData = viewModel.createSession(gameId, token, uuidString);
        sessionLiveData.observe(this, new androidx.lifecycle.Observer<CreateSessionResponse>() {
            @Override
            public void onChanged(CreateSessionResponse response) {
                sessionLiveData.removeObserver(this);

                if (!inForeground || pendingSessionResponse != null) return;

                if (response == null) {
                    Log.e(TAG, "API response is null");
                    updateStatus("Falha na Comunicação", "Erro ao conectar com o servidor da API.");
                    pollingHandler.removeCallbacks(pollingRunnable);
                    pollingHandler.postDelayed(pollingRunnable, 10000);
                    return;
                }

                Log.d(TAG, "API Response: success=" + response.isSuccess() + ", status=" + response.getStatus() + ", message=" + response.getMessage());

                if (!response.isSuccess()) {
                    updateStatus("Não foi possível iniciar", response.getMessage());
                    return;
                }

                // Check for queued state (HTTP 202)
                int httpStatus = response.getStatus();
                CreateSessionResponse.Data.Queue queue = response.getQueue();

                if (httpStatus == 202 || queue != null || "queued".equals(response.getDataStatus())) {
                    setQueued(true);
                    if (queue != null) {
                        handleQueueUpdate(queue.getPosition(), queue.getTotal(), queue.getEstimatedWaitSeconds());
                    } else {
                        updateStatus("Sua sessão está na fila", response.getMessage());
                    }

                    pollingHandler.removeCallbacks(pollingRunnable);
                    return;
                }

                // If not queued (assume HTTP 201), check for host IP
                if (response.getIp() == null) {
                    Log.e(TAG, "Host IP is null in successful response");
                    updateStatus("Erro de Configuração", "IP do host não encontrado.");
                    return;
                }

                updateStatus("Sessão pronta!", response.getMessage());
                pendingSessionResponse = response;
                tryStartStreaming();
            }
        });
    }

    /**
     * Attempts to start streaming if all conditions are met.
     */
    private void tryStartStreaming() {
        if (pendingSessionResponse == null) return;

        if (managerBinder == null) {
            updateStatus("Iniciando serviço...", null);
            return;
        }

        initializeComputerDetails();
        updateStatus("Verificando pareamento...", null);

        new Thread(() -> {
            try {
                NvHTTP httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(computer),
                        computer.httpsPort, managerBinder.getUniqueId(), computer.serverCert,
                        PlatformBinding.getCryptoProvider(this));



                if (httpConn.getPairState() == PairingManager.PairState.PAIRED) {
                    computer.pairState = PairingManager.PairState.PAIRED;
                    handlePairedState(httpConn);
                } else {
                    runOnUiThread(() -> {
                        updateStatus("Pareamento necessário", null);
                        doPair(computer, null, null);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking pair state", e);
                NvApp fallbackApp = new NvApp(gameName != null ? gameName : "Steam", FALLBACK_APP_UUID, 0, false);
                runOnUiThread(() -> performStartStreaming(fallbackApp));
            }
        }).start();
    }

    /**
     * Prepares the computer object with network details.
     */
    private void initializeComputerDetails() {
        String targetUuid = uuidString != null ? uuidString : DEFAULT_UUID;
        if (computer == null) {
            computer = managerBinder.getComputer(targetUuid);
            if (computer == null) {
                computer = new ComputerDetails();
                computer.uuid = targetUuid;
            }
        }

        int port = pendingSessionResponse.getPort();
        if (port == 0) port = NvHTTP.DEFAULT_HTTP_PORT;

        computer.activeAddress = new ComputerDetails.AddressTuple(pendingSessionResponse.getIp(), port);
        computer.manualAddress = computer.activeAddress;
        computer.state = ComputerDetails.State.ONLINE;
        computer.httpsPort = port;
    }

    /**
     * Handles the logic when the device is already paired.
     */
    private void handlePairedState(NvHTTP httpConn) throws IOException, XmlPullParserException {
        ComputerDetails serverDetails = httpConn.getComputerDetails(true);
        NvApp targetApp = resolveTargetApp(httpConn, serverDetails);

        syncComputerDetails(serverDetails);

        runOnUiThread(() -> performStartStreaming(targetApp));
    }

    /**
     * Resolves which app to launch based on server state and intent data.
     */
    private NvApp resolveTargetApp(NvHTTP httpConn, ComputerDetails serverDetails) throws IOException, XmlPullParserException {
        // Resume running game if applicable
        if (serverDetails.runningGameId != 0) {
            Log.d(TAG, "Resuming running game with ID: " + serverDetails.runningGameId);
            return new NvApp(gameName != null ? gameName : "Running Game", null, serverDetails.runningGameId, false);
        }

        // Try to find the app by name
        NvApp app = httpConn.getAppByName(gameName);
        if (app != null) {
            Log.d(TAG, "Found app by name: " + gameName + " with ID: " + app.getAppId());
            return app;
        }

        // Fallback to Desktop
        app = httpConn.getAppByName("Desktop");
        if (app != null) {
            Log.d(TAG, "Falling back to Desktop with ID: " + app.getAppId());
            return app;
        }

        // Manual fallback to avoid common "Error 0"
        Log.w(TAG, "App not found in list, using manual fallback");
        return new NvApp(gameName != null ? gameName : "Desktop", FALLBACK_APP_UUID, DEFAULT_DESKTOP_ID, false);
    }

    /**
     * Synchronizes computer details between local and server states.
     */
    private void syncComputerDetails(ComputerDetails serverDetails) {
        computer.uuid = serverDetails.uuid;
        if (serverDetails.serverCert != null) {
            computer.serverCert = serverDetails.serverCert;
        }
        computer.name = serverDetails.name;
        computer.pairState = serverDetails.pairState;

        ComputerDetails binderComp = managerBinder.getComputer(computer.uuid);
        if (binderComp != null) {
            if (serverDetails.serverCert != null) {
                binderComp.serverCert = computer.serverCert;
            }
            binderComp.uuid = computer.uuid;
            binderComp.name = computer.name;
            binderComp.pairState = computer.pairState;
        }
    }

    /**
     * Starts the actual streaming process.
     */
    private void performStartStreaming(NvApp nvapp) {
        if (managerBinder == null || pendingSessionResponse == null) return;

        try {
            updateStatus("Preparando conexão...", "IP: " + pendingSessionResponse.getIp());

            ComputerDetails binderComp = managerBinder.getComputer(computer.uuid);
            if (binderComp != null) {
                binderComp.activeAddress = computer.activeAddress;
                binderComp.manualAddress = computer.manualAddress;
                binderComp.state = ComputerDetails.State.ONLINE;
                binderComp.httpsPort = computer.httpsPort;
                computer = binderComp;
            }

            computer.pairState = PairingManager.PairState.PAIRED;
            Log.d("STATUSP", "STARTING");
            ServerHelper.doStart(this, nvapp, computer, managerBinder, true);
            pendingSessionResponse = null;
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error in doStart", e);
            updateStatus("Erro ao carregar o jogo", e.getMessage());
        }
    }

    /**
     * Handles the pairing process with the remote PC.
     */
    private void doPair(final ComputerDetails targetComputer, String otp, String passphrase) {
        Log.d("STATUSP", "paring");
        if (targetComputer.state == ComputerDetails.State.OFFLINE || targetComputer.activeAddress == null) {
            Toast.makeText(this, R.string.pair_pc_offline, Toast.LENGTH_SHORT).show();
            return;
        }
        if (managerBinder == null) {
            Toast.makeText(this, R.string.error_manager_not_running, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, R.string.pairing, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                stopComputerUpdates(true);
                NvHTTP httpConn = new NvHTTP(ServerHelper.getCurrentAddressFromComputer(targetComputer),
                        targetComputer.httpsPort, managerBinder.getUniqueId(), targetComputer.serverCert,
                        PlatformBinding.getCryptoProvider(this));

                if (httpConn.getPairState() == PairingManager.PairState.PAIRED) {
                    handlePairingSuccess(targetComputer, httpConn, null);
                } else {
                    handlePairingProcess(targetComputer, httpConn, otp, passphrase);
                }
            } catch (Exception e) {
                Log.e(TAG, "Pairing error", e);
                handlePairingFailure(e.getMessage());
            }
        }).start();
    }

    /**
     * Manages the active pairing challenge.
     */
    private void handlePairingProcess(ComputerDetails targetComp, NvHTTP httpConn, String otp, String passphrase) throws IOException, XmlPullParserException {
        String pinStr = "0000";
        if (pinStr == null) {
            pinStr = PairingManager.generatePinString();
            final String finalPin = pinStr;
            runOnUiThread(() -> updateStatus("Digite o PIN no PC", "PIN: " + finalPin));
        }
        Log.d("STATUSP", "paring");
        PairingManager pm = httpConn.getPairingManager();
        PairingManager.PairState state = pm.pair(httpConn.getServerInfo(false), "0000", passphrase);

        Log.d("STATUSP", state.toString());

        String errorMsg = null;
        switch (state) {
            case PAIRED:
                handlePairingSuccess(targetComp, httpConn, pm);
                return;
            case PIN_WRONG: errorMsg = getString(R.string.pair_incorrect_pin); break;
            case FAILED: errorMsg = targetComp.runningGameId != 0 ? getString(R.string.pair_pc_ingame) : getString(R.string.pair_fail); break;
            case ALREADY_IN_PROGRESS: errorMsg = getString(R.string.pair_already_in_progress); break;
        }
        handlePairingFailure(errorMsg);
    }

    /**
     * Finalizes the pairing process upon success.
     */
    private void handlePairingSuccess(ComputerDetails targetComp, NvHTTP httpConn, @Nullable PairingManager pm) throws IOException, XmlPullParserException {
        targetComp.uuid = httpConn.getComputerDetails(false).uuid;
        ComputerDetails binderComp = managerBinder.getComputer(targetComp.uuid);
        if (binderComp == null) binderComp = targetComp;
        if (pm != null) binderComp.serverCert = pm.getPairedCert();
        binderComp.pairState = PairingManager.PairState.PAIRED;

        getSharedPreferences("auth", MODE_PRIVATE).edit().putString("computer_uuid", targetComp.uuid).apply();
        uuidString = targetComp.uuid;
        managerBinder.invalidateStateForComputer(targetComp.uuid);

        runOnUiThread(this::tryStartStreaming);
    }

    /**
     * Handles pairing failure by showing a message and resuming updates.
     */
    private void handlePairingFailure(String errorMsg) {
        Dialog.closeDialogs();
        runOnUiThread(() -> {
            if (errorMsg != null) Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            startComputerUpdates();
        });
    }

    /**
     * Starts polling for computer state updates.
     */
    private void startComputerUpdates() {
        if (managerBinder != null && !runningPolling && inForeground) {
            managerBinder.startPolling(details -> Log.d(TAG, "notifyComputerUpdated: " + details));
            runningPolling = true;
        }
    }

    /**
     * Stops polling for computer state updates.
     * @param wait Whether to wait for polling to stop.
     */
    private void stopComputerUpdates(boolean wait) {
        if (managerBinder != null && runningPolling) {
            managerBinder.stopPolling();
            if (wait) managerBinder.waitForPollingStopped();
            runningPolling = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        inForeground = true;
        startComputerUpdates();
        if (isQueued) {
            pollingHandler.removeCallbacks(heartbeatRunnable);
            pollingHandler.post(heartbeatRunnable);
        }
        if (pendingSessionResponse == null && gameId != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            requestSession();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        inForeground = false;
        stopComputerUpdates(false);
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.removeCallbacks(heartbeatRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        SocketManager.getInstance(this).removeListener(socketListener);
        if (managerBinder != null) {
            unbindService(serviceConnection);
        }
    }
}
