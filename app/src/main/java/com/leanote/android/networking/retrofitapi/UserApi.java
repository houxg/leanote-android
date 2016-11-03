package com.leanote.android.networking.retrofitapi;


import com.leanote.android.networking.retrofitapi.model.BaseResponse;
import com.leanote.android.networking.retrofitapi.model.SyncState;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface UserApi {

    @GET("user/updateUsername")
    Observable<BaseResponse> updateUsername(@Query("username") String username);

    @GET("user/updatePwd")
    Observable<BaseResponse> updatePwd(@Query("oldPwd") String oldPwd, @Query("pwd") String pwd);

    @GET("user/getSyncState")
    Observable<SyncState> getSyncState();
}
