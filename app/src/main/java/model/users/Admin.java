package model.users;

import android.graphics.Bitmap;

public class Admin extends User{
    public Admin(int id, String name, String email, String lastName, String phoneNumber, String address, String password, Bitmap profilePhoto, boolean blocked){
        super(id, name, lastName, email, phoneNumber, address, password, profilePhoto, blocked);
    }
}