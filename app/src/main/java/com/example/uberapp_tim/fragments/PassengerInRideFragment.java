package com.example.uberapp_tim.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.uberapp_tim.BuildConfig;
import com.example.uberapp_tim.R;
import com.example.uberapp_tim.connection.ServiceUtils;
import com.example.uberapp_tim.connection.WebSocket;
import com.example.uberapp_tim.dto.ReviewDTO;
import com.example.uberapp_tim.dto.RideDTO;
import com.example.uberapp_tim.dto.VehicleLocatingDTO;
import com.example.uberapp_tim.model.message.Panic;
import com.example.uberapp_tim.model.ride.RideStatus;
import com.example.uberapp_tim.receiver.NotificationReceiver;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.Vehicle;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import at.markushi.ui.CircleButton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerInRideFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private LocationManager locationManager;
    String provider;
    LatLng start, end;
    private WebSocket webSocket = new WebSocket();
    private RideDTO rideRespDTO;
    private Long driverId;
    private String rideId;
    CircleButton panic;

    Marker driverMarker;

    public static PassengerInRideFragment newInstance() {
        PassengerInRideFragment mpf = new PassengerInRideFragment();
        return mpf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        start = b.getParcelable("start");
        end = b.getParcelable("finish");
        driverId = Long.valueOf(b.getString("driverID"));
        rideId = b.getString("rideId");
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        subscribeToWebSocket();
    }


    @Override
    public void onResume() {
        super.onResume();
        mMapFragment = SupportMapFragment.newInstance();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.map_container, mMapFragment).commit();
        mMapFragment.getMapAsync(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.map_layout, container, false);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        panic = getActivity().findViewById(R.id.panicBtn);
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.addMarker(new MarkerOptions().position(start).title("Start"));


        mMap.addMarker(new MarkerOptions().position(end).title("Finish"));

        List<LatLng> path = new ArrayList<>();
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(BuildConfig.MAPS_API_KEY).build();

        DirectionsApiRequest req = DirectionsApi.getDirections(
                context,
                start.latitude+","+start.longitude,
                end.latitude+","+ end.longitude);

        try {
            DirectionsResult res = req.await();
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];
                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                            for (com.google.maps.model.LatLng coord1 : coords1) {
                                                path.add(new LatLng(coord1.lat, coord1.lng));
                                            }
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (path.size() > 0) {
            PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.parseColor("#f57804")).width(5);
            mMap.addPolyline(opts);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15));
    }

    @SuppressLint("CheckResult")
    private void subscribeToWebSocket() {

        Gson g = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), format);
            }
        }).create();
        webSocket.stompClient.topic("/ride-passenger/"+getActivity().getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("id", null)).subscribe(topicMessage -> {
            String msg = topicMessage.getPayload();
            rideRespDTO = g.fromJson(msg, RideDTO.class);
            Log.i("Value: ", rideRespDTO.toString());
            if (rideRespDTO.getStatus() == RideStatus.ACTIVE) {

                Log.i("IN Ride", "Da");
                panic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i("PANIC:", "true");
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Please State a Reason");
                        final EditText input = new EditText(getActivity());
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setPadding(30, 30, 30, 30);
                        builder.setView(input);
                        builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String reason = input.getText().toString();
                                Panic panic = new Panic();
                                panic.setReason(reason);

                                String jwt = getActivity().getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                                ServiceUtils.rideService.panicRide("Bearer " + jwt, rideRespDTO.getId(), panic).enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                        if(response.code() == 200) {
                                            getActivity().finish();
                                            getActivity().overridePendingTransition(0, 0);
                                            startActivity(getActivity().getIntent());
                                            getActivity().overridePendingTransition(0, 0);
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
                });
            }

            else if (rideRespDTO.getStatus() == RideStatus.FINISHED) {
                Log.i("usao2", " si");
                webSocket.disconnect();

                Intent i = new Intent(getActivity(), NotificationReceiver.class);
                i.putExtra("title", "Ride finished");
                i.putExtra("text", "Ride is finished. Hope you enjoyed!");
                i.putExtra("channel", "passenger_channel");
                i.putExtra("id", getActivity().getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("id", null));

                Log.d("BEFORE BROADCAS", "");
                getActivity().sendBroadcast(i);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showReviewDialog();
                    }
                });
            }
           else {      // RideStatus.ACCEPTED
                Log.i("koja je ovde:", "a");
            }

        }, throwable -> Log.i("Throwable iz inRide-a: ", throwable.getMessage()));

        webSocket.stompClient.topic("/update-vehicle-location/").subscribe(topicMessage -> {
            String message = topicMessage.getPayload();
            Type listType = new TypeToken<ArrayList<VehicleLocatingDTO>>(){}.getType();
            List<VehicleLocatingDTO> vehicles= g.fromJson(message, listType);

            if(getActivity() != null){
                for(VehicleLocatingDTO v : vehicles){
                    if(!Objects.equals(v.getDriverId(), driverId)){
                        continue;
                    }
                    getActivity().runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    addVehicle(v);
                                }
                            }
                    );

                }
            }

        }, throwable -> Log.wtf("Throwable psng in ride frag update veh:", throwable.getMessage()));

        webSocket.stompClient.topic("/driver-arrived/"+rideId).subscribe(topicMessage -> {
            Intent i = new Intent(getContext(), NotificationReceiver.class);
            i.putExtra("title", "Ride");
            i.putExtra("text", "Driver arrived on departure location");
            i.putExtra("channel", "passenger_channel");
            i.putExtra("id", getActivity().getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("id", null));

            Log.d("BEFORE BROADCAS", "");
            getActivity().sendBroadcast(i);
        }, throwable -> Log.wtf("Driver arrived socket throwable:", throwable.getMessage()));
    }

    private void addVehicle(VehicleLocatingDTO vehicle){
        if (mMap != null) {
            if (vehicle.getRideStatus() == RideStatus.FINISHED && driverMarker == null) {
                Log.d("NE POSTOJI", "FINISHED");
                LatLng location = new LatLng(vehicle.getVehicle().getCurrentLocation().getLatitude(), vehicle.getVehicle().getCurrentLocation().getLongitude());
                driverMarker = mMap.addMarker(
                        new MarkerOptions()
                                .title("Available")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .position(location)
                );
                driverMarker.setTag(vehicle.getVehicle().getId());

            }
            else if (vehicle.getRideStatus().equals(RideStatus.ACTIVE) && driverMarker == null) {
                Log.d("NE POSTOJI", "ACTIVE");
                LatLng location = new LatLng(vehicle.getVehicle().getCurrentLocation().getLatitude(), vehicle.getVehicle().getCurrentLocation().getLongitude());
                driverMarker = mMap.addMarker(new MarkerOptions()
                        .title("Busy")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        .position(location)
                );
                driverMarker.setTag(vehicle.getVehicle().getId());
            }
            else if (driverMarker != null && vehicle.getRideStatus().equals(RideStatus.ACTIVE)
                    && this.driverMarker.getTitle().equals("Available")) {
                Log.d("AVAILABLE TO ", "BUSY");
                driverMarker.setTitle("Busy");
                driverMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                LatLng location = new LatLng(vehicle.getVehicle().getCurrentLocation().getLatitude(), vehicle.getVehicle().getCurrentLocation().getLongitude());
                driverMarker.setPosition(location);
            }
            else if (driverMarker != null && vehicle.getRideStatus().equals(RideStatus.FINISHED)
                    && this.driverMarker.getTitle().equals("Busy")) {
                Log.d("BUSY YO", "AVAILABLE");
                driverMarker.setTitle("Active");
                driverMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                LatLng location = new LatLng(vehicle.getVehicle().getCurrentLocation().getLatitude(), vehicle.getVehicle().getCurrentLocation().getLongitude());
                driverMarker.setPosition(location);
            }
            else {
                Log.d("PROMENA", "LOKACIJE");
                LatLng location = new LatLng(vehicle.getVehicle().getCurrentLocation().getLatitude(), vehicle.getVehicle().getCurrentLocation().getLongitude());
                this.driverMarker.setPosition(location);
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(location)
                        .zoom(15)
                        .bearing(0).build();
                CameraUpdate update = CameraUpdateFactory.newCameraPosition(cameraPosition);
                mMap.animateCamera(update);
            }
        }

    }

    public void showReviewDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_review, null);

        RatingBar driverRating = dialogView.findViewById(R.id.driver_ratingbar);
        RatingBar vehicleRating = dialogView.findViewById(R.id.vehicle_ratingbar);
        TextInputEditText driverComment = dialogView.findViewById(R.id.driver_comment);
        TextInputEditText vehicleComment = dialogView.findViewById(R.id.vehicle_comment);

        builder.setView(dialogView);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                getActivity().finish();
                getActivity().overridePendingTransition(0, 0);
                startActivity(getActivity().getIntent());
                getActivity().overridePendingTransition(0, 0);

            }
        });

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean sendDriver = false;
                boolean sendVehicle = false;

                boolean close = true;

                float dRating = driverRating.getRating();
                String dComment = driverComment.getText().toString();
                Log.wtf("COMMENT", dComment);
                Log.wtf("RATINg", String.valueOf(dComment));
                if (!dComment.equals("") && dRating == 0) {
                    Toast.makeText(getActivity(), "You cannot leave a comment without a rating", Toast.LENGTH_SHORT).show();
                    close = false;
                    Log.wtf("CLOSE", String.valueOf(close));
                    return;
                } else {
                    if (dRating > 0) {
                        sendDriver = true;
                    }
                }
                float vRating = vehicleRating.getRating();
                String vComment = vehicleComment.getText().toString();
                if (!vComment.equals("") && vRating == 0) {
                    Toast.makeText(getActivity(), "You cannot leave a comment without a rating", Toast.LENGTH_SHORT).show();
                    close = false;
                    return;
                } else {
                    if (vRating > 0) {
                        sendVehicle = true;
                    }
                }
                String jwt = "Bearer " + getActivity().getSharedPreferences("AirRide_preferences", Context.MODE_PRIVATE).getString("accessToken", "");
                if (sendVehicle) {
                    ServiceUtils.reviewService.createReviewVehicle(jwt, rideRespDTO.getId(), new ReviewDTO((int) Math.round(dRating), dComment, null)).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {

                        }
                    });
                }
                if (sendDriver) {
                    ServiceUtils.reviewService.createReviewDriver(jwt, rideRespDTO.getId(), new ReviewDTO((int) Math.round(dRating), dComment, null)).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {

                        }
                    });
                    Log.wtf("KRAJNJI CLOSE", String.valueOf(close));
                    if (close) dialog.cancel();
                    //else dialog stays open. Make sure you have an obvious way to close the dialog especially if you set cancellable to false.
                }
            }

        });

    }
}

