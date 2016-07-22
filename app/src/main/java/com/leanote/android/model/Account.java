package com.leanote.android.model;

import com.leanote.android.datasets.AccountTable;

/**
 * Class for managing logged in user informations.
 */
public class Account extends AccountModel {

    public void signOut() {
        //init();
        clearToken();
        save();
    }

    public void save() {
        AccountTable.save(this);
    }
}
