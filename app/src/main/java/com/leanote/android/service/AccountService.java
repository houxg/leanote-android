package com.leanote.android.service;


import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.NewAccount;
import com.leanote.android.networking.retrofitapi.ApiProvider;
import com.leanote.android.networking.retrofitapi.RetrofitUtils;

import rx.Observable;

public class AccountService {

    public static Observable<NewAccount> login(String email, String password) {
        return RetrofitUtils.create(ApiProvider.getInstance().getAuthApi().login(email, password));
    }

    public static NewAccount getCurrent() {
        return AppDataBase.getAccountWithToken();
    }

    public static boolean isSignedIn() {
        return getCurrent() != null;
    }
}
