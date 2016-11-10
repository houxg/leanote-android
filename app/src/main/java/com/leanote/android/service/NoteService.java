package com.leanote.android.service;


import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
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

import org.bson.types.ObjectId;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                for (NoteInfo noteMeta : notes) {
                    NoteInfo remoteNote = RetrofitUtils.excute(getNoteByServerId(noteMeta.getNoteId()));
                    if (remoteNote == null) {
                        return false;
                    }
                    NoteInfo localNote = AppDataBase.getNoteByServerId(noteMeta.getNoteId());
                    noteUsn = remoteNote.getUsn();
                    long localId;
                    if (localNote == null) {
                        localId = remoteNote.insert();
                        remoteNote.setId(localId);
                        Log.i(TAG, "note insert, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId() + ", local=" + localId);
                    } else {
                        if (localNote.isDirty()) {
                            Log.w(TAG, "note conflict, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                            continue;
                        } else {
                            Log.i(TAG, "note update, usn=" + remoteNote.getUsn() + ", id=" + remoteNote.getNoteId());
                            remoteNote.setId(localNote.getId());
                            localId = localNote.getId();
                        }
                    }
                    remoteNote.setIsDirty(false);
                    if (remoteNote.isMarkDown()) {
                        remoteNote.setContent(convertToLocalImageLinkForMD(localId, remoteNote.getContent()));
                    } else {
                        remoteNote.setContent(convertToLocalImageLinkForRichText(localId, remoteNote.getContent()));
                    }
                    Log.i(TAG, "content=" + remoteNote.getContent());
                    remoteNote.update();
                    handleFile(localId, remoteNote.getNoteFiles());
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

    private static void handleFile(long noteLocalId, List<NoteFile> remoteFiles) {
        if (CollectionUtils.isEmpty(remoteFiles)) {
            return;
        }
        Log.i(TAG, "file size=" + remoteFiles.size());
        List<String> excepts = new ArrayList<>();
        for (NoteFile remote : remoteFiles) {
            NoteFile local = AppDataBase.getNoteFileByServerId(remote.getServerId());
            if (local != null) {
                Log.i(TAG, "has local file, id=" + remote.getServerId());
                remote.setLocalId(local.getLocalId());
                remote.setLocalPath(local.getLocalPath());
            } else {
                Log.i(TAG, "need to insert, id=" + remote.getServerId());
                remote.setLocalId(new ObjectId().toString());
            }
            remote.setNoteId(noteLocalId);
            remote.setIsDraft(false);
            remote.save();
            excepts.add(remote.getLocalId());
        }
        Log.i(TAG, "delete exclude=" + new Gson().toJson(excepts));
        AppDataBase.deleteFileExcept(noteLocalId, excepts);
    }

    public static String replace(String content, String tagExp, String targetExp, Replacer replacer, Object... extraData) {
        Pattern tagPattern = Pattern.compile(tagExp);
        Pattern targetPattern = Pattern.compile(targetExp);
        Matcher tagMather = tagPattern.matcher(content);
        StringBuilder contentBuilder = new StringBuilder(content);
        int offset = 0;
        while (tagMather.find()) {
            String tag = tagMather.group();
            Matcher targetMatcher = targetPattern.matcher(tag);
            if (!targetMatcher.find()) {
                continue;
            }
            String original = targetMatcher.group();
            int originalLen = original.length();
            String modified = replacer.replaceWith(original, extraData);
            contentBuilder.replace(tagMather.start() + targetMatcher.start() + offset,
                    tagMather.end() - (tag.length() - targetMatcher.end()) + offset,
                    modified);
            offset += modified.length() - originalLen;
        }
        return contentBuilder.toString();
    }

    public interface Replacer {
        String replaceWith(String original, Object... extraData);
    }

    private static String convertToLocalImageLinkForRichText(long noteLocalId, String noteContent) {
        return replace(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                "\\ssrc\\s*=\\s*\"https://leanote.com/api/file/getImage\\?fileId=.*?\"",
                new Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Log.i(TAG, "in=" + original);
                        Uri linkUri = Uri.parse(original.substring(6, original.length() - 1));
                        String serverId = linkUri.getQueryParameter("fileId");
                        NoteFile noteFile = AppDataBase.getNoteFileByServerId(serverId);
                        if (noteFile == null) {
                            noteFile = new NoteFile();
                            noteFile.setNoteId((Long) extraData[0]);
                            noteFile.setLocalId(new ObjectId().toString());
                            noteFile.setServerId(serverId);
                            noteFile.save();
                        }
                        String localId = noteFile.getLocalId();
                        String result = String.format(Locale.US, " src=\"%s\"", NoteFileService.getLocalImageUri(localId).toString());
                        Log.i(TAG, "out=" + result);
                        return result;
                    }
                }, noteLocalId);
    }

    private static String convertToLocalImageLinkForMD(long noteLocalId, String noteContent) {
        return replace(noteContent,
                "!\\[.*?\\]\\(https://leanote.com/api/file/getImage\\?fileId=.*?\\)",
                "\\(https://leanote.com/api/file/getImage\\?fileId=.*?\\)",
                new Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String serverId = linkUri.getQueryParameter("fileId");
                        NoteFile noteFile = AppDataBase.getNoteFileByServerId(serverId);
                        if (noteFile == null) {
                            noteFile = new NoteFile();
                            noteFile.setNoteId((Long) extraData[0]);
                            noteFile.setLocalId(new ObjectId().toString());
                            noteFile.setServerId(serverId);
                            noteFile.save();
                        }
                        String localId = noteFile.getLocalId();
                        return String.format(Locale.US, "(%s)", NoteFileService.getLocalImageUri(localId).toString());
                    }
                }, noteLocalId);
    }

    public static boolean updateNote(final NoteInfo modifiedNote) {
        NoteInfo note;
        if (modifiedNote.getUsn() == 0) {
            note = RetrofitUtils.excute(addNote(modifiedNote));
        } else {
            NoteInfo remoteNote = RetrofitUtils.excute(getNoteByServerId(modifiedNote.getNoteId()));
            if (remoteNote == null) {
                return false;
            }
            note = RetrofitUtils.excute(updateNote(remoteNote, modifiedNote));
        }
        if (note == null) {
            return false;
        }
        if (note.isOk()) {
            note.setId(modifiedNote.getId());
            note.setNoteBookId(modifiedNote.getNoteBookId());
            note.setIsDirty(false);
            note.setContent(modifiedNote.getContent());
            handleFile(modifiedNote.getId(), note.getNoteFiles());
            note.save();
            if (note.getUsn() - modifiedNote.getUsn() == 1) {
                Log.d(TAG, "update usn=" + note.getUsn());
                LeanoteDbManager.getInstance().updateAccountUsn(note.getUsn(), AccountHelper.getDefaultAccount().getUserId());
            }
        } else {
            throw new IllegalArgumentException(note.getMsg());
        }
        return true;
    }

    private static String convertToServerImageLinkForMD(String noteContent) {
        return replace(noteContent,
                "!\\[.*?\\]\\(file:/getImage\\?id=.*?\\)",
                "\\(file:/getImage\\?id=.*?\\)",
                new Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(1, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        String serverId = NoteFileService.convertFromLocalIdToServerId(localId);
                        return String.format(Locale.US, "(%s)", NoteFileService.getServerImageUri(serverId).toString());
                    }
                });
    }

    private static String convertToServerImageLinkForRichText(String noteContent) {
        return replace(noteContent,
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>",
                "\\ssrc\\s*=\\s*\"file:/getImage\\?id=.*?\"",
                new Replacer() {
                    @Override
                    public String replaceWith(String original, Object... extraData) {
                        Uri linkUri = Uri.parse(original.substring(6, original.length() - 1));
                        String localId = linkUri.getQueryParameter("id");
                        String serverId = NoteFileService.convertFromLocalIdToServerId(localId);
                        return String.format(Locale.US, " src=\"%s\"", NoteFileService.getServerImageUri(serverId).toString());
                    }
                });
    }

    private static Call<List<NoteInfo>> getSyncNotes(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNoteApi().getSyncNotes(afterUsn, maxEntry);
    }

    private static Call<List<NotebookInfo>> getSyncNotebooks(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNotebookApi().getSyncNotebooks(afterUsn, maxEntry);
    }

    public static Call<NoteInfo> getNoteByServerId(String serverId) {
        return ApiProvider.getInstance().getNoteApi().getNoteAndContent(serverId);
    }

    public static boolean revertNote(String serverId) {
        NoteInfo serverNote = RetrofitUtils.excute(NoteService.getNoteByServerId(serverId));
        if (serverNote == null) {
            return false;
        }
        NoteInfo localNote = AppDataBase.getNoteByServerId(serverId);
        long localId;
        if (localNote == null) {
            localId = serverNote.insert();
        } else {
            localId = localNote.getId();
        }
        serverNote.setId(localId);
        if (serverNote.isMarkDown()) {
            serverNote.setContent(convertToLocalImageLinkForMD(localId, serverNote.getContent()));
        } else {
            serverNote.setContent(convertToLocalImageLinkForRichText(localId, serverNote.getContent()));
        }
        handleFile(localId, serverNote.getNoteFiles());
        serverNote.save();
        return true;
    }

    public static Call<NoteInfo> addNote(NoteInfo note) {
        List<MultipartBody.Part> fileBodies = new ArrayList<>();

        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        String content = note.getContent();
        if (note.isMarkDown()) {
            content = convertToServerImageLinkForMD(content);
        } else {
            content = convertToServerImageLinkForRichText(content);
        }
        requestBodyMap.put("NotebookId", createPartFromString(note.getNoteBookId()));
        requestBodyMap.put("Title", createPartFromString(note.getTitle()));
        requestBodyMap.put("Content", createPartFromString(content));
        requestBodyMap.put("IsMarkdown", createPartFromString(getBooleanString(note.isMarkDown())));
        requestBodyMap.put("IsBlog", createPartFromString(getBooleanString(note.isPublicBlog())));
        requestBodyMap.put("CreatedTime", createPartFromString(getTime(System.currentTimeMillis())));
        requestBodyMap.put("UpdatedTime", createPartFromString(getTime(System.currentTimeMillis())));

        List<NoteFile> files = AppDataBase.getAllRelatedFile(note.getId());
        if (CollectionUtils.isNotEmpty(files)) {
            int size = files.size();
            for (int index = 0; index < size; index++) {
                NoteFile noteFile = files.get(index);
                requestBodyMap.put(String.format("Files[%s][LocalFileId]", index), createPartFromString(noteFile.getLocalId()));
                requestBodyMap.put(String.format("Files[%s][IsAttach]", index), createPartFromString(getBooleanString(noteFile.isAttach())));
                requestBodyMap.put(String.format("Files[%s][FileId]", index), createPartFromString(StringUtils.notNullStr(noteFile.getServerId())));
                boolean shouldUploadFile = TextUtils.isEmpty(noteFile.getServerId());
                requestBodyMap.put(String.format("Files[%s][HasBody]", index), createPartFromString(getBooleanString(shouldUploadFile)));
                if (shouldUploadFile) {
                    fileBodies.add(createFilePart(noteFile));
                }
            }
        }
        return ApiProvider.getInstance().getNoteApi().add(requestBodyMap, fileBodies);
    }

    private static String getTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return String.format(Locale.US, "%d-%d-%d %d:%d:%d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }

    private static Call<NoteInfo> updateNote(NoteInfo original, NoteInfo modified) {
        List<MultipartBody.Part> fileBodies = new ArrayList<>();

        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        String noteId = original.getNoteId();
        String content = modified.getContent();
        if (modified.isMarkDown()) {
            content = convertToServerImageLinkForMD(content);
        } else {
            content = convertToServerImageLinkForRichText(content);
        }
        requestBodyMap.put("NoteId", createPartFromString(noteId));
        requestBodyMap.put("NotebookId", createPartFromString(modified.getNoteBookId()));
        requestBodyMap.put("Usn", createPartFromString(String.valueOf(original.getUsn())));
        requestBodyMap.put("Title", createPartFromString(modified.getTitle()));
        requestBodyMap.put("Content", createPartFromString(content));
        requestBodyMap.put("IsMarkdown", createPartFromString(getBooleanString(modified.isMarkDown())));
        requestBodyMap.put("IsBlog", createPartFromString(getBooleanString(modified.isPublicBlog())));
        requestBodyMap.put("UpdatedTime", createPartFromString(getTime(System.currentTimeMillis())));

        List<NoteFile> files = AppDataBase.getAllRelatedFile(modified.getId());
        if (CollectionUtils.isNotEmpty(files)) {
            int size = files.size();
            for (int index = 0; index < size; index++) {
                NoteFile noteFile = files.get(index);
                requestBodyMap.put(String.format("Files[%s][LocalFileId]", index), createPartFromString(noteFile.getLocalId()));
                requestBodyMap.put(String.format("Files[%s][IsAttach]", index), createPartFromString(getBooleanString(noteFile.isAttach())));
                requestBodyMap.put(String.format("Files[%s][FileId]", index), createPartFromString(StringUtils.notNullStr(noteFile.getServerId())));
                boolean shouldUploadFile = TextUtils.isEmpty(noteFile.getServerId());
                requestBodyMap.put(String.format("Files[%s][HasBody]", index), createPartFromString(getBooleanString(shouldUploadFile)));
                if (shouldUploadFile) {
                    fileBodies.add(createFilePart(noteFile));
                }
            }
        }

        if (!original.getNoteBookId().equals(modified.getNoteBookId())) {
            requestBodyMap.put("NotebookId", createPartFromString(modified.getNoteBookId()));
        }

        if (original.isTrash() != modified.isTrash()) {
            requestBodyMap.put("IsTrash", createPartFromString(getBooleanString(modified.isTrash())));
        }
        return ApiProvider.getInstance().getNoteApi().update(requestBodyMap, fileBodies);
    }

    public static Call<UpdateRet> deleteNote(String noteId, int usn) {
        return ApiProvider.getInstance().getNoteApi().delete(noteId, usn);
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
