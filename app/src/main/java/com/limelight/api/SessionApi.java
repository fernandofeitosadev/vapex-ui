package com.limelight.api;


import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SessionApi {

    private static final String BASE_URL =
            "http://187.86.201.28:3000/api/sessions";

    private final RequestQueue queue;
    private final String token;

    public interface Callback {

        void onSuccess(JSONObject response);

        void onError(String error);
    }

    public SessionApi(Context context, String token) {

        this.queue = Volley.newRequestQueue(context);
        this.token = token;
    }

    // =========================
    // CREATE SESSION
    // =========================

    public void createSession(int gameId, Callback callback) {

        String url = BASE_URL + "/create";

        try {

            JSONObject body = new JSONObject();
            body.put("game_id", gameId);

            JsonObjectRequest request =
                    new JsonObjectRequest(

                            Request.Method.POST,
                            url,
                            body,

                            response -> {

                                callback.onSuccess(response);
                            },

                            error -> {

                                Log.e("SESSION_API", error.toString());

                                callback.onError(
                                        "Erro ao criar sessão"
                                );
                            }

                    ) {

                        @Override
                        public Map<String, String> getHeaders()
                                throws AuthFailureError {

                            Map<String, String> headers =
                                    new HashMap<>();

                            headers.put(
                                    "Authorization",
                                    "Bearer " + token
                            );

                            headers.put(
                                    "Content-Type",
                                    "application/json"
                            );

                            return headers;
                        }
                    };

            queue.add(request);

        } catch (Exception e) {

            callback.onError(e.getMessage());
        }
    }
}