package com.leanote.android.db;

import com.leanote.android.model.NoteDetailList;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NoteInfo_Table;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.model.NotebookInfo_Table;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;

@Database(name = "leanote_db", version = 1)
public class AppDataBase {

    public static void updateNoteSettings(String noteId, String notebookId, String tags, boolean isBlog) {
        NoteInfo noteInfo = getNoteByServerId(noteId);
        if (noteInfo == null) {
            return;
        }
        noteInfo.setNoteBookId(notebookId);
        noteInfo.setIsPublicBlog(isBlog);
        noteInfo.setTags(tags);
        noteInfo.update();
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

    public static NoteDetailList getNotesFromNotebook(String userId, String notebookId) {
        List<NoteInfo> noteInfos =  SQLite.select()
                .from(NoteInfo.class)
                .where(NoteInfo_Table.notebookId.eq(notebookId))
                .and(NoteInfo_Table.userId.eq(userId))
                .queryList();
        NoteDetailList detailList = new NoteDetailList();
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
}
