package com.leanote.android.model;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.networking.retrofitapi.model.BaseResponse;
import com.leanote.android.util.StringUtils;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.util.List;

@Table(name = "Note", database = AppDataBase.class)
public class Note extends BaseResponse {

    private static final String TAG = "Note";

    @Column(name = "localId")
    @PrimaryKey(autoincrement = true)
    Long localId;
    Long localNotebookId;
    String desc;
    String noteAbstract;
    String fileIds;
    boolean isDeleted;
    boolean isDirty;
    boolean isUploading;
    boolean uploadSucc = true;

    @SerializedName("NoteId")
    String noteId;
    @SerializedName("NotebookId")
    String noteBookId;
    @SerializedName("UserId")
    String userId;
    @SerializedName("Title")
    String title;
    @SerializedName("Tags")
    String[] tags;
    @SerializedName("Content")
    String content;
    @SerializedName("IsMarkdown")
    boolean isMarkDown;
    @SerializedName("IsTrash")
    boolean isDeletedOnServer;
    @SerializedName("IsBlog")
    boolean isPublicBlog;
    @SerializedName("CreatedTime")
    String createdTime;
    @SerializedName("UpdatedTime")
    String updatedTime;
    @SerializedName("PublicTime")
    String publicTime;
    @SerializedName("Usn")
    int usn;
    @SerializedName("Files")
    List<NoteFile> noteFiles;

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
        return localId;
    }

    public void setId(Long id) {
        this.localId = id;
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

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public List<NoteFile> getNoteFiles() {
        if (noteFiles == null || noteFiles.isEmpty()) {

        }
        return noteFiles;
    }

    public boolean isMarkDown() {
        return isMarkDown;
    }

    public void setIsMarkDown(boolean isMarkDown) {
        this.isMarkDown = isMarkDown;
    }

    public boolean isDeletedOnServer() {
        return isDeletedOnServer;
    }

    public void setIsTrash(boolean isTrash) {
        this.isDeletedOnServer = isTrash;
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

    @Override
    public String toString() {
        return "NoteInfo{" +
                "id=" + localId +
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
                ", isTrash=" + isDeletedOnServer +
                ", isDeleted=" + isDeleted +
                ", isDirty=" + isDirty +
                ", isPublicBlog=" + isPublicBlog +
                ", createdTime='" + createdTime + '\'' +
                ", updatedTime='" + updatedTime + '\'' +
                ", publicTime='" + publicTime + '\'' +
                ", usn=" + usn +
                '}';
    }

    public boolean hasChanges(Note otherNote) {
        logDiff("title", title, otherNote.title);
        logContentDiff(content, otherNote.content);
        logDiff("notebookId", noteBookId, otherNote.noteBookId);
        logDiff("isMarkdown", isMarkDown, otherNote.isMarkDown);
        logDiff("tags", tags, otherNote.tags);
        logDiff("isBlog", isPublicBlog, otherNote.isPublicBlog);

        return otherNote == null || !StringUtils.equals(title, otherNote.title)
                || !StringUtils.equals(content, otherNote.content)
                || !StringUtils.equals(noteBookId, otherNote.noteBookId)
                || isMarkDown != otherNote.isMarkDown
//                || !StringUtils.equals(tags, otherNote.tags)
                || isPublicBlog != otherNote.isPublicBlog;
    }

    private void logDiff(String name, Object oldObj, Object newObj) {
        if (!oldObj.equals(newObj)) {
            Log.i(TAG, String.format("%s changed, old=%s.", name, oldObj));
            Log.i(TAG, String.format("%s changed, new=%s.", name, newObj));
        }
    }

    private void logContentDiff(String oldContent, String newContent) {
        if (oldContent.length() != newContent.length()) {
            Log.i(TAG, "length has changed, old=" + oldContent.length() + ", new=" + newContent.length());
        }

        int minimum = Math.min(oldContent.length(), newContent.length());

        for (int i = 0; i < minimum; i++) {
            char oldChar = oldContent.charAt(i);
            char newChar = newContent.charAt(i);
            if (oldChar != newChar) {
                Log.i(TAG, "not match from " + i);
                break;
            }
        }
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
}
