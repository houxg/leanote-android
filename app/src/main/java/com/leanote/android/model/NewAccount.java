package com.leanote.android.model;

import com.google.gson.annotations.SerializedName;
import com.leanote.android.db.AppDataBase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(name = "Account", database = AppDataBase.class)
public class NewAccount extends BaseModel {

    @SerializedName("Ok")
    boolean isOk;
    @SerializedName("Msg")
    String msg;

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    @SerializedName("LocalUserId")
    long localUserId;
    @Column(name = "userId")
    @SerializedName("UserId")
    String userId = "";
    @Column(name = "userName")
    @SerializedName("Username")
    String userName = "";
    @Column(name = "email")
    @SerializedName("Email")
    String email = "";
    @Column(name = "verified")
    @SerializedName("Verified")
    boolean verified;
    @Column(name = "avatar")
    @SerializedName("Avatar")
    String avatar = "";
    @Column(name = "token")
    @SerializedName("Token")
    String accessToken = "";
    @Column(name = "useMarkDown")
    @SerializedName("UseMarkdown")
    boolean useMarkdown = true;
    @Column(name = "lastUsn")
    @SerializedName("LastSyncUsn")
    int lastSyncUsn;
    @Column(name = "host")
    @SerializedName("Host")
    String host = "";

    public NewAccount() {
    }

    public boolean isOk() {
        return isOk;
    }

    public long getLocalUserId() {
        return localUserId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getLastSyncUsn() {
        return lastSyncUsn;
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

    public String getMsg() {
        return msg;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLastUsn(int lastUsn) {
        this.lastSyncUsn = lastUsn;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public String toString() {
        return "NewAccount{" +
                "isOk=" + isOk +
                ", msg='" + msg + '\'' +
                ", localUserId=" + localUserId +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", verified=" + verified +
                ", avatar='" + avatar + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", useMarkdown=" + useMarkdown +
                ", lastSyncUsn=" + lastSyncUsn +
                ", host='" + host + '\'' +
                '}';
    }
}
