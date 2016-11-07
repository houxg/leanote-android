package com.leanote.android.model;

import com.google.gson.annotations.SerializedName;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.util.AppLog;
import com.leanote.android.util.CollectionUtils;
import com.leanote.android.util.StringUtils;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;
import java.util.List;

/**
 * Created by binnchx on 10/18/15.
 */
@Table(name = "Notes", database = AppDataBase.class)
public class NoteInfo extends BaseModel implements Serializable {

    @SerializedName("Ok")
    boolean isOk = true;
    @SerializedName("Msg")
    String msg;

    @Column(name = "noteId")
    @SerializedName("NoteId")
    String noteId;
    @Column(name = "notebookId")
    @SerializedName("NotebookId")
    String noteBookId;
    @Column(name = "userId")
    @SerializedName("UserId")
    String userId;
    @Column(name = "title")
    @SerializedName("Title")
    String title;
    @Column(name = "tags")
    String tags;
    @SerializedName("Tags")
    List<String> tagData;
    @Column(name = "content")
    @SerializedName("Content")
    String content;
    @Column(name = "isMarkDown")
    @SerializedName("IsMarkdown")
    boolean isMarkDown;
    @Column(name = "IsDeletedOnServer")
    @SerializedName("IsTrash")
    boolean isTrash;
    @Column(name = "isBlog")
    @SerializedName("IsBlog")
    boolean isPublicBlog;
    @Column(name = "createdTime")
    @SerializedName("CreatedTime")
    String createdTime;
    @Column(name = "updatedTime")
    @SerializedName("UpdatedTime")
    String updatedTime;
    @Column(name = "publicTime")
    @SerializedName("PublicTime")
    String publicTime;
    @Column(name = "usn")
    @SerializedName("Usn")
    int usn;

    //TODO:handle files
    @SerializedName("Files")
    List<NoteFile> noteFiles;

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    Long id;
    Long localNotebookId;
    @Column(name = "desc")
    String desc;
    @Column(name = "noteAbstract")
    String noteAbstract;
    String fileIds;
    boolean isDeleted;
    @Column(name = "isDirty")
    boolean isDirty;
    @Column(name = "isUploading")
    boolean isUploading;
    boolean uploadSucc = true;

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getPublicTime() {
        return publicTime;
    }

    public void setPublicTime(String publicTime) {
        this.publicTime = publicTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNoteBookId() {
        return noteBookId;
    }

    public void setNoteBookId(String noteBookId) {
        this.noteBookId = noteBookId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isMarkDown() {
        return isMarkDown;
    }

    public void setIsMarkDown(boolean isMarkDown) {
        this.isMarkDown = isMarkDown;
    }

    public boolean isTrash() {
        return isTrash;
    }

    public void setIsTrash(boolean isTrash) {
        this.isTrash = isTrash;
    }

    public int getUsn() {
        return usn;
    }

    public boolean isUploadSucc() {
        return uploadSucc;
    }

    public void setUploadSucc(boolean uploadSucc) {
        this.uploadSucc = uploadSucc;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public void updateTags() {
        if (CollectionUtils.isEmpty(tagData)) {
            tags = "";
            return;
        }
        StringBuilder tagBuilder = new StringBuilder();
        int size = tagData.size();
        int lastIndex = size - 1;
        for (int i = 0; i < size; i++) {
            tagBuilder.append(tagData.get(i));
            if (i < lastIndex) {
                tagBuilder.append(",");
            }
        }
        tags = tagBuilder.toString();
    }

    @Override
    public String toString() {
        return "NoteInfo{" +
                "id=" + id +
                ", noteId='" + noteId + '\'' +
                ", noteBookId='" + noteBookId + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", tags='" + tags + '\'' +
                ", noteAbstract='" + noteAbstract + '\'' +
                ", content='" + content + '\'' +
                ", fileIds='" + fileIds + '\'' +
                ", isMarkDown=" + isMarkDown +
                ", isTrash=" + isTrash +
                ", isDeleted=" + isDeleted +
                ", isDirty=" + isDirty +
                ", isPublicBlog=" + isPublicBlog +
                ", createdTime='" + createdTime + '\'' +
                ", updatedTime='" + updatedTime + '\'' +
                ", publicTime='" + publicTime + '\'' +
                ", usn=" + usn +
                '}';
    }

    public boolean hasChanges(NoteInfo otherNote) {

        AppLog.i("title equals:" + !StringUtils.equals(title, otherNote.title));
        AppLog.i("content equals:" + !StringUtils.equals(content, otherNote.content));
        AppLog.i("notebookid equals:" + !(noteBookId.equals(otherNote.noteBookId)));
        AppLog.i("isMarkDown equal:" + (isMarkDown != otherNote.isMarkDown));
        AppLog.i("tags equals:" + !StringUtils.equals(tags, otherNote.tags));
        AppLog.i("isblog equals:" + (isPublicBlog != otherNote.isPublicBlog));

        return otherNote == null || !StringUtils.equals(title, otherNote.title)
                || !StringUtils.equals(content, otherNote.content)
                || !StringUtils.equals(noteBookId, otherNote.noteBookId)
                || isMarkDown != otherNote.isMarkDown
                || !StringUtils.equals(tags, otherNote.tags)
                || isPublicBlog != otherNote.isPublicBlog;
    }

    public boolean isPublicBlog() {
        return isPublicBlog;
    }

    public void setIsPublicBlog(boolean isPublicBlog) {
        this.isPublicBlog = isPublicBlog;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public String getNoteAbstract() {
        return noteAbstract;
    }

    public void setNoteAbstract(String noteAbstract) {
        this.noteAbstract = noteAbstract;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileIds() {
        return fileIds;
    }

    public void setFileIds(String fileIds) {
        this.fileIds = fileIds;
    }

    public boolean isUploading() {
        return isUploading;
    }

    public void setIsUploading(boolean isUploading) {
        this.isUploading = isUploading;
    }

    public Long getLocalNotebookId() {
        return localNotebookId;
    }

    public void setLocalNotebookId(Long localNotebookId) {
        this.localNotebookId = localNotebookId;
    }

    public boolean isOk() {
        return isOk;
    }

    public String getMsg() {
        return msg;
    }
}
