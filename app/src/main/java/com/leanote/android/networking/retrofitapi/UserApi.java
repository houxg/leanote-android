package com.leanote.android.networking.retrofitapi;


import com.leanote.android.networking.retrofitapi.model.BaseResponse;
import com.leanote.android.networking.retrofitapi.model.SyncState;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserApi {

    @GET("user/updateUsername")
    Call<BaseResponse> updateUsername(@Query("username") String username);

    @GET("user/updatePwd")
    Call<BaseResponse> updatePwd(@Query("oldPwd") String oldPwd, @Query("pwd") String pwd);

    @GET("user/getSyncState")
    Call<SyncState> getSyncState();
}
