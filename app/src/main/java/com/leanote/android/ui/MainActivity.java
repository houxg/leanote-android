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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.NewAccount;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.model.User;
import com.leanote.android.service.AccountService;
import com.leanote.android.ui.main.NoteFragment;
import com.leanote.android.ui.note.NoteEditActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements NotebookAdapter.NotebookAdapterListener {

    private static final String EXT_SHOULD_RELOAD = "ext_should_reload";
    private static final String TAG_NOTE_FRAGMENT = "note_fragment";

    NoteFragment mNoteFragment;

    @BindView(R.id.rv_notebook)
    RecyclerView mNotebookRv;
    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.tv_email)
    TextView mEmailTv;
    @BindView(R.id.iv_avatar)
    ImageView mAvatarIv;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTv;
    @BindView(R.id.iv_notebook_triangle)
    View mNotebookTriangle;
    @BindView(R.id.rl_notebook_list)
    View mNotebookPanel;

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white);

        if (savedInstanceState == null) {
            mNoteFragment = NoteFragment.newInstance();
            getFragmentManager().beginTransaction().add(R.id.container, mNoteFragment, TAG_NOTE_FRAGMENT).commit();
        } else {
            mNoteFragment = (NoteFragment) getFragmentManager().findFragmentByTag(TAG_NOTE_FRAGMENT);
        }

        mNotebookRv.setLayoutManager(new LinearLayoutManager(this));
        NotebookAdapter adapter = new NotebookAdapter();
        adapter.setListener(this);
        mNotebookRv.setAdapter(adapter);
        adapter.init();
        mEmailTv.setText(AccountService.getCurrent().getEmail());
        mNotebookTriangle.setTag(false);
        refreshInfo();
        fetchInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START, true);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START, true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchInfo() {
        AccountService.getInfo(AccountService.getCurrent().getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<User>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(User user) {
                        AccountService.saveToAccount(user, AccountService.getCurrent().getHost());
                        refreshInfo();
                    }
                });
    }

    private void refreshInfo() {
        NewAccount account = AccountService.getCurrent();
        mUserNameTv.setText(account.getUserName());
        mEmailTv.setText(account.getEmail());
        if (!TextUtils.isEmpty(account.getAvatar())) {
            Glide.with(this)
                    .load(account.getAvatar())
                    .centerCrop()
                    .bitmapTransform(new CropCircleTransformation(this))
                    .into(mAvatarIv);
        }
    }

    @Override
    public void onClickedNotebook(NotebookInfo notebook) {
        mNoteFragment.loadNoteFromLocal(notebook.getId());
        mDrawerLayout.closeDrawer(GravityCompat.START, true);
        setTitle(notebook.getTitle());
    }

    @OnClick(R.id.fab)
    void clickedFab() {
        NewAccount account = AccountService.getCurrent();
        NoteInfo newNote = new NoteInfo();
        NotebookInfo notebook = AppDataBase.getRecentNoteBook(account.getUserId());
        newNote.setNoteBookId(notebook.getNotebookId());
        newNote.setIsMarkDown(account.getDefaultEditor() == NewAccount.EDITOR_MARKDOWN);
        newNote.save();
        Intent intent = NoteEditActivity.getOpenIntent(this, newNote.getId());
        startActivity(intent);
    }

    @OnClick(R.id.rl_recent_notes)
    void showRecentNote() {
        mNoteFragment.loadRecentNote();
        mDrawerLayout.closeDrawer(GravityCompat.START, true);
        setTitle("Recent notes");
    }

    @OnClick(R.id.rl_notebook)
    void toggleNotebook() {
        boolean shouldShowNotebook = (boolean) mNotebookTriangle.getTag();
        shouldShowNotebook = !shouldShowNotebook;
        if (shouldShowNotebook) {
            mNotebookTriangle.animate()
                    .rotation(180)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            mNotebookTriangle.animate()
                    .rotation(0)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        mNotebookPanel.setVisibility(shouldShowNotebook ? View.VISIBLE : View.GONE);
        mNotebookTriangle.setTag(shouldShowNotebook);
    }
}
