package com.leanote.android.ui.note.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.leanote.android.Leanote;
import com.leanote.android.R;
import com.leanote.android.db.LeanoteDbManager;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.networking.retrofit.RetrofitUtil;
import com.leanote.android.service.NoteSyncService;
import com.leanote.android.util.AppLog;
import com.leanote.android.util.MediaFile;
import com.leanote.android.util.MediaUtils;
import com.leanote.android.util.NoteSyncResultEnum;
import com.leanote.android.util.XLog;
import com.leanote.android.util.helper.ImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.RequestBody;

//提交笔记到服务器
public class NoteUploadService extends Service {

    public NoteUploadService() {
    }

    private static Context mContext;
    private static final ArrayList<NoteInfo> M_NOTE_DETAILS_LIST = new ArrayList<>();
    private static NoteInfo mCurrentUploadingNote = null;
    private UploadNoteTask mCurrentTask = null;
    //private FeatureSet mFeatureSet;

    public static void addNoteToUpload(NoteInfo currentNote) {
        synchronized (M_NOTE_DETAILS_LIST) {
            M_NOTE_DETAILS_LIST.add(currentNote);
        }
    }

    /*
     * returns true if the passed NoteInfo is either uploading or waiting to be uploaded
     */
    public static boolean isNoteUploading(long localNoteId) {
        // first check the currently uploading NoteInfo
        if (mCurrentUploadingNote != null && mCurrentUploadingNote.getId() == localNoteId) {
            return true;
        }

        // then check the list of NoteDetails waiting to be uploaded
        if (M_NOTE_DETAILS_LIST.size() > 0) {
            synchronized (M_NOTE_DETAILS_LIST) {
                for (NoteInfo note : M_NOTE_DETAILS_LIST) {
                    if (note.getId() == localNoteId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel current task, it will reset NoteInfo from "uploading" to "local draft"
        if (mCurrentTask != null) {
            AppLog.d(AppLog.T.POSTS, "cancelling current upload task");
            mCurrentTask.cancel(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.i("upload size:" + M_NOTE_DETAILS_LIST.size());

        synchronized (M_NOTE_DETAILS_LIST) {
            if (M_NOTE_DETAILS_LIST.size() == 0 || mContext == null) {
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        uploadNextNote();
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    private void uploadNextNote() {
        synchronized (M_NOTE_DETAILS_LIST) {
            if (mCurrentTask == null) { //make sure nothing is running
                mCurrentUploadingNote = null;
                if (M_NOTE_DETAILS_LIST.size() > 0) {
                    mCurrentUploadingNote = M_NOTE_DETAILS_LIST.remove(0);
                    mCurrentTask = new UploadNoteTask();
                    mCurrentTask.execute(mCurrentUploadingNote);
                } else {
                    stopSelf();
                }
            }
        }
    }

    private void noteUploaded() {
        synchronized (M_NOTE_DETAILS_LIST) {
            mCurrentTask = null;
            mCurrentUploadingNote = null;
        }
        uploadNextNote();
    }

    private class UploadNoteTask extends AsyncTask<NoteInfo, Boolean, NoteSyncResultEnum> {

        private NoteInfo mNote;

        @Override
        protected void onPostExecute(NoteSyncResultEnum result) {
            super.onPostExecute(result);
            noteUploaded();
            if (result == NoteSyncResultEnum.FAIL) {
                mNote.setIsDirty(true);
                mNote.setUploadSucc(false);
            } else {
                mNote.setIsDirty(false);
                mNote.setUploadSucc(true);
            }

            AppLog.i("upload result:" + result);
            EventBus.getDefault().post(new NoteEvents.PostUploadEnded(result));
        }

        @Override
        protected NoteSyncResultEnum doInBackground(NoteInfo... params) {
            /*
            同步步骤：
            1) pull 2) push 3) 保存usn
             */

            mNote = params[0];

            //processNoteMedia(mNote.getContent());

            //1.pull
            NoteSyncService.syncPullNote();
            //2.push
            AppLog.i("start to upload note");
            String noteTitle = TextUtils.isEmpty(mNote.getTitle()) ? getString(R.string.untitled) : mNote.getTitle();
            String uploadingNoteTitle = String.format(getString(R.string.uploading_note), noteTitle);
            String uploadingPostMessage = String.format(
                    getString(R.string.uploading_note),
                    getString(R.string.note).toLowerCase()
            );

            //mNote.setIsDirty(false);

            Map<String, Object> contentStruct = getContentStruct(mNote);

            try {
                EventBus.getDefault().post(new NoteEvents.PostUploadStarted());

                // request the new/updated post from the server to ensure local copy matches server
                //push
                JSONObject response = uploadNoteToServer(mNote, contentStruct);

                //handle push response
                return processResponse(response, mNote);

            } catch (final Exception e) {
                e.printStackTrace();
            }
            return NoteSyncResultEnum.FAIL;
        }

    }

    private NoteSyncResultEnum processResponse(JSONObject response, NoteInfo mNote) {
        String noteId = null;
        String msg = null;

        if (response == null) {
            return NoteSyncResultEnum.FAIL;
        }

        try {
            if (response.has("NoteId")) {
                noteId = response.getString("NoteId");
            }

            if (response.has("Msg")) {
                msg = response.getString("Msg");
            }

        } catch (Exception e) {
            Log.e("error:", "", e);
        }


        LeanoteDbManager.getInstance().updateNote(mNote);

        if ("conflict".equals(msg)) {
            //server端note覆盖本地note
            try {

                updateNoteToLocal(mNote.getNoteId());
                return NoteSyncResultEnum.CONFLICT;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            //push成功
            if (!TextUtils.isEmpty(noteId)) {
                updateLocalNote(response, mNote);
                //push成功后更新usn
                int serverUsn = NoteSyncService.getServerSyncState();
                AppLog.i("last serverUsn:" + serverUsn);
                LeanoteDbManager.getInstance().updateAccountUsn(serverUsn, AccountHelper.getDefaultAccount().getUserId());

                return NoteSyncResultEnum.SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NoteSyncResultEnum.FAIL;
    }

    private JSONObject uploadNoteToServer(NoteInfo mNote, Map<String, Object> contentStruct) {
        String api;
        String host = AccountHelper.getDefaultAccount().getHost();
        String token = AccountHelper.getDefaultAccount().getAccessToken();
        if (mNote.getUsn() == 0) {
//            api = String.format("%s/api/note/addNote", host);
            api = "addNote";
        } else {
//            api = String.format("%s/api/note/updateNote", host);
            api = "updateNote";
            contentStruct.put("usn", mNote.getUsn());
        }
        XLog.e(XLog.getTag(), XLog.TAG_GU + String.format("%s/api/note/" + api, host));
        contentStruct.put("token", token);

        String response = RetrofitUtil.getInstance()
                .setBaseUrl(host)
                .setTimeout(30000)
                .build()
                .uploadNoteToServer(api, contentStruct);

        XLog.e(XLog.getTag(), XLog.TAG_GU + "updateNote response=   " + response);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject object = null;
        try {
            object = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AppLog.i("upload response:" + object);
        return object;
    }

    /**
     * 根据笔记对象获取对应的参数Map
     * 对象中可以能回有文件
     *
     * @param mNote 笔记对象
     *
     * @return
     */
    private Map<String, Object> getContentStruct(NoteInfo mNote) {
        Map<String, Object> contentStruct = new HashMap<>();

        String content = mNote.getContent();
        contentStruct.put("title", mNote.getTitle());
        contentStruct.put("IsBlog", mNote.isPublicBlog());
        contentStruct.put("IsMarkdown", mNote.isMarkDown());
        //content = content.replaceAll("\uFFFC", "");
        contentStruct.put("Content", content);
        contentStruct.put("NotebookId", mNote.getNoteBookId());

        String tags = mNote.getTags();
        if (!TextUtils.isEmpty(tags)) {
            String[] tagArray = tags.split(",");
            for (int i = 0; i < tagArray.length; i++) {
                if (!TextUtils.isEmpty(tagArray[i]) && !"\"\"".equals(tagArray[i])) {
                    contentStruct.put(String.format("Tags[%s]", i), tagArray[i]);
                }
            }
        }

        //本地新创建笔记的fileId
        String fileIds = mNote.getFileIds();
        AppLog.i("fileIds:" + fileIds);

        if (!TextUtils.isEmpty(fileIds)) {
            //mediafile本地id
            String[] fileIdArray = fileIds.split(",");
            for (int i = 0; i < fileIdArray.length; i++) {
                MediaFile mf =LeanoteDbManager.getInstance().getMediaFileById(fileIdArray[i]);
                if (mf != null) {
                    contentStruct.put(String.format("Files[%s][LocalFileId]", i), mf.getId());
                    contentStruct.put(String.format("Files[%s][IsAttach]", i), false);
                    if (!TextUtils.isEmpty(mf.getMediaId())) {
                        contentStruct.put(String.format("Files[%s][FileId]", i), mf.getMediaId());
                        contentStruct.put(String.format("Files[%s][HasBody]", i), false);
                    } else {
                        contentStruct.put(String.format("Files[%s][HasBody]", i), true);
                        MediaFile compressedFile = getCompressedFile(mf);

                        File tempFile;
                        try {
                            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mf.getFileName());
                            AppLog.i("fileExtension:" + fileExtension);
                            //tempFile = createTempUploadFile("." + fileExtension);
                            tempFile = new File(compressedFile.getFilePath());
                        } catch (Exception e) {
                            return null;
                        }
                        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), tempFile);
                        contentStruct.put(String.format("FileDatas[%s]", mf.getId()), fileBody);
                    }
                }
            }
        }
        return contentStruct;
    }

    private File createTempUploadFile(String fileExtension) throws IOException {
        return File.createTempFile("lea-", fileExtension, mContext.getCacheDir());
    }

    private MediaFile getCompressedFile(MediaFile mf) {
        Uri imageUri = Uri.parse(mf.getFilePath());
        File imageFile = null;
        String mimeType = "", path = "";

        if (imageUri.toString().contains("content:")) {
            String[] projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.MIME_TYPE};

            Cursor cur = Leanote.getContext().getContentResolver().query(imageUri, projection, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                int mimeTypeColumn = cur.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
                String thumbData = cur.getString(dataColumn);
                imageFile = new File(thumbData);

                mimeType = cur.getString(mimeTypeColumn);
                path = thumbData;
                mf.setFilePath(thumbData);
                LeanoteDbManager.getInstance().saveMediaFile(mf);

            }
        } else {
            imageFile = new File(mf.getFilePath());
        }
        if (imageFile == null) {
            return null;
        }

        if (TextUtils.isEmpty(mimeType)) {
            mimeType = MediaUtils.getMediaFileMimeType(imageFile);
        }


        String fileName = MediaUtils.getMediaFileName(imageFile, mimeType);
        String fileExtension = "." + MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();

        int orientation = ImageUtils.getImageOrientation(mContext, path);

        boolean shouldUploadResizedVersion = true;
        // If it's not a gif and blog don't keep original size, there is a chance we need to resize
        if (mimeType.equals("image/gif")) {
            return mf;
        }

        Bitmap.CompressFormat fmt;
        if (fileExtension.equalsIgnoreCase("png")) {
            fmt = Bitmap.CompressFormat.PNG;
        } else {
            fmt = Bitmap.CompressFormat.JPEG;
        }

        MediaFile resizedMediaFile = new MediaFile(mf);
        // Create resized image
//            byte[] bytes = ImageUtils.createThumbnailFromUri(mContext, imageUri, resizedMediaFile.getWidth(),
//                    fileExtension, orientation);

        Bitmap bitmap = getSmallBitmap(resizedMediaFile.getFilePath(), fmt);
        if (bitmap == null) {
            // We weren't able to resize the image, so we will upload the full size image with css to resize it
            AppLog.i("unable to compress image...");
            return mf;
        } else {
            // Save temp image
            String tempFilePath;
            File resizedImageFile;
            try {
                resizedImageFile = File.createTempFile("lea-image-", fileExtension);
                FileOutputStream out = new FileOutputStream(resizedImageFile);

                bitmap.compress(fmt, 100, out);
//                    out.write(bytes);
                out.flush();
                out.close();
                tempFilePath = resizedImageFile.getPath();
            } catch (IOException e) {
                AppLog.w(AppLog.T.POSTS, "failed to create image temp file");
                //mErrorMessage = mContext.getString(R.string.error_media_upload);
                return mf;
            }

            // upload resized picture
            if (!TextUtils.isEmpty(tempFilePath)) {
                resizedMediaFile.setFileName(fileName);
                resizedMediaFile.setFilePath(tempFilePath);
                //resizedImageFile.delete();
                return resizedMediaFile;
            } else {
                AppLog.w(AppLog.T.POSTS, "failed to create resized picture");
                //mErrorMessage = mContext.getString(R.string.out_of_memory);
                return mf;
            }
        }

    }


    public static Bitmap getSmallBitmap(String filePath, Bitmap.CompressFormat fmt) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 480, 800);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap bm = BitmapFactory.decodeFile(filePath, options);
        if (bm == null) {
            return null;
        }
        int degree = readPictureDegree(filePath);
        bm = rotateBitmap(bm, degree);
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();

            bm.compress(fmt, 30, baos);

        } finally {
            try {
                if (baos != null)
                    baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bm;

    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotate) {
        if (bitmap == null)
            return null;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        // Setting post rotate to 90
        Matrix mtx = new Matrix();
        mtx.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? widthRatio : heightRatio;
        }

        return inSampleSize;
    }

    private static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    private void updateLocalNote(JSONObject response, NoteInfo mNote) throws JSONException {
        int usn = response.getInt("Usn");
        AppLog.i("push usn:" + usn);

        //新增笔记需要更新所有字段
        List<String> localNoteIds = LeanoteDbManager.getInstance().getLocalNoteIds(AccountHelper.getDefaultAccount().getUserId());
        try {
            //更新media表mediaId
            //更新服务端fileId 到 media表的 mediaId
            if (response.get("Files") instanceof JSONArray) {
                JSONArray files = response.getJSONArray("Files");
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);
                    String localFileId = file.getString("LocalFileId");
                    String serverFileId = file.getString("FileId");
                    LeanoteDbManager.getInstance().updateMedia(localFileId, serverFileId);
                }
            }

            NoteInfo note = NoteSyncService.parseServerNote(response, localNoteIds);
            //Leanote.leaDB.updateDirtyUsn(noteId, usn);
            if (note != null) {
                note.setId(mNote.getId());
                LeanoteDbManager.getInstance().updateNote(note);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateNoteToLocal(String noteId) throws JSONException {
        NoteInfo serverNote = NoteSyncService.getServerNote(noteId);
        serverNote.setIsDirty(false);
        LeanoteDbManager.getInstance().updateNoteByNoteId(serverNote);
    }

}
