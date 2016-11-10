package com.leanote.android.model;


import com.google.gson.annotations.SerializedName;
import com.leanote.android.networking.retrofitapi.model.BaseResponse;

public class UpdateRet extends BaseResponse {
    @SerializedName("Usn")
    int usn;

    public int getUsn() {
        return usn;
    }
}
