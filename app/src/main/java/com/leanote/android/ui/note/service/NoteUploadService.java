package com.leanote.android.ui.note.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.leanote.android.model.NoteInfo;
import com.leanote.android.service.NoteService;
import com.leanote.android.util.AppLog;
import com.leanote.android.util.NoteSyncResultEnum;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

//提交笔记到服务器
public class NoteUploadService extends Service {

    private static final String TAG = "NoteUploadService";

    public NoteUploadService() {
    }

    private static Context mContext;
    private static final ArrayList<NoteInfo> M_NOTE_DETAILS_LIST = new ArrayList<>();
    private static NoteInfo mCurrentUploadingNote = null;
    //private FeatureSet mFeatureSet;
    Object mCurrentTask;

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
                    Observable.create(
                            new Observable.OnSubscribe<Boolean>() {
                                @Override
                                public void call(Subscriber<? super Boolean> subscriber) {
                                    if (!subscriber.isUnsubscribed()) {
                                        subscriber.onNext(NoteService.updateNote(mCurrentUploadingNote));
                                        subscriber.onCompleted();
                                    }
                                }
                            })
                            .subscribeOn(Schedulers.io())
                            .doOnSubscribe(new Action0() {
                                @Override
                                public void call() {
                                    EventBus.getDefault().post(new NoteEvents.PostUploadStarted());
                                }
                            })
                            .doOnCompleted(new Action0() {
                                @Override
                                public void call() {
                                    EventBus.getDefault().post(new NoteEvents.PostUploadEnded(NoteSyncResultEnum.SUCCESS));
                                    noteUploaded();
                                }
                            })
                            .subscribe();
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
}
