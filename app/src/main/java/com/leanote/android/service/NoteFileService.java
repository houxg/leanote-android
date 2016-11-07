package com.leanote.android.service;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.leanote.android.Leanote;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteFile;

import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

public class NoteFileService {

    private static final String TAG = "NoteFileService";

    private static final String SCHEME = "file";
    private static final String IMAGE_PATH = "getImage";
    private static final String IMAGE_PATH_WITH_SLASH = "/getImage";

    public static String convertFromServerIdToLocalId(String serverId) {
        NoteFile noteFile = AppDataBase.getNoteFileByServerId(serverId);
        if (noteFile == null) {
            noteFile = new NoteFile();
            noteFile.setLocalId(new ObjectId().toString());
            noteFile.setServerId(serverId);
            noteFile.save();
        }
        return noteFile.getLocalId();
    }

    public static String convertFromLocalIdToServerId(String localId) {
        NoteFile noteFile = AppDataBase.getNoteFileByLocalId(localId);
        return noteFile == null ? null : noteFile.getServerId();
    }

    public static Uri newLocalImage(String filePath) {
        NoteFile noteFile = new NoteFile();
        noteFile.setLocalId(new ObjectId().toString());
        noteFile.setLocalPath(filePath);
        noteFile.save();
        return getLocalImageUri(noteFile.getLocalId());
    }

    public static Uri getLocalImageUri(String localId) {
        return new Uri.Builder().scheme(SCHEME).path(IMAGE_PATH).appendQueryParameter("id", localId).build();
    }

    public static Uri getServerImageUri(String serverId) {
        return new Uri.Builder().scheme("https").authority("leanote.com").appendEncodedPath("api/file/getImage").appendQueryParameter("fileId", serverId).build();
    }

    public static boolean isLocalImageUri(Uri uri) {
        return SCHEME.equals(uri.getScheme()) && IMAGE_PATH_WITH_SLASH.equals(uri.getPath());
    }

    public static InputStream getImage(String localId) {
        NoteFile noteFile = AppDataBase.getNoteFileByLocalId(localId);
        if (noteFile == null) {
            return null;
        }
        String filePath = null;
        if (isLocalFileExist(noteFile.getLocalPath())) {
            filePath = noteFile.getLocalPath();
            Log.i(TAG, "use local image, path=" + filePath);
        } else {
            String url = NoteFileService.getUrl("https://leanote.com", noteFile.getServerId(), AccountHelper.getDefaultAccount().getAccessToken());
            Log.i(TAG, "use server image, url=" + url);
            try {
                filePath = NoteFileService.getImageFromServer(Uri.parse(url), Leanote.getContext().getCacheDir());
                noteFile.setLocalPath(filePath);
                Log.i(TAG, "download finished, path=" + filePath);
                noteFile.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileInputStream inputStream = null;
        try {
            if (!TextUtils.isEmpty(filePath)) {
                inputStream = new FileInputStream(filePath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    private static String getImageFromServer(Uri targetUri, File parentDir) throws IOException {
        URI target = URI.create(targetUri.toString());
        String fileName = String.format(Locale.US, "leanote-%s.png", new ObjectId().toString());
        File file = new File(parentDir, fileName);
//        Log.i(TAG, "target=" + target.toString() + ", file=" + file.getAbsolutePath());

        InputStream input = target.toURL().openStream();
        BufferedSource source = Okio.buffer(Okio.source(input));
        Sink output = Okio.sink(file);
        source.readAll(output);
        source.close();
        output.flush();
        output.close();
        return file.getAbsolutePath();
    }

    private static String getUrl(String baseUrl, String serverId, String token) {
        return String.format(Locale.US, "%s/api/file/getImage?fileId=%s&token=%s", baseUrl, serverId, token);
    }

    private static boolean isLocalFileExist(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            return file.isFile();
        }
        return false;
    }
}
