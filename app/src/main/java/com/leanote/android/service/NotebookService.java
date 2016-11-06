package com.leanote.android.service;


import com.leanote.android.model.NotebookInfo;
import com.leanote.android.networking.retrofitapi.ApiProvider;

import java.util.List;

import rx.Observable;

public class NotebookService {
    public static Observable<List<NotebookInfo>> getSyncNotebook(int afterUsn, int maxEntry) {
        return ApiProvider.getInstance().getNotebookApi().getSyncNotebooks(afterUsn, maxEntry);
    }
}
