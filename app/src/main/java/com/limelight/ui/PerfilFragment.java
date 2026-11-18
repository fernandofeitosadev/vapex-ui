package com.limelight.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.limelight.PerfilLoggado;
import com.limelight.R;
import com.limelight.SessionManager;
import com.limelight.login.ui.Login;
import com.limelight.register.ui.SignUp;

public class PerfilFragment extends Fragment {

    private Button btnSignIn, btnSignUp;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        SessionManager session = new SessionManager(requireContext());

        if (session.isLoggedIn()) {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new PerfilLoggado())
                    .commit();
        }


        View view = inflater.inflate(R.layout.fragment_perfil, container, false);


        btnSignIn = (Button) view.findViewById(R.id.btnLogin);
        btnSignUp = (Button) view.findViewById(R.id.btnCadastro);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), Login.class);
                startActivity(intent);
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), SignUp.class);
                startActivity(intent);
            }
        });




        return view;
    }
}