package com.example.uberapp_tim.activities.passenger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uberapp_tim.R;
import com.example.uberapp_tim.activities.InRideChatActivity;
import com.example.uberapp_tim.connection.ServiceUtils;
import com.example.uberapp_tim.connection.WebSocket;
import com.example.uberapp_tim.dto.RideDTO;
import com.example.uberapp_tim.dto.RideRequestDTO;
import com.example.uberapp_tim.dto.UserShortDTO;
import com.example.uberapp_tim.fragments.DrawRouteFragment;
import com.example.uberapp_tim.fragments.MapFragment;
import com.example.uberapp_tim.fragments.PassengerInRideFragment;
import com.example.uberapp_tim.model.message.Panic;
import com.example.uberapp_tim.model.ride.Ride;
import com.example.uberapp_tim.model.ride.RideStatus;
import com.example.uberapp_tim.model.route.Location;
import com.example.uberapp_tim.model.route.Route;
import com.example.uberapp_tim.model.vehicle.CarType;
import com.example.uberapp_tim.receiver.NotificationReceiver;
import com.example.uberapp_tim.service.FragmentToActivity;
import com.example.uberapp_tim.tools.FragmentTransition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.maps.model.Duration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import at.markushi.ui.CircleButton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PassengerMainActivity extends AppCompatActivity implements View.OnClickListener, FragmentToActivity, AdapterView.OnItemSelectedListener {

    private static final DecimalFormat dfSharp = new DecimalFormat("#.##");
    private Route route = new Route();
    BottomNavigationView passengerNav;
    Button btnTimePicker, linkPassengerBtn;
    TextInputLayout ly1, ly2, l3;
    TextInputEditText txtTime, distanceTxt, estimateTxt, priceTxt, linkPsngr, start, finish;
    private int hour, minute, hour1=0, minute1=0;
    LocalDateTime scheduledTime = null;
    ToggleButton pets, babies;
    Spinner carType;
    CircleButton requestRideBtn, panicBtn, chatBtn;
    private com.example.uberapp_tim.model.route.Location startLocation = null, endLocation = null;
    private float distance=0;
    private double duration;
    private boolean isClicked = false, textInputted=false, notified5First=false, notified15=false, notified5Second=false;
    String startLoc="", finishLoc="", timeData="", carTypeSelected = "STANDARD", sLocation="", fLocation="";
    LatLng s, f;
    List<String> invitedPeople = new ArrayList<>();
    private boolean isus = false;

    public static WebSocket webSocket = new WebSocket();
    AlertDialog alertDialog = null, acceptRideDialog = null;

    private RideRequestDTO rideDTO = new RideRequestDTO();
    private Ride rideResp;
    private RideDTO rideRespDTO = null, dtoForDialog;
    MapFragment mapFragment;
    DrawRouteFragment drawRouteFragment;

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

        createNotificationChannel();

        setContentView(R.layout.passenger_main_activity);
        subscribeToWebSocket();
        btnTimePicker = findViewById(R.id.btnTimePicker);
        btnTimePicker.setOnClickListener(this);
        requestRideBtn = findViewById(R.id.requestRide);
        requestRideBtn.setOnClickListener(this);
        panicBtn = findViewById(R.id.panicBtn);
        panicBtn.setOnClickListener(this);
        chatBtn = findViewById(R.id.msgBtn);
        chatBtn.setOnClickListener(this);
        linkPassengerBtn = findViewById(R.id.addFriendsBtn);
        linkPassengerBtn.setOnClickListener(this);

        ly1 = findViewById(R.id.distLayout);
        ly2 = findViewById(R.id.timeLayout);
        l3 = findViewById(R.id.priceLay);
        priceTxt = findViewById(R.id.price);

        txtTime = findViewById(R.id.time);
        linkPsngr = findViewById(R.id.addFriends);
        distanceTxt = findViewById(R.id.distance);
        estimateTxt = findViewById(R.id.timeTctLayout);
        start = findViewById(R.id.startLocation);
        finish = findViewById(R.id.whereTo);
//        onTextChanged();

        carType = findViewById(R.id.carType);
        carType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CarType.values()));


        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        babies = findViewById(R.id.babiesInRide);
        pets = findViewById(R.id.petsInRide);


        passengerNav = findViewById(R.id.passengerNav);
        passengerNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent i;
                switch (item.getItemId()) {

                    case (R.id.action_main):
                        return true;
                    case (R.id.action_account):
                        i = new Intent(PassengerMainActivity.this, PassengerAccountActivity.class);
                        startActivity(i);
                        overridePendingTransition(0,0);
                        return true;
                    case (R.id.action_inbox):
                        i = new Intent(PassengerMainActivity.this, PassengerInboxActivity.class);
                        startActivity(i);
                        return true;
                    case (R.id.action_reports):
                        i = new Intent(PassengerMainActivity.this, PassengerReportsActivity.class);
                        startActivity(i);
                        return true;
                }
                return false;
            }
        });
    }

    private void createNotificationChannel(){
        NotificationChannel channelPassenger = new NotificationChannel("passenger_channel", "Passenger notifications", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channelPassenger);

        NotificationChannel channelRide = new NotificationChannel("in_ride_channel", "In ride notifications", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channelRide);
    }

    @Override
    public void communicate(String s) {
        String id = s.split(",")[0];
        String val = s.split(",")[1];
        if (id.equals("start")) {
            startLoc = val;
            start.setText(val.split(",")[0]);
        } else if (id.equals("finish")) {
            finishLoc = val;
            finish.setText(val.split(",")[0]);
        }
    }

    @Override
    public void sendStartLocation(com.example.uberapp_tim.model.route.Location location) {
        startLocation = location;
    }

    @Override
    public void sendFinishLocation(Location location) {
        endLocation = location;
    }

    @Override
    public void sendRideData(float distance, Duration duration) {
        timeData = duration.humanReadable;
        this.duration = (double) duration.inSeconds/60;
        this.distance = distance;

        String txt = timeData+"";
        estimateTxt.setText(txt);

        txt = dfSharp.format(this.distance)+"km";
        distanceTxt.setText(txt);

        txt = (int) estimatePrice()+"RSD";
        priceTxt.setText(txt);
    }

    @Override
    public void saveLatLng(String where, LatLng latLng) {
        if (where.equals("s")) {
            s = latLng;
        } else {
            f = latLng;
        }
    }

    @Override
    public void communicate(Long value) {

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
        mapFragment = MapFragment.newInstance();
        FragmentTransition.to(mapFragment, this, false);
        passengerNav.setSelectedItemId(R.id.action_main);
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

    @Override
    public void onClick(View view) {
        if(view == btnTimePicker) {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {

                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfDay) {

                    final Calendar c = Calendar.getInstance();
                    hour = c.get(Calendar.HOUR_OF_DAY);
                    minute = c.get(Calendar.MINUTE);
                    hour1 = hourOfDay;
                    minute1 = minuteOfDay;
                    String txt = hourOfDay + ":" + minuteOfDay;
                    LocalDateTime now = LocalDateTime.now();
                    scheduledTime = now.plusHours(hour1 - hour).plusMinutes(minute1 - minute);
                    Log.i("VREME IZ PICKERA: ", scheduledTime.toString());
                    txtTime.setText(txt);
                }}, hour, minute, true);
            timePickerDialog.show();
        }
        else if (view == requestRideBtn) {
            if (start != null && finish != null) {
                Log.i("ADRS1:", start.getText().toString());
                Log.i("ADRS2:", finish.getText().toString());
                s = getLocationFromAddress(start.getText().toString());
                f = getLocationFromAddress(finish.getText().toString());
                if (endLocation == null || startLocation == null) {
                    endLocation = new Location(f.longitude, f.latitude);
                    startLocation = new Location(s.longitude, s.latitude);
                    startLoc = start.getText().toString();
                    finishLoc = finish.getText().toString();
                }
                Log.i("Star:", s.toString());
                Log.i("Finish: ", f.toString());
                if (isClicked) {
                    requestRideBtn.setVisibility(View.GONE);
                    createRide();
                    createRoute();
                    this.rideDTO.addRoute(route);
                    Log.i("Ride: ", rideDTO.toString());
                    Log.i("Route: ", route.toString());
                    attemptCreateRide();
                }
                isClicked = true;
                setTextForRide();


                drawRouteFragment = DrawRouteFragment.newInstance();
                FragmentTransition.to(drawRouteFragment, this, false);
            }
        }
        else if (view == linkPassengerBtn) {
            if (invitedPeople.size() <= 4) {
                if (linkPsngr.getText() != null) {
                    String temp = linkPsngr.getText().toString();
                    if (!temp.equals(getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("email", ""))) {
                        String jwt = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                        ServiceUtils.userService.doesUserExist("Bearer " + jwt, temp).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.code() == 200) {
                                    invitedPeople.add(temp);
                                    linkPsngr.setText("");
                                    Toast.makeText(PassengerMainActivity.this, "User Added", Toast.LENGTH_SHORT).show();
                                } else {
                                    linkPsngr.setText("");
                                    Toast.makeText(PassengerMainActivity.this, "User Does Not Exist", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(PassengerMainActivity.this, "Greska 569", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } else {
                Toast.makeText(this, "You Cannot Invite More People", Toast.LENGTH_SHORT).show();
            }
        }
        else if (view == panicBtn) {
            Log.i("PANIC:", "true");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Please State a Reason");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setPadding(30, 30, 30, 30);
            builder.setView(input);
            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String reason = input.getText().toString();
                    Panic panic = new Panic();
                    panic.setReason(reason);
                    String jwt = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                    ServiceUtils.rideService.panicRide("Bearer " + jwt, rideRespDTO.getId(), panic).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if(response.code() == 200) {
                                Toast.makeText(PassengerMainActivity.this, "Submitted, Sorry For Inconvenience", Toast.LENGTH_SHORT).show();
                                finish();
                                overridePendingTransition(0, 0);
                                startActivity(getIntent());
                                overridePendingTransition(0, 0);
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.wtf("panic msg:", t.getMessage());
                        }
                    });

                }
            });

            builder.create().show();
        }
        else if (view == chatBtn) {
            Intent chatIntent = new Intent(PassengerMainActivity.this, InRideChatActivity.class);
            // rideId
            chatIntent.putExtra("rideId", rideRespDTO.getId());
            // userId
            chatIntent.putExtra("userId", rideRespDTO.getDriver().getId());
            startActivity(chatIntent);

            // TODO NA POVRATKU JE INKONZISTENTNO :(((
        }
    }

    private void setTextForRide() {
        linkPsngr.setVisibility(View.GONE);
        linkPassengerBtn.setVisibility(View.GONE);
        btnTimePicker.setVisibility(View.GONE);
        babies.setVisibility(View.GONE);
        pets.setVisibility(View.GONE);
        carType.setVisibility(View.GONE);
        txtTime.setVisibility(View.GONE);

        ly1.setVisibility(View.VISIBLE);
        ly2.setVisibility(View.VISIBLE);
        l3.setVisibility(View.VISIBLE);

        estimateTxt.setVisibility(View.VISIBLE);
        estimateTxt.setEnabled(false);
        distanceTxt.setVisibility(View.VISIBLE);
        distanceTxt.setEnabled(false);
        priceTxt.setVisibility(View.VISIBLE);
        priceTxt.setEnabled(false);
        start.setEnabled(false);
        finish.setEnabled(false);
    }

    public Bundle rideBundle(){
        Bundle bundle = new Bundle();
        bundle.putParcelable("start", this.s);
        bundle.putParcelable("finish", this.f);
        return bundle;

    }

    private void createRide() {
        this.rideDTO.setBabyTransport(babies.isChecked());
        this.rideDTO.setPetTransport(pets.isChecked());
        this.rideDTO.setVehicleType(resolveCarType());
        if (scheduledTime != null) {
            this.rideDTO.setScheduledTime(scheduledTime + "Z");
        }
        this.rideDTO.setPassengers(createPassengerList());
    }

    private List<UserShortDTO> createPassengerList() {
        List<UserShortDTO> li = new ArrayList<>();
        for (String email : invitedPeople) {
            li.add(new UserShortDTO(email));
        }
        return li;
    }

    private void createRoute() {
        startLocation.setAddress(startLoc);
        endLocation.setAddress(finishLoc);
        route.setDeparture(startLocation);
        route.setDestination(endLocation);
        rideDTO.setEstimatedPrice(estimatePrice());
        rideDTO.setEstimatedTime(duration);
        route.setDistance(distance);
        if ((hour1 != 0) && (minute1 != 0)){
            rideDTO.setStartTime(LocalDateTime.now());
        } else {
            rideDTO.setStartTime(null);
        }
        rideDTO.setEndTime(null);

    }

    private float estimatePrice() {
        if (carTypeSelected.equals("STANDARD")) {
            return 300 + 120*distance;
        } else if (carTypeSelected.equals("LUXURY")) {
            return 500 + 120*distance;
        } else {
            return 400 + 12*distance;
        }
    }

    public CarType resolveCarType() {
        CarType type = CarType.STANDARD;
        if (carType.getSelectedItem().toString().equals("LUXURY")) {
            type = CarType.LUXURY;
        } else if (carType.getSelectedItem().toString().equals("VAN")) {
            type = CarType.VAN;
        }
        return type;
    }

    public LatLng getLocationFromAddress(String strAddress) {
        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 2,
                    45.085839, 19.503961,
                    45.423230, 20.108896);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return p1;
    }

    public String getAddressFromLocation(LatLng location) {
        String address="";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if(addresses != null){
                Address returned = addresses.get(0);
                address = returned.getAddressLine(0);
            } else {
                address = "No Address Found";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        carTypeSelected = adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void showWaitingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PassengerMainActivity.this);
        LayoutInflater inflater = PassengerMainActivity.this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.passenger_waiting_dialog, null);

        builder.setView(dialogView);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String jwt = "Bearer " + getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                ServiceUtils.rideService.withdrawRide(jwt, rideResp.getId()).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.code() == 200) {
                            Toast.makeText(PassengerMainActivity.this, "Successful Decline", Toast.LENGTH_SHORT).show();
                            finish();
                            overridePendingTransition(0, 0);
                            startActivity(getIntent());
                            overridePendingTransition(0, 0);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.i("PORUKA FAIL:", t.getMessage());
                    }
                });
            }

        });
        alertDialog = builder.create();
        alertDialog.show();
    }

    @SuppressLint("CheckResult")
    private void subscribeToWebSocket() {
        String id = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("id", null);


        webSocket.stompClient.topic("/ride-passenger/"+ id).subscribe(
                topicMassage -> {
            String message = topicMassage.getPayload();
            Gson g = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), format);
                }
            }).create();
            rideRespDTO = g.fromJson(message, RideDTO.class);
            if (rideRespDTO.getStatus() == RideStatus.ACCEPTED) {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                    Bundle b = rideBundle();
                    b.putString("driverID", String.valueOf(rideRespDTO.getDriver().getId()));
                    b.putString("rideId", String.valueOf(rideRespDTO.getId()));
                    PassengerInRideFragment fragment = PassengerInRideFragment.newInstance();
                    fragment.setArguments(b);

//                    FragmentTransition.remove(this);
                    FragmentTransition.to(fragment, this, true);

                    isus = !isus;
                }
            }
            else if (!isus && rideRespDTO.getStatus() != RideStatus.ACTIVE && rideRespDTO.getStatus() != RideStatus.FINISHED){
                Log.i("ISUS", "da");
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            }
            else{
                
                Log.i("Ovde puca", "da"+rideRespDTO);
            }
        }, throwable -> Log.e("THROWABLE WEBSOCKET: ", throwable.getMessage()));

        webSocket.stompClient.topic("/linkPassengers/" + id).subscribe(
                topicMessage-> {
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("title", "Ride");
            intent.putExtra("text", "You have been linked by a friend to a new ride");
            intent.putExtra("channel", "passenger_channel");
            intent.putExtra("id", id);
            sendBroadcast(intent);
            String rideMessage = topicMessage.getPayload();
            Gson g = null;

            g = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), format);
                }
            }).create();
            dtoForDialog = g.fromJson(rideMessage, RideDTO.class);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(isDestroyed() || isFinishing()){
                        Log.wtf("Hey now, you're", "rock star");
                    } else {
                        showLinkedPassengerDialog();
                    }
                }
            });
        }, throwable -> Log.e("THROWABLE linkPassenger: ", throwable.getMessage()));

        webSocket.stompClient.topic("/scheduledNotifications/"+id).subscribe(
                topicMessage -> {
                    Gson g = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), format);
                        }
                    }).create();

                    RideDTO schedule = g.fromJson(topicMessage.getPayload(), RideDTO.class);
                    Intent intent = new Intent(this, NotificationReceiver.class);
                    intent.putExtra("channel", "passenger_channel");
                    intent.putExtra("id", id);
                    if (schedule.getStatus() == RideStatus.REJECTED) {
                        intent.putExtra("title", "Unsuccessful ride schedule");
                        intent.putExtra("text", "Unfortunately, all drivers are busy.");
                    } else if (schedule.getStatus() == RideStatus.ACCEPTED) {
                        intent.putExtra("title", "Scheduled ride");
                        intent.putExtra("text", "Driver is on his way");
                        dtoForDialog = schedule;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinishing() || isDestroyed()) {
                                    Log.wtf("your blood is my", "wine");
                                } else {
                                    showLinkedPassengerDialog();
                                }
                            }
                        });
                        // TODO dialog koji vodi u ride

                    }
                    sendBroadcast(intent);
                }, throwable -> Log.wtf("Throwable is scheduled:", throwable.getMessage()));

        webSocket.stompClient.topic("/notify15/" + id).subscribe(
                topicMessage -> {
                    if (!notified15) {
                        notified15 = true;
                        Intent intent = new Intent(this, NotificationReceiver.class);
                        intent.putExtra("channel", "passenger_channel");
                        intent.putExtra("id", id);
                        intent.putExtra("title", "Scheduled ride");
                        intent.putExtra("text", "You Have Scheduled Ride In 15 Minutes");
                        sendBroadcast(intent);
                    }
                }, throwable -> Log.e("TROWABLE WEBSOCKET: ", throwable.getMessage()));

        webSocket.stompClient.topic("/notify10/" + id).subscribe(
                topicMessage -> {
                    if (!notified5First) {
                        notified5First = true;
                        Intent intent = new Intent(this, NotificationReceiver.class);
                        intent.putExtra("channel", "passenger_channel");
                        intent.putExtra("id", id);
                        intent.putExtra("title", "Scheduled ride");
                        intent.putExtra("text", "You Have Scheduled Ride In 10 Minutes");
                        sendBroadcast(intent);
                    }
                }, throwable ->  Log.e("TROWABLE WEBSOCKET: ", throwable.getMessage()));

        webSocket.stompClient.topic("/notify5/" + id).subscribe(
                topicMessage -> {
                    if (!notified5Second) {
                        notified5Second = true;
                        Intent intent = new Intent(this, NotificationReceiver.class);
                        intent.putExtra("channel", "passenger_channel");
                        intent.putExtra("id", id);
                        intent.putExtra("title", "Scheduled ride");
                        intent.putExtra("text", "You Have Scheduled Ride In 5 Minutes");
                        sendBroadcast(intent);
                    }
                }, throwable ->  Log.e("TROWABLE WEBSOCKET: ", throwable.getMessage()));
    }

    private void showLinkedPassengerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = PassengerMainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_driver_new_ride, null);

        final EditText startLocation = dialogView.findViewById(R.id.start_location_dialog);
        final EditText endLocation = dialogView.findViewById(R.id.end_location_dialog);
        final EditText time = dialogView.findViewById(R.id.dialog_time);
        final EditText km = dialogView.findViewById(R.id.dialog_kilometers);
        final EditText people = dialogView.findViewById(R.id.person_num_dialog);
        final EditText price = dialogView.findViewById(R.id.price_dialog);


        startLocation.setText(dtoForDialog.getLocations().get(0).getDeparture().getAddress());
        endLocation.setText(dtoForDialog.getLocations().get(0).getDestination().getAddress());
        String value = String.valueOf(dtoForDialog.getEstimatedTimeInMinutes()) + " min";
        time.setText(value);
        System.err.println(dtoForDialog.getLocations().get(0));
        value = String.valueOf(dtoForDialog.getTotalDistance()) + " km";
        km.setText(value);
        people.setText(String.valueOf(dtoForDialog.getPassengers().size()));
        value = String.valueOf(dtoForDialog.getTotalCost()) + " RSD";
        price.setText(value);

        builder.setView(dialogView);
        s = new LatLng(dtoForDialog.getLocations().get(0).getDeparture().getLatitude(),
                dtoForDialog.getLocations().get(0).getDeparture().getLongitude());
        f = new LatLng(dtoForDialog.getLocations().get(0).getDestination().getLatitude(),
                dtoForDialog.getLocations().get(0).getDestination().getLongitude());

        PassengerMainActivity that = this;

        builder.setPositiveButton(R.string.dialog_accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        acceptRideDialog.dismiss();
                        Bundle b = rideBundle();
                        b.putString("driverID", String.valueOf(dtoForDialog.getDriver().getId()));
                        b.putString("rideId", String.valueOf(rideRespDTO.getId()));
                        PassengerInRideFragment fragment = PassengerInRideFragment.newInstance();
                        fragment.setArguments(b);


                        setTextForRide();
                        estimateTxt.setText(String.valueOf(dtoForDialog.getEstimatedTimeInMinutes()));
                        distanceTxt.setText(String.valueOf(dtoForDialog.getTotalDistance()));
                        start.setText(dtoForDialog.getLocations().get(0).getDeparture().getAddress());
                        finish.setText(dtoForDialog.getLocations().get(0).getDestination().getAddress());
                        priceTxt.setText(String.valueOf(dtoForDialog.getTotalCost()));


                        requestRideBtn.setVisibility(View.GONE);
                        panicBtn.setVisibility(View.VISIBLE);
                        chatBtn.setVisibility(View.VISIBLE);
                        FragmentTransition.to(fragment, that, true);
                    }
                });

        acceptRideDialog = builder.create();
        acceptRideDialog.show();
    }

    private void attemptCreateRide() {
        String jwt = getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
        ServiceUtils.rideService.createRide("Bearer " + jwt, this.rideDTO).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    if (rideDTO.getScheduledTime() == null) {       // Voznja odmah
                        Gson g = null;
                        try {
                            String json = response.body().string();
                            g = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {

                                @Override
                                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                                    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                    return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), format);
                                }
                            }).create();
                            rideResp = g.fromJson(json, Ride.class);
                            start.setVisibility(View.GONE);
                            finish.setVisibility(View.GONE);
                            requestRideBtn.setVisibility(View.GONE);

                            showWaitingDialog();
                            panicBtn.setVisibility(View.VISIBLE);
                            chatBtn.setVisibility(View.VISIBLE);



                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {                                        // Voznja zakazivanje
                        if (response.code() == 200) {
                            Toast.makeText(PassengerMainActivity.this, "Scheduling Successful", Toast.LENGTH_SHORT).show();
                            finish();
                            overridePendingTransition(0, 0);
                            startActivity(getIntent());
                            overridePendingTransition(0, 0);
                        }
                    }
                }
                else if (response.code() == 400) {
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(getIntent());
                    overridePendingTransition(0, 0);
                    Toast.makeText(PassengerMainActivity.this, "No Available Vehicles", Toast.LENGTH_SHORT).show();
                    Log.wtf("No Drivers Online", String.valueOf(response));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(PassengerMainActivity.this, "Greska 569", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onTextChanged() {
        start.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        finish.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
}
