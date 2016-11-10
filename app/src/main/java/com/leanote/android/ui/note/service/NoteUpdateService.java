package com.leanote.android.ui.note.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.leanote.android.model.AccountHelper;
import com.leanote.android.service.NoteService;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by binnchx on 10/19/15.
 */
public class NoteUpdateService extends Service {

    private static final String TAG = "NoteUpdateService";

    public static void startServiceForNote(Context context) {
        if (!AccountHelper.isSignedIn()) {
            return;
        }

        Intent intent = new Intent(context, NoteUpdateService.class);
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(NoteService.fetchFromServer());
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        NoteEvents.RequestNotes event = new NoteEvents.RequestNotes();
                        event.setmFailed(true);
                        EventBus.getDefault().post(event);
                    }

                    @Override
                    public void onNext(Boolean isSucceed) {
                        NoteEvents.RequestNotes event = new NoteEvents.RequestNotes();
                        event.setmFailed(!isSucceed);
                        EventBus.getDefault().post(event);
                    }
                });

        return START_NOT_STICKY;
    }
}
