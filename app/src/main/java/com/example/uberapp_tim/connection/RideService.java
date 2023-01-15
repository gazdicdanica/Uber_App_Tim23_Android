package com.example.uberapp_tim.connection;

import com.example.uberapp_tim.dto.RideDTO;
import com.example.uberapp_tim.model.ride.Rejection;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RideService {

    @PUT("ride/{id}/cancel")
    Call<ResponseBody> cancelRide(@Path("id")Long id, @Body Rejection rejection);

    @GET("ride/{id}")
    Call<RideDTO> getRide(@Path("id")Long id);

    @PUT("ride/{id}/accept")
    Call<ResponseBody> acceptRide(@Path("id") Long id);
}