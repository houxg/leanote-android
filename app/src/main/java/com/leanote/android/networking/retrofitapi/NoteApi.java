package com.leanote.android.networking.retrofitapi;


import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.UpdateRet;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface NoteApi {

    @GET("note/getSyncNotes")
    Call<List<NoteInfo>> getSyncNotes(@Query("afterUsn") int afterUsn, @Query("maxEntry") int maxEntry);

    @GET("note/getNotes")
    Call<List<NoteInfo>> getNotes(@Query("notebookId") String notebookId);

    @GET("note/getNoteAndContent")
    Call<NoteInfo> getNoteAndContent(@Query("noteId") String noteId);

    @Multipart
    @POST("note/addNote")
    Call<NoteInfo> add(@PartMap Map<String, RequestBody> body, @Part List<MultipartBody.Part> files);

    @Multipart
    @POST("note/updateNote")
    Call<NoteInfo> update(@PartMap Map<String, RequestBody> body, @Part List<MultipartBody.Part> files);

    @POST("note/deleteTrash")
    Call<UpdateRet> delete(@Query("noteId") String noteId, @Query("usn") int usn);
}
