package com.leanote.android.db;

import android.util.Log;

import com.leanote.android.model.NoteDetailList;
import com.leanote.android.model.NoteFile;
import com.leanote.android.model.NoteFile_Table;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NoteInfo_Table;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.model.NotebookInfo_Table;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Database(name = "leanote_db", version = 1)
public class AppDataBase {

    private static final String TAG = "AppDataBase";

    public static void updateNoteSettings(long localId, String notebookId, String tags, boolean isBlog) {
        NoteInfo noteInfo = getNoteByLocalId(localId);
        if (noteInfo == null) {
            Log.i(TAG, "updateNoteSettings(), note not found");
            return;
        }
        noteInfo.setNoteBookId(notebookId);
        noteInfo.setIsPublicBlog(isBlog);
        noteInfo.setTags(tags);
        noteInfo.save();
    }

    public static void updateNoteTitle(long localId, String title) {
        NoteInfo noteInfo = getNoteByLocalId(localId);
        if (noteInfo == null) {
            Log.i(TAG, "updateNote(), note not found");
            return;
        }
        noteInfo.setTitle(title);
        noteInfo.save();
    }

    public static void updateNoteContent(long localId, String content) {
        NoteInfo noteInfo = getNoteByLocalId(localId);
        if (noteInfo == null) {
            Log.i(TAG, "updateNote(), note not found");
            return;
        }
        noteInfo.setContent(content);
        noteInfo.save();
    }

    public static void deleteNoteByLocalId(long localId) {
        NoteInfo info = getNoteByLocalId(localId);
        info.delete();
    }

    public static NoteInfo getNoteByServerId(String serverId) {
        return SQLite.select()
                .from(NoteInfo.class)
                .where(NoteInfo_Table.noteId.eq(serverId))
                .querySingle();
    }

    public static NoteInfo getNoteByLocalId(long localId) {
        return SQLite.select()
                .from(NoteInfo.class)
                .where(NoteInfo_Table.id.eq(localId))
                .querySingle();
    }

    public static NoteDetailList getNotesFromNotebook(String userId, long localNotebookId) {
        NoteDetailList detailList = new NoteDetailList();
        NotebookInfo notebookInfo = getNotebookByLocalId(localNotebookId);
        if (notebookInfo == null) {
            return detailList;
        }
        List<NoteInfo> noteInfos = SQLite.select()
                .from(NoteInfo.class)
                .where(NoteInfo_Table.notebookId.eq(notebookInfo.getNotebookId()))
                .and(NoteInfo_Table.userId.eq(userId))
                .queryList();

        detailList.addAll(noteInfos);
        return detailList;
    }

    public static NoteDetailList getAllNotes(String userId) {
        List<NoteInfo> noteInfos = SQLite.select()
                .from(NoteInfo.class)
                .where(NoteInfo_Table.userId.eq(userId))
                .queryList();
        NoteDetailList detailList = new NoteDetailList();
        detailList.addAll(noteInfos);
        return detailList;
    }

    public static NotebookInfo getNotebookByServerId(String serverId) {
        return SQLite.select()
                .from(NotebookInfo.class)
                .where(NotebookInfo_Table.notebookId.eq(serverId))
                .querySingle();
    }

    public static NotebookInfo getNotebookByLocalId(long localId) {
        return SQLite.select()
                .from(NotebookInfo.class)
                .where(NotebookInfo_Table.id.eq(localId))
                .querySingle();
    }

    public static List<NotebookInfo> getAllNotebook(String userId) {
        return SQLite.select()
                .from(NotebookInfo.class)
                .where(NoteInfo_Table.userId.eq(userId))
                .queryList();
    }

    public static List<String> getAllNotebookTitles(String userId) {
        List<String> titles = new ArrayList<>();
        List<NotebookInfo> notebookInfos = getAllNotebook(userId);
        for (NotebookInfo notebookInfo : notebookInfos) {
            titles.add(notebookInfo.getTitle());
        }
        return titles;
    }

    public static List<NotebookInfo> getNoteisBlogList(String userId) {
        return SQLite.select()
                .from(NotebookInfo.class)
                .where(NoteInfo_Table.userId.eq(userId))
                .and(NoteInfo_Table.isBlog.eq(true))
                .queryList();
    }

    public static List<NoteFile> getAllRelatedFile(long noteLocalId) {
        return SQLite.select()
                .from(NoteFile.class)
                .where(NoteFile_Table.noteLocalId.eq(noteLocalId))
                .queryList();
    }

    public static NoteFile getNoteFileByLocalId(String localId) {
        return SQLite.select()
                .from(NoteFile.class)
                .where(NoteFile_Table.localId.eq(localId))
                .querySingle();
    }

    public static NoteFile getNoteFileByServerId(String serverId) {
        return SQLite.select()
                .from(NoteFile.class)
                .where(NoteFile_Table.serverId.eq(serverId))
                .querySingle();
    }

    public static void deleteFileExcept(long noteLocalId, Collection<String> excepts) {
        SQLite.delete()
                .from(NoteFile.class)
                .where(NoteFile_Table.noteLocalId.eq(noteLocalId))
                .and(NoteFile_Table.localId.notIn(excepts))
                .async()
                .execute();
    }
}
