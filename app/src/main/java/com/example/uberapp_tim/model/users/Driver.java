package com.example.uberapp_tim.model.users;

import android.graphics.Bitmap;

import java.io.Serializable;

public class Driver extends User implements Serializable {
    private Bitmap driverLicence;
    private Bitmap registration;
    private boolean isActive;

    public Driver(Long id, String name, String lastName,String email, String phoneNumber,
                  String address, String password, String profilePhoto, boolean blocked,
                  Bitmap driverLicence, Bitmap registration, boolean isActive){

        super(id, name, lastName, email, phoneNumber, address, password, profilePhoto, blocked);
        this.driverLicence = driverLicence;
        this.registration = registration;
        this.isActive = isActive;
    }

    public Bitmap getRegistration() {
        return registration;
    }

    public void setRegistration(Bitmap registration) {
        this.registration = registration;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Bitmap getDriverLicence() {
        return driverLicence;
    }

    public void setDriverLicence(Bitmap driverLicence) {
        this.driverLicence = driverLicence;
    }

}
