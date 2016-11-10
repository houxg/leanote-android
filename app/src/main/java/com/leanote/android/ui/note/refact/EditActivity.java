package com.leanote.android.ui.note.refact;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.networking.NetworkUtils;
import com.leanote.android.service.NoteFileService;
import com.leanote.android.service.NoteService;
import com.leanote.android.util.ToastUtils;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class EditActivity extends AppCompatActivity implements EditorFragment.EditorFragmentListener {

    private static final String TAG = "EditActivity";
    public static final String EXT_NOTE_LOCAL_ID = "ext_note_local_id";

    private EditorFragment mEditorFragment;
    private NoteInfo mOriginal;
    private NoteInfo mModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        long noteLocalId = getIntent().getLongExtra(EXT_NOTE_LOCAL_ID, -1);
        mOriginal = AppDataBase.getNoteByLocalId(noteLocalId);
        mModified = AppDataBase.getNoteByLocalId(noteLocalId);

        mEditorFragment = EditorFragment.getNewInstance(false, this);
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.add(R.id.container_editor, mEditorFragment);
        trans.commit();
    }

    public static Intent getOpenIntent(Context context, long noteLocalId) {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putExtra(EXT_NOTE_LOCAL_ID, noteLocalId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_notebook);
        item.setTitle(String.format("Notebook:%s", getNotebookTitle(mModified)));
        return super.onPrepareOptionsMenu(menu);
    }

    private String getNotebookTitle(NoteInfo note) {
        return AppDataBase.getNotebookByServerId(note.getNoteBookId())
                .getTitle();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                checkChangeOrDirty()
                        .subscribe(new Action1<NoteInfo>() {
                            @Override
                            public void call(NoteInfo noteInfo) {
                                saveAsDraft(noteInfo);
                                if (NetworkUtils.isNetworkAvailable(EditActivity.this)) {
                                    boolean isSucceed = NoteService.updateNote(AppDataBase.getNoteByLocalId(mModified.getId()));
                                    if (isSucceed) {
                                        finish();
                                    } else {
                                        ToastUtils.showToast(EditActivity.this, R.string.upload_fail, ToastUtils.Duration.SHORT);
                                    }
                                } else {
                                    ToastUtils.showToast(EditActivity.this, R.string.no_network_message, ToastUtils.Duration.SHORT);
                                }
                            }
                        });
                return true;
            case R.id.action_notebook:
                final List<NotebookInfo> notebooks = AppDataBase.getAllNotebook(AccountHelper.getDefaultAccount().getUserId());
                NotebookInfo currentNoteBook = AppDataBase.getNotebookByServerId(mModified.getNoteBookId());
                int currentSelection = -1;
                String[] titles = new String[notebooks.size()];
                for (int i = 0; i < titles.length; i++) {
                    titles[i] = notebooks.get(i).getTitle();
                    if (notebooks.get(i).getNotebookId().equals(currentNoteBook.getNotebookId())) {
                        currentSelection = i;
                    }
                }
                new AlertDialog.Builder(this)
                        .setTitle("Choose notebook")
                        .setSingleChoiceItems(titles, currentSelection, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mModified.setNoteBookId(notebooks.get(which).getNotebookId());
                                Log.i(TAG, "select=" + notebooks.get(which).getTitle());
                                invalidateOptionsMenu();
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        checkChangeOrDirty()
                .subscribe(new Action1<NoteInfo>() {
                    @Override
                    public void call(NoteInfo note) {
                        if (!isNewNote(note) || !(TextUtils.isEmpty(note.getTitle()) || TextUtils.isEmpty(note.getContent()))) {
                            saveAsDraft(note);
                        } else {
                            Log.i(TAG, "remove empty note, id=" + note.getId());
                            AppDataBase.deleteNoteByLocalId(note.getId());
                        }
                    }
                });
    }

    private Observable<NoteInfo> checkChangeOrDirty() {
        return Observable.create(
                new Observable.OnSubscribe<NoteInfo>() {
                    @Override
                    public void call(Subscriber<? super NoteInfo> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            updateNote();
                            if (mModified.isDirty() || mModified.hasChanges(mOriginal) || isNewNote(mModified)) {
                                subscriber.onNext(mModified);
                            }
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    private void updateNote() {
        String title = mEditorFragment.getTitle();
        String content = mEditorFragment.getContent();
        mModified.setTitle(title);
        mModified.setContent(content);
    }

    private void saveAsDraft(NoteInfo note) {
        Log.i(TAG, "saveAsDraft(), local id=" + note.getId());
        NoteInfo noteFromDb = AppDataBase.getNoteByLocalId(note.getId());
        noteFromDb.setContent(note.getContent());
        noteFromDb.setTitle(note.getTitle());
        noteFromDb.setIsDirty(true);
        noteFromDb.update();
    }

    private boolean isNewNote(NoteInfo note) {
        return TextUtils.isEmpty(note.getNoteId());
    }

    @Override
    public Uri createImage(String filePath) {
        return NoteFileService.createImageFile(mModified.getId(), filePath);
    }

    @Override
    public Uri createAttach(String filePath) {
        return null;
    }

    @Override
    public void onInitialized() {
        mEditorFragment.setTitle(mModified.getTitle());
        mEditorFragment.setContent(mModified.getContent());
    }
}
