package com.limelight.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ParearManager {

    private static final String BASE_URL =
            "http://187.86.201.28:3000/api/sunschine/parear?pincode=";

    public static void parear(Context context, String pincode) {

        RequestQueue queue = Volley.newRequestQueue(context);

        String url = BASE_URL + pincode;

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("RESPOSTA: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("ERRO: " + error.toString());
                    }
                }
        );

        queue.add(request);
    }
}