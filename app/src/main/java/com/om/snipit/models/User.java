package com.om.snipit.models;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private String full_name;
    private String email_address;
    private String photo_url;

    public User() {
    }

    public User(Parcel in) {
        full_name = in.readString();
        email_address = in.readString();
        photo_url = in.readString();
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(full_name);
        out.writeString(email_address);
        out.writeString(photo_url);
    }
}
