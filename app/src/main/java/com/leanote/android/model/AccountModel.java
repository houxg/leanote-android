package com.leanote.android.model;

import com.leanote.android.db.LeanoteDbManager;

/**
 * Created by binnchx on 8/26/15.
 */
public class AccountModel {

    private boolean isOk;
    private long localUserId;
    private String userId;
    private String userName;
    private String email;
    private boolean verified;
    private String avatar;
    private String accessToken;
    private boolean useMarkdown;
    private int lastSyncUsn;
    private String host;

    public AccountModel() {
        localUserId = 0L;
    }

    public void init() {
        userId = "";
        userName = "";
        email = "";
        verified = false;
        avatar = "";
        accessToken = "";
        lastSyncUsn = 0;
    }

    public boolean isOk() {
        return isOk;
    }

    public void setOk(boolean ok) {
        isOk = ok;
    }

    public void clearToken() {
        accessToken = "";
    }

    public long getLocalUserId() {
        return localUserId;
    }

    public void setLocalUserId(long localUserId) {
        this.localUserId = localUserId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String mUserId) {
        this.userId = mUserId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getLastSyncUsn() {
        return LeanoteDbManager.getInstance().getAccountUsn(userId);
    }

    public void setLastSyncUsn(int lastSyncUsn) {
        this.lastSyncUsn = lastSyncUsn;
    }

    public boolean isUseMarkdown() {
        return useMarkdown;
    }

    public void setUseMarkdown(boolean useMarkdown) {
        this.useMarkdown = useMarkdown;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
