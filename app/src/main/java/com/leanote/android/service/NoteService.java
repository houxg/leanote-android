package com.leanote.android.service;


import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.leanote.android.db.AppDataBase;
import com.leanote.android.db.LeanoteDbManager;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteFile;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.model.UpdateRet;
import com.leanote.android.networking.retrofitapi.ApiProvider;
import com.leanote.android.networking.retrofitapi.RetrofitUtils;
import com.leanote.android.util.CollectionUtils;
import com.leanote.android.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

public class NoteService {

    private static final String TAG = "NoteService";
    private static final String TRUE = "1";
    private static final String FALSE = "0";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final int MAX_ENTRY = 20;


    public static boolean fetchFromServer() {
        int noteUsn = AccountHelper.getDefaultAccount().getLastSyncUsn();
        int notebookUsn = noteUsn;
        List<NoteInfo> notes;
        do {
            notes = RetrofitUtils.excute(getSyncNotes(noteUsn, MAX_ENTRY));
            if (notes != null) {
                for (NoteInfo remoteNote : notes) {
                    NoteInfo localNote = AppDataBase.getNoteByServerId(remoteNote.getNoteId());
                    //TODO: add convert to local protocol link
                    if (localNote == null) {
                        Log.i(TAG, "note insert, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                        remoteNote.insert();
                    } else {
                        //FIXME: handle files
                        if (localNote.isDirty()) {
                            Log.w(TAG, "note conflict, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                        } else {
                            Log.i(TAG, "note update, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                            remoteNote.setId(localNote.getId());
                            remoteNote.setIsDirty(false);
                            remoteNote.update();
                        }
                    }
                    noteUsn = remoteNote.getUsn();
                }
            } else {
                return false;
            }
        } while (notes.size() == MAX_ENTRY);

        List<NotebookInfo> notebooks;
        do {
            notebooks = RetrofitUtils.excute(getSyncNotebooks(notebookUsn, MAX_ENTRY));
            if (notebooks != null) {
                for (NotebookInfo remoteNotebook : notebooks) {
                    NotebookInfo localNotebook = AppDataBase.getNotebookByServerId(remoteNotebook.getNotebookId());
                    if (localNotebook == null) {
                        Log.i(TAG, "notebook insert, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                        remoteNotebook.insert();
                    } else {
                        if (localNotebook.isDirty()) {
                            Log.w(TAG, "notebook conflict, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                        } else {
                            Log.i(TAG, "notebook update, usn=" + remoteNotebook.getUsn() + ", id=" + remoteNotebook.getNotebookId());
                            remoteNotebook.setId(localNotebook.getId());
                            remoteNotebook.setIsDirty(false);
                            remoteNotebook.update();
                        }
                    }
                    notebookUsn = remoteNotebook.getUsn();
                }
            } else {
                return false;
            }
        } while (notebooks.size() == MAX_ENTRY);

        Log.i(TAG, "noteUsn=" + noteUsn + ", notebookUsn=" + notebookUsn);
        int max = Math.max(notebookUsn, noteUsn);
        LeanoteDbManager.getInstance().updateAccountUsn(max, AccountHelper.getDefaultAccount().getUserId());
        return true;
    }

    public static Call<List<NoteInfo>> getSyncNotes(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNoteApi().getSyncNotes(afterUsn, maxEntry);
    }

    public static Call<List<NotebookInfo>> getSyncNotebooks(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNotebookApi().getSyncNotebooks(afterUsn, maxEntry);
    }

    public static Call<NoteInfo> getNoteByServerId(String serverId) {
        return ApiProvider.getInstance().getNoteApi().getNoteAndContent(serverId);
    }

    public static Call<NoteInfo> addNote(NoteInfo noteInfo) {
        return ApiProvider.getInstance().getNoteApi().add(noteInfo);
    }

    public static Call<NoteInfo> updateNote(NoteInfo original, NoteInfo modified) {
        List<MultipartBody.Part> fileBodies = new ArrayList<>();

        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        String noteId = original.getNoteId();
        requestBodyMap.put("NoteId", createPartFromString(noteId));
        requestBodyMap.put("Usn", createPartFromString(String.valueOf(original.getUsn())));
        requestBodyMap.put("IsMarkdown", createPartFromString(getBooleanString(modified.isMarkDown())));

        List<NoteFile> files = NoteFileService.getAllRelatedFile(noteId);
        if (CollectionUtils.isNotEmpty(files)) {
            int size = files.size();
            for (int index = 0; index < size; index++) {
                NoteFile noteFile = files.get(index);
                requestBodyMap.put(String.format("Files[%s][LocalFileId]", index), createPartFromString(noteFile.getLocalId()));
                requestBodyMap.put(String.format("Files[%s][IsAttach]", index), createPartFromString(getBooleanString(noteFile.isAttach())));
                requestBodyMap.put(String.format("Files[%s][FileId]", index), createPartFromString(StringUtils.notNullStr(noteFile.getServerId())));
                boolean shouldUploadFile = TextUtils.isEmpty(noteFile.getServerId());
                requestBodyMap.put(String.format("Files[%s][HasBody]", index), createPartFromString(getBooleanString(!shouldUploadFile)));
                if (shouldUploadFile) {
                    fileBodies.add(createFilePart(noteFile));
                }
            }
        }

        if (!original.getNoteBookId().equals(modified.getNoteBookId())) {
            requestBodyMap.put("NotebookId", createPartFromString(modified.getNoteBookId()));
        }
        if (!original.getTitle().equals(modified.getTitle())) {
            requestBodyMap.put("Title", createPartFromString(modified.getTitle()));
        }
        if (!original.getContent().equals(modified.getContent())) {
            requestBodyMap.put("Content", createPartFromString(modified.getContent()));
        }
        if (original.isTrash() != modified.isTrash()) {
            requestBodyMap.put("IsTrash", createPartFromString(getBooleanString(modified.isTrash())));
        }
        return ApiProvider.getInstance().getNoteApi().update(requestBodyMap, fileBodies);
    }

    public static Call<UpdateRet> deleteNote(NoteInfo note) {
        return ApiProvider.getInstance().getNoteApi().delete(note.getNoteId(), note.getUsn());
    }

    private static RequestBody createPartFromString(String content) {
        return RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);
    }

    private static String getBooleanString(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    private static MultipartBody.Part createFilePart(NoteFile noteFile) {
        File tempFile;
        try {
            tempFile = new File(noteFile.getLocalPath());
            if (!tempFile.isFile()) {
                Log.w(TAG, "not a file");
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        String extension = MimeTypeMap.getFileExtensionFromUrl(tempFile.toURI().toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), tempFile);
        return MultipartBody.Part.createFormData(String.format("FileDatas[%s]", noteFile.getLocalId()), tempFile.getName(), fileBody);
    }
}
