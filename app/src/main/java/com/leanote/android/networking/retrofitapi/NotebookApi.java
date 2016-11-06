package com.leanote.android.networking.retrofitapi;


import com.leanote.android.model.NotebookInfo;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

public interface NotebookApi {

    @GET("notebook/getSyncNotebooks")
    Observable<List<NotebookInfo>> getSyncNotebooks(@Query("afterUsn") int afterUsn, @Query("maxEntry") int maxEntry);

    @GET("notebook/getNotebooks")
    Observable<List<NotebookInfo>> getNotebooks();

    @POST("notebook/addNotebook")
    Observable<NotebookInfo> addNotebook(@Query("title") String title, @Query("parentNotebookId") String parentId);
}
