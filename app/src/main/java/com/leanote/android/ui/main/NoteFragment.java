package com.leanote.android.ui.main;


import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leanote.android.R;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.ui.note.NotePreviewActivity;
import com.leanote.android.ui.note.service.NoteEvents;
import com.leanote.android.ui.note.service.NoteUpdateService;
import com.leanote.android.util.DisplayUtils;
import com.leanote.android.util.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class NoteFragment extends Fragment implements NoteAdapter.NoteAdapterListener {

    private static final String TAG = "NoteFragment";

    private int RECENT_NOTES = -1;

    @BindView(R.id.recycler_view)
    RecyclerView mNoteListView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout mSwipeRefresh;

    private NoteAdapter mAdapter;

    private long mCurrentNotebookId = RECENT_NOTES;

    public NoteFragment() {
    }

    public static NoteFragment newInstance() {
        return new NoteFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);
        ButterKnife.bind(this, view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(container.getContext());
        mNoteListView.setLayoutManager(layoutManager);
        mNoteListView.setItemAnimator(new DefaultItemAnimator());

        int dashGap = DisplayUtils.dpToPx(container.getContext(), 4);
        int dashWidth = DisplayUtils.dpToPx(container.getContext(), 8);
        int height = DisplayUtils.dpToPx(container.getContext(), 1);
        mNoteListView.addItemDecoration(new DashDividerDecoration(0xffa0a0a0, dashGap, dashWidth, height));
        mAdapter = new NoteAdapter(this);
        mNoteListView.setAdapter(mAdapter);

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //TODO:check network
                NoteUpdateService.startServiceForNote(getActivity());
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public void loadNoteFromLocal(long notebookLocalId) {
        mCurrentNotebookId = notebookLocalId;
        mAdapter.loadFromLocal(notebookLocalId);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter.loadFromLocal();
    }

    @Override
    public void onClickNote(NoteInfo note) {
        startActivity(NotePreviewActivity.getOpenIntent(getActivity(), note.getId()));
    }

    public void onEventMainThread(NoteEvents.RequestNotes event) {
        Log.i(TAG, "RequestNotes rcv: isSucceed=" + event.ismFailed());
        if (isAdded()) {
            mSwipeRefresh.setRefreshing(false);
            if (mCurrentNotebookId > 0) {
                mAdapter.loadFromLocal(mCurrentNotebookId);
            } else {
                mAdapter.loadFromLocal();
            }
            if (event.ismFailed()) {
                ToastUtils.showToast(getActivity(), getString(R.string.note_sync_fail));
            }
        }
    }

    private static class DashDividerDecoration extends RecyclerView.ItemDecoration {

        private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Path mPath;

        public DashDividerDecoration(int color, int dashGap, int dashWidth, int height) {
            mPaint.setColor(color);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(height);
            mPaint.setPathEffect(new DashPathEffect(new float[]{dashWidth, dashGap}, 0));
            mPath = new Path();
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();
            final int childCount = parent.getChildCount();
            final int strokeWidth = (int) mPaint.getStrokeWidth();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin +
                        Math.round(ViewCompat.getTranslationY(child));
                int offsetY = top + strokeWidth / 2;

                mPath.reset();
                mPath.moveTo(left, offsetY);
                mPath.lineTo(right, offsetY);
                c.drawPath(mPath, mPaint);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(0, 0, 0, (int) mPaint.getStrokeWidth());
        }
    }
}
