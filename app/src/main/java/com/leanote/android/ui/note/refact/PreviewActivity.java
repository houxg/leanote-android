package com.leanote.android.ui.note.refact;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.NoteInfo;

public class PreviewActivity extends AppCompatActivity implements EditorFragment.EditorFragmentListener {

    private static final String TAG = "PreviewActivity";
    public static final String EXT_NOTE_LOCAL_ID = "ext_note_local_id";
    public static final int REQ_EDIT = 1;

    private EditorFragment mEditorFragment;
    private NoteInfo mNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        long noteLocalId = getIntent().getLongExtra(EXT_NOTE_LOCAL_ID, -1);
        mNote = AppDataBase.getNoteByLocalId(noteLocalId);

        mEditorFragment = EditorFragment.getNewInstance(mNote.isMarkDown(), this);
        getFragmentManager().beginTransaction().add(R.id.container, mEditorFragment).commit();
    }

    public static Intent getOpenIntent(Context context, long noteLocalId) {
        Intent intent = new Intent(context, PreviewActivity.class);
        intent.putExtra(EXT_NOTE_LOCAL_ID, noteLocalId);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                startActivityForResult(EditActivity.getOpenIntent(this, mNote.getId()), REQ_EDIT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK) {
            mNote = AppDataBase.getNoteByLocalId(mNote.getId());
            if (mNote == null) {
                finish();
            } else {
                refresh();
            }
        }
    }

    @Override
    public Uri createImage(String filePath) {
        return null;
    }

    @Override
    public Uri createAttach(String filePath) {
        return null;
    }

    @Override
    public void onInitialized() {
        refresh();
        mEditorFragment.setEditingEnabled(false);
    }

    private void refresh() {
        mEditorFragment.setTitle(mNote.getTitle());
        mEditorFragment.setContent(mNote.getContent());
    }
}
