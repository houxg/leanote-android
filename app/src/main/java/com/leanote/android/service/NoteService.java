package com.leanote.android.service;


import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.leanote.android.model.NoteFile;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.UpdateRet;
import com.leanote.android.networking.retrofitapi.ApiProvider;
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
import rx.Observable;

public class NoteService {

    private static final String TAG = "NoteService";
    private static final String TRUE = "1";
    private static final String FALSE = "0";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    public static Observable<List<NoteInfo>> getSyncNotes(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNoteApi().getSyncNotes(afterUsn, maxEntry);
    }

    public static Observable<NoteInfo> getNoteByServerId(String serverId) {
        return ApiProvider.getInstance().getNoteApi().getNoteAndContent(serverId);
    }

    public static Observable<NoteInfo> updateNote(NoteInfo original, NoteInfo modified) {
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

    public static Observable<UpdateRet> deleteNote(NoteInfo note) {
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
