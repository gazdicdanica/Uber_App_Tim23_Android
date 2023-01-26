package com.example.uberapp_tim.activities.driver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberapp_tim.R;
import com.example.uberapp_tim.activities.ChatActivity;
import com.example.uberapp_tim.connection.ServiceUtils;
import com.example.uberapp_tim.dto.RideDTO;
import com.example.uberapp_tim.fragments.RideFragment;
import com.example.uberapp_tim.model.message.Panic;
import com.example.uberapp_tim.model.route.Location;
import com.example.uberapp_tim.service.FragmentToActivity;
import com.example.uberapp_tim.tools.FragmentTransition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.model.Duration;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverRideActivity extends AppCompatActivity implements FragmentToActivity {

    Long rideId;
    Long passengerId;

    MaterialButton startRideBtn;
    MaterialButton endRideBtn;
    FloatingActionButton messageBtn;
    ExtendedFloatingActionButton panicBtn;
    TextView countdownTV;
    TextView kmTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rideId = getIntent().getLongExtra("id", 0);

        setContentView(R.layout.activity_ride);

        startRideBtn = findViewById(R.id.start_ride_btn);
        endRideBtn = findViewById(R.id.end_ride_btn);
        messageBtn = findViewById(R.id.message_btn_ride);
        panicBtn = findViewById(R.id.panic_btn);
        countdownTV = findViewById(R.id.countdown_time);
        kmTV = findViewById(R.id.countdown_distance);


        startRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String jwt = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                ServiceUtils.rideService.startRide("Bearer " + jwt, rideId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        startRideBtn.setVisibility(View.GONE);
                        endRideBtn.setVisibility(View.VISIBLE);
                        panicBtn.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });

            }
        });

        endRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String jwt = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                ServiceUtils.rideService.endRide("Bearer " + jwt, rideId).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Intent main = new Intent(DriverRideActivity.this, DriverMainActivity.class);
                        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(main);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });

            }
        });

        panicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                android.app.AlertDialog.Builder builder = new AlertDialog.Builder(DriverRideActivity.this);
                builder.setTitle("Please enter a reason");
                final EditText input = new EditText(DriverRideActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setPadding(30,30,30,30);
                builder.setView(input);

                builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String reason = input.getText().toString();
                        Panic panic = new Panic();
                        panic.setReason(reason);

                        String jwt = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                        ServiceUtils.rideService.panicRide("Bearer " + jwt, rideId, panic).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Toast.makeText(DriverRideActivity.this, "Panic was sent", Toast.LENGTH_SHORT).show();
                                Intent main = new Intent(DriverRideActivity.this, DriverMainActivity.class);
                                main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(main);
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {

                            }
                        });
                    }
                });
                builder.create().show();
            }
        });

        messageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chatIntent = new Intent(DriverRideActivity.this, ChatActivity.class);
                chatIntent.putExtra("rideId", rideId);
                chatIntent.putExtra("userId", passengerId);
                startActivity(chatIntent);
            }
        });

        Fragment fragment = RideFragment.newInstance();
        FragmentTransition.to(fragment, DriverRideActivity.this, false);
    }

    public Bundle getIdBundle(){
        Bundle bundle = new Bundle();
        bundle.putLong("id", rideId);
        return bundle;
    }


    @Override
    public void communicate(String msg) {

    }

    @Override
    public void sendStartLocation(Location location) {

    }

    @Override
    public void sendFinishLocation(Location location) {

    }

    @Override
    public void sendRideData(float distance, Duration duration) {

    }

    @Override
    public void saveLatLng(String where, LatLng latLng) {

    }

    @Override
    public void communicate(Long value) {
        passengerId = value;
    }
}