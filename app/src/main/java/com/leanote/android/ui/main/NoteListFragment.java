package com.leanote.android.ui.main;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leanote.android.Constants;
import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.db.LeanoteDbManager;
import com.leanote.android.model.NoteDetailList;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.networking.NetworkUtils;
import com.leanote.android.networking.retrofitapi.RetrofitUtils;
import com.leanote.android.service.NoteService;
import com.leanote.android.ui.ActivityLauncher;
import com.leanote.android.ui.EmptyViewMessageType;
import com.leanote.android.ui.RequestCodes;
import com.leanote.android.ui.note.EditNotebookActivity;
import com.leanote.android.ui.note.NoteListAdapter;
import com.leanote.android.ui.note.service.NoteEvents;
import com.leanote.android.ui.note.service.NoteUpdateService;
import com.leanote.android.util.AniUtils;
import com.leanote.android.util.AppLog;
import com.leanote.android.util.NoteSyncResultEnum;
import com.leanote.android.util.SwipeToRefreshHelper;
import com.leanote.android.util.ToastUtils;
import com.leanote.android.widget.CustomSwipeRefreshLayout;
import com.leanote.android.widget.PostListButton;
import com.leanote.android.widget.RecyclerItemDecoration;

import de.greenrobot.event.EventBus;

public class NoteListFragment extends Fragment
        implements NoteListAdapter.OnNotesLoadedListener,
        NoteListAdapter.OnNotesSelectedListener,
        NoteListAdapter.OnNotesButtonClickListener {


    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private NoteListAdapter mNoteListAdapter;
    private View mFabView;

    private RecyclerView mRecyclerView;

    private View mEmptyView;
    private ProgressBar mProgressBar;
    private TextView mEmptyViewTitle;
    private ImageView mEmptyViewImage;

    private boolean mIsFetchingPosts;

    private final NoteDetailList mTrashedNotes = new NoteDetailList();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public static NoteListFragment newInstance() {
        return new NoteListFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        AppLog.i("notelist oncreateview...");
        View view = inflater.inflate(R.layout.note_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mFabView = view.findViewById(R.id.fab_button);

        mProgressBar.setVisibility(ProgressBar.GONE);

        mEmptyView = view.findViewById(R.id.empty_view);

        mEmptyView.setVisibility(View.VISIBLE);
        mEmptyViewTitle = (TextView) mEmptyView.findViewById(R.id.title_empty);
        mEmptyViewTitle.setVisibility(View.VISIBLE);
        CharSequence initMsg = getText(R.string.notes_fetching);
        mEmptyViewTitle.setText(initMsg);
        mEmptyViewImage = (ImageView) mEmptyView.findViewById(R.id.image_empty);
        mEmptyViewImage.setVisibility(View.VISIBLE);
        //temp add
        mEmptyView.setVisibility(View.VISIBLE);
        mEmptyViewTitle.setVisibility(View.VISIBLE);

        Context context = getActivity();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mRecyclerView.addItemDecoration(
                new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newNote();
            }
        });

        return view;
    }


    private void initSwipeToRefreshHelper() {
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(
                getActivity(),
                (CustomSwipeRefreshLayout) getView().findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }

                        if (!NetworkUtils.checkConnection(getActivity())) {
                            setRefreshing(false);
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                            return;
                        }
                        //该方法拉取笔记后存在本地的db中，然后通过EventBus通知AsyncTask加载到页面中
                        requestNotes();
                    }
                });
    }

    public NoteListAdapter getNoteListAdapter() {
        if (mNoteListAdapter == null) {
            mNoteListAdapter = new NoteListAdapter(getActivity());
            mNoteListAdapter.setOnNotesLoadedListener(this);
            mNoteListAdapter.setOnNoteSelectedListener(this);
            mNoteListAdapter.setOnNoteButtonClickListener(this);

        }

        return mNoteListAdapter;
    }

    private boolean isNoteAdapterEmpty() {
        return (mNoteListAdapter != null && mNoteListAdapter.getItemCount() == 0);
    }

    private void loadNotes(Long localNotebookId) {
        getNoteListAdapter().loadNotes(localNotebookId);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        Bundle extras = getActivity().getIntent().getExtras();

        if (extras != null) {
            Long localNotebookId = extras.getLong(EditNotebookActivity.EXTRA_LOCAL_NOTEBOOK_ID);
            CustomSwipeRefreshLayout refreshLayout = (CustomSwipeRefreshLayout) getView().findViewById(R.id.ptr_layout);
            refreshLayout.setEnabled(false);
            loadNotes(localNotebookId);
            return;
        }

        initSwipeToRefreshHelper();
        // since setRetainInstance(true) is used, we only need to request latest
        // posts the first time this is called (ie: not after device rotation)
        if (bundle == null && NetworkUtils.checkConnection(getActivity())) {
            updateEmptyView(EmptyViewMessageType.LOADING);
            requestNotes();
        }
    }

    private void newNote() {
        if (!isAdded()) return;

        ActivityLauncher.addNewNoteForResult(getActivity());
    }

    public void onResume() {
        super.onResume();

        if (mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getNoteListAdapter());
        }

        Bundle extras = getActivity().getIntent().getExtras();

        if (extras != null) {
            Long localNotebookId = extras.getLong(EditNotebookActivity.EXTRA_LOCAL_NOTEBOOK_ID);
            CustomSwipeRefreshLayout refreshLayout = (CustomSwipeRefreshLayout) getView().findViewById(R.id.ptr_layout);
            refreshLayout.setEnabled(false);
            loadNotes(localNotebookId);
            return;
        }

        // always (re)load when resumed to reflect changes made elsewhere
        loadNotes(null);

        // scale in the fab after a brief delay if it's not already showing
        if (mFabView.getVisibility() != View.VISIBLE) {
            long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        AniUtils.scaleIn(mFabView, AniUtils.Duration.MEDIUM);
                    }
                }
            }, delayMs);
        }
    }

    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    private void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NoteEvents.PostUploadStarted event) {
        if (isAdded()) {
            loadNotes(null);
        }
    }

    /*
     * upload ended, reload regardless of success/fail so correct status of uploaded post appears
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(NoteEvents.PostUploadEnded event) {
        NoteSyncResultEnum result = event.result;
        if (isAdded() && result.getCode() == NoteSyncResultEnum.SUCCESS.getCode()) {
            loadNotes(null);

            ToastUtils.showToast(getActivity(), getString(R.string.upload_successfully));
        } else {

            ToastUtils.showToast(getActivity(), result.getMsg());
        }
    }

    private void requestNotes() {
        if (!isAdded() || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        mIsFetchingPosts = true;

        NoteUpdateService.startServiceForNote(getActivity());

    }


    private void hideLoadMoreProgress() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }


    /*
     * PostUpdateService finishHed a request to retrieve new posts
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(NoteEvents.RequestNotes event) {
        Log.i("listen load note", ".....");
        mIsFetchingPosts = false;
        if (isAdded()) {
            setRefreshing(false);
            hideLoadMoreProgress();

            Log.i("is fail:", String.valueOf(event.ismFailed()));
            loadNotes(null);
            if (event.ismFailed()) {
                ToastUtils.showToast(getActivity(), getString(R.string.note_sync_fail));
            }
        }
    }

    private void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        int stringId;
        switch (emptyViewMessageType) {
            case LOADING:
                stringId = R.string.notes_fetching;
                break;
            case NETWORK_ERROR:
                stringId = R.string.no_network_message;
                break;
            case GENERIC_ERROR:
                stringId = R.string.error_refresh_notes;
                break;
            case NO_CONTENT:
                stringId = R.string.no_notes;
                break;
            default:
                return;
        }

        CharSequence errorMsg = getText(stringId);
        mEmptyViewTitle.setText(errorMsg);
        mEmptyView.setVisibility(View.VISIBLE);
        mEmptyViewTitle.setVisibility(View.VISIBLE);

        mEmptyViewImage.setVisibility(emptyViewMessageType == EmptyViewMessageType.NO_CONTENT ? View.VISIBLE : View.GONE);
        mEmptyView.setVisibility(TextUtils.isEmpty(errorMsg) ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /*
     * called by the adapter after posts have been loaded
     */
    @Override
    public void onNotesLoaded(int postCount) {
        if (!isAdded()) {
            return;
        }

        if (postCount == 0 && !mIsFetchingPosts) {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
            } else {
                updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            }
        } else if (postCount > 0) {
            mEmptyView.setVisibility(View.GONE);

        }
    }


    /*
     * called by the adapter when the user clicks a post
     */
    @Override
    public void onNotesSelected(NoteInfo note) {
        onNoteButtonClicked(PostListButton.BUTTON_PREVIEW, note);
    }

    /*
     * called by the adapter when the user clicks the edit/view/stats/trash button for a post
     */
    @Override
    public void onNoteButtonClicked(int buttonType, NoteInfo note) {
        if (!isAdded()) return;

        //Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(post.getPostId());
        //load note detail
        AppLog.i("click note id:" + note.getId());
        NoteInfo fullNote = AppDataBase.getNoteByLocalId(note.getId());
        if (fullNote == null) {
            ToastUtils.showToast(getActivity(), R.string.note_not_found);
            return;
        }

        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                ActivityLauncher.editNoteForResult(getActivity(), fullNote.getId());
                break;
            case PostListButton.BUTTON_PREVIEW:
            case PostListButton.BUTTON_VIEW:
                ActivityLauncher.previewNoteForResult(getActivity(), fullNote.getId());
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                // prevent deleting post while it's being uploaded
                trashNote(fullNote);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.EDIT_NOTE && resultCode == Activity.RESULT_OK) {

        }
    }

    /*
         * send the passed post to the trash with undo
         */
    private void trashNote(final NoteInfo note) {
        if (!isAdded() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        //final Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(note.getNoteId());

        // remove post from the list and add it to the list of trashed posts
        getNoteListAdapter().hidePost(note);
        mTrashedNotes.add(note);

        View.OnClickListener undoListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // user undid the trash, so unhide the post and remove it from the list of trashed posts
                mTrashedNotes.remove(note);
                getNoteListAdapter().unhidePost(note);
            }
        };

        // different undo text if this is a local draft since it will be deleted rather than trashed
//        String text = getString(R.string.note_trashed);
        Snackbar.make(getView().findViewById(R.id.coordinator), R.string.note_trashed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, undoListener)
                .show();

        // wait for the undo snackbar to disappear before actually deleting the post
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // if the post no longer exists in the list of trashed posts it's because the
                // user undid the trash, so don't perform the deletion
                if (!mTrashedNotes.contains(note)) {
                    AppLog.d(AppLog.T.POSTS, "user undid trashing");
                    return;
                }

                //delete note in local
                AppLog.i("delete note id:" + note.getId());
                AppDataBase.deleteNoteByLocalId(note.getId());
                LeanoteDbManager.getInstance().deleteMediaFileByNoteId(note.getNoteId());
                //delete note in server
                new DeleteNoteTask().execute(note);
            }
        }, Constants.SNACKBAR_LONG_DURATION_MS);
    }

    private class DeleteNoteTask extends AsyncTask<NoteInfo, Spanned, Void> {


        @Override
        protected Void doInBackground(NoteInfo... params) {
            NoteInfo note = params[0];

            // local draft note
            if (TextUtils.isEmpty(note.getNoteId())) {
                return null;
            }
            RetrofitUtils.excute(NoteService.deleteNote(note.getNoteId(), note.getUsn()));
            //ToastUtils.showToast(getActivity(), getString(R.string.delete_note_succ));
            return null;
        }
    }

    
}
