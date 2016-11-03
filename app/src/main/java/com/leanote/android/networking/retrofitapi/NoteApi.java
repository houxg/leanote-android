package com.leanote.android.networking.retrofitapi;


import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.UpdateRet;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;
import rx.Observable;

public interface NoteApi {

    @GET("note/getSyncNotes")
    Observable<List<NoteInfo>> getSyncNotes(@Query("afterUsn") int afterUsn, @Query("maxEntry") int maxEntry);

    @GET("note/getNotes")
    Observable<List<NoteInfo>> getNotes(@Query("notebookId") String notebookId);

    @GET("note/getNoteAndContent")
    Observable<NoteInfo> getNoteAndContent(@Query("noteId") String noteId);

    @POST("note/addNote")
    Observable<NoteInfo> add(@Body NoteInfo note);

    @Multipart
    @POST("note/updateNote")
    Observable<NoteInfo> update(@PartMap Map<String, RequestBody> body, @Part List<MultipartBody.Part> files);

    @POST("note/deleteTrash")
    Observable<UpdateRet> delete(@Query("noteId") String noteId, @Query("usn") int usn);
}
