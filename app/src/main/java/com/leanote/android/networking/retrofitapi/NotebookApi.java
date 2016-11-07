package com.leanote.android.networking.retrofitapi;


import com.leanote.android.model.NotebookInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NotebookApi {

    @GET("notebook/getSyncNotebooks")
    Call<List<NotebookInfo>> getSyncNotebooks(@Query("afterUsn") int afterUsn, @Query("maxEntry") int maxEntry);

    @GET("notebook/getNotebooks")
    Call<List<NotebookInfo>> getNotebooks();

    @POST("notebook/addNotebook")
    Call<NotebookInfo> addNotebook(@Query("title") String title, @Query("parentNotebookId") String parentId);
}
