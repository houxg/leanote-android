package com.leanote.android.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.leanote.android.R;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.service.AccountService;
import com.leanote.android.ui.main.NoteFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements NotebookAdapter.NotebookAdapterListener {

    private static final String EXT_SHOULD_RELOAD = "ext_should_reload";

    NoteFragment mNoteFragment;

    @BindView(R.id.rv_notebook)
    RecyclerView mNotebookRv;
    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.tv_email)
    TextView mEmailTv;

    public static Intent getOpenIntent(Context context, boolean shouldReload) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXT_SHOULD_RELOAD, shouldReload);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_main);
        ButterKnife.bind(this);
        initToolBar((Toolbar) findViewById(R.id.toolbar));

        mNoteFragment = NoteFragment.newInstance();
        getFragmentManager().beginTransaction().add(R.id.container, mNoteFragment).commit();

        mNotebookRv.setLayoutManager(new LinearLayoutManager(this));
        NotebookAdapter adapter = new NotebookAdapter();
        adapter.setListener(this);
        mNotebookRv.setAdapter(adapter);
        adapter.init();
        mEmailTv.setText(AccountService.getCurrent().getEmail());
    }

    @Override
    public void onClickedNotebook(NotebookInfo notebook) {
        mNoteFragment.loadNoteFromLocal(notebook.getId());
        mDrawerLayout.closeDrawer(GravityCompat.START, true);
    }
}
