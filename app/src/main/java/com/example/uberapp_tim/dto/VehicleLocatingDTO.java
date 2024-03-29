package com.example.uberapp_tim.dto;

import com.example.uberapp_tim.model.ride.RideStatus;
import com.example.uberapp_tim.model.vehicle.Vehicle;

public class VehicleLocatingDTO {
    private Long driverId;
    private String driverEmail;
    private Vehicle vehicle;
    private RideStatus rideStatus;
    private String duration;


    public VehicleLocatingDTO(Long driverId, String driverEmail, Vehicle vehicle, RideStatus rideStatus, String duration) {
        this.driverId = driverId;
        this.driverEmail = driverEmail;
        this.vehicle = vehicle;
        this.rideStatus = rideStatus;
        this.duration = duration;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getDriverEmail() {
        return driverEmail;
    }

    public void setDriverEmail(String driverEmail) {
        this.driverEmail = driverEmail;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public RideStatus getRideStatus() {
        return rideStatus;
    }

    public void setRideStatus(RideStatus rideStatus) {
        this.rideStatus = rideStatus;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
