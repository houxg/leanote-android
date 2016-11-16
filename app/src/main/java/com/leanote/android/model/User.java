package com.leanote.android.model;


import com.google.gson.annotations.SerializedName;
import com.leanote.android.networking.retrofitapi.model.BaseResponse;
import com.raizlabs.android.dbflow.annotation.Column;

public class User extends BaseResponse {

    @SerializedName("UserId")
    String userId = "";
    @SerializedName("Username")
    String userName = "";
    @Column(name = "email")
    @SerializedName("Email")
    String email = "";
    @SerializedName("Verified")
    boolean isVerified;
    @SerializedName("Logo")
    String avatar = "";

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getAvatar() {
        return avatar;
    }
}
