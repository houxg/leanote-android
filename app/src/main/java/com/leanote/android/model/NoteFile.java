package com.leanote.android.model;

import com.google.gson.annotations.SerializedName;
import com.leanote.android.db.AppDataBase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(name = "NoteFile", database = AppDataBase.class)
public class NoteFile extends BaseModel {

    @Column(name = "noteId")
    String noteId;

    @Column(name = "serverId")
    @SerializedName("FileId")
    String mServerId;

    @PrimaryKey
    @Column(name = "localId")
    @SerializedName("LocalFileId")
    String mLocalId;

    @Column(name = "localPath")
    String mLocalPath;

    @SerializedName("Type")
    String mType;

    @SerializedName("Title")
    String mTitle;

    @SerializedName("HasBody")
    boolean mHasBody;

    @Column(name = "isAttach")
    @SerializedName("IsAttach")
    boolean mIsAttach;

    @Column(name = "isDraft")
    boolean mIsDraft;


    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getServerId() {
        return mServerId;
    }

    public String getLocalId() {
        return mLocalId;
    }

    public String getType() {
        return mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public boolean isHasBody() {
        return mHasBody;
    }

    public boolean isAttach() {
        return mIsAttach;
    }

    public String getLocalPath() {
        return mLocalPath;
    }

    public void setServerId(String mServerId) {
        this.mServerId = mServerId;
    }

    public void setLocalId(String mLocalId) {
        this.mLocalId = mLocalId;
    }

    public void setLocalPath(String mLocalPath) {
        this.mLocalPath = mLocalPath;
    }

    public void setType(String mType) {
        this.mType = mType;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setHasBody(boolean mHasBody) {
        this.mHasBody = mHasBody;
    }

    public void setIsAttach(boolean mIsAttach) {
        this.mIsAttach = mIsAttach;
    }
}
