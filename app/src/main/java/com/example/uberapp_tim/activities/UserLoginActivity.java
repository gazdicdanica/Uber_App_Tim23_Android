package com.example.uberapp_tim.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.auth0.android.jwt.JWT;
import com.example.uberapp_tim.R;
import com.example.uberapp_tim.activities.driver.DriverMainActivity;
import com.example.uberapp_tim.activities.passenger.PassengerMainActivity;
import com.example.uberapp_tim.activities.passenger.PassengerRegisterActivity;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.uberapp_tim.dto.LoginDTO;
import com.example.uberapp_tim.dto.TokensDTO;
import com.example.uberapp_tim.connection.ServiceUtils;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserLoginActivity extends AppCompatActivity {

    EditText editTextEmail;
    EditText editTextPassword;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.user_login_activity);
        editTextEmail= findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextTextPassword);

        sharedPreferences = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE);

        Button logInButton = findViewById(R.id.button3);
        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button signInButton = findViewById(R.id.buttonSignIn);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(UserLoginActivity.this, PassengerRegisterActivity.class));
            }
        });

        TextView txtForgotPassword = findViewById(R.id.forgot_pw);
        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO Forgot password activity
                Intent i = new Intent(UserLoginActivity.this, ResetPasswordActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
//                startActivity(new Intent(UserLoginActivity.this, ));
            }
        });
    }

    private void attemptLogin(){
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        LoginDTO loginDTO = new LoginDTO(email, password);

        ServiceUtils.userService.login(loginDTO).enqueue(new Callback<TokensDTO>() {
            @Override
            public void onResponse(Call<TokensDTO> call, Response<TokensDTO> response) {
                if(response.code() == 200){
                    sharedPreferences = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    Log.i("Response code", response.code() + " ");
                    Log.i("Message", "User successfully logged in");

                    editor.putString("accessToken", response.body().getAccessToken());
                    editor.apply();

                    changeActivity();

                }else if(response.code() == 400){
                    editTextEmail.setText("");
                    editTextPassword.setText("");
                    TextInputLayout emailLayout = (TextInputLayout) findViewById(R.id.emailInputLayout);
                    emailLayout.setError("   ");
                    TextInputLayout passwordLayout = (TextInputLayout) findViewById(R.id.passwordInputLayout);
                    passwordLayout.setError(getString(R.string.login_error));
                    editTextEmail.requestFocus();
                }

            }

            @Override
            public void onFailure(Call<TokensDTO> call, Throwable t) {
                Toast.makeText(UserLoginActivity.this, "Bad request", Toast.LENGTH_SHORT).show();
                Logger.getLogger(UserLoginActivity.class.getName()).log(Level.SEVERE, "ERROR", t);
            }

        });
    }

    private void changeActivity(){
        String token = sharedPreferences.getString("accessToken", null);
        if(token != null){
            JWT jwt = new JWT(token);

            ArrayList<String> claim = jwt.getClaim("role").asObject(ArrayList.class);
            assert claim != null;
            String role = claim.get(0);

            String id = jwt.getClaim("id").asString();

            sharedPreferences.edit().putString("id", id).apply();
            sharedPreferences.edit().putString("role", role).apply();
            sharedPreferences.edit().putString("email", editTextEmail.getText().toString()).apply();
            Log.d("LOGIN", getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("id", null));
            if(role.equals("ROLE_USER")){
                startActivity(new Intent(UserLoginActivity.this, PassengerMainActivity.class));
            }else{
                startActivity(new Intent(UserLoginActivity.this, DriverMainActivity.class));
            }

        }

    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
    }

    @Override
    protected void onResume(){
        super.onResume();
        editTextEmail.setText("");
        editTextPassword.setText("");
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}