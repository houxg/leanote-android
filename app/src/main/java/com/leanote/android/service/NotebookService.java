package com.leanote.android.service;


import android.util.Log;

import com.leanote.android.model.NewAccount;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.networking.retrofitapi.ApiProvider;
import com.leanote.android.networking.retrofitapi.RetrofitUtils;

public class NotebookService {

    private static final String TAG = "NotebookService";

    public static void addNotebook(String title, String parentNotebookId) {
        NotebookInfo notebook = RetrofitUtils.excute(ApiProvider.getInstance().getNotebookApi().addNotebook(title, parentNotebookId));
        if (notebook == null) {
            throw new IllegalStateException("Network error");
        }
        if (notebook.isOk()) {
            NewAccount account = AccountService.getCurrent();
            if (notebook.getUsn() - account.getLastSyncUsn() == 1) {
                Log.d(TAG, "update usn=" + notebook.getUsn());
                account.setLastUsn(notebook.getUsn());
                account.save();
            }
            notebook.insert();
        } else {
            throw new IllegalStateException(notebook.getMsg());
        }
    }
}
