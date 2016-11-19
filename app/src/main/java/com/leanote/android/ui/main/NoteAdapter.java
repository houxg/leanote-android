package com.leanote.android.ui.main;


import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.service.AccountService;
import com.leanote.android.util.TimeUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteHolder> {

    private List<NoteInfo> mData;
    private Map<String, String> mNotebookId2TitleMaps;
    private NoteAdapterListener mListener;

    public NoteAdapter(NoteAdapterListener listener) {
        mListener = listener;
    }

    public void loadFromLocal() {
        mData = AppDataBase.getAllNotes(AccountService.getCurrent().getUserId());
        sortByUpdatedTime();
        notifyDataSetChanged();
    }

    public void loadFromLocal(long notebookLocalId) {
        mData = AppDataBase.getNotesFromNotebook(AccountService.getCurrent().getUserId(), notebookLocalId);
        sortByUpdatedTime();
        updateNotebookMap();
        notifyDataSetChanged();
    }

    public void load(List<NoteInfo> source) {
        mData = source;
        sortByUpdatedTime();
        updateNotebookMap();
        notifyDataSetChanged();
    }

    private void sortByUpdatedTime() {
        Collections.sort(mData, new Comparator<NoteInfo>() {
            @Override
            public int compare(NoteInfo lhs, NoteInfo rhs) {
                long lTime = lhs.getUpdatedTimeVal();
                long rTime = rhs.getUpdatedTimeVal();
                if (lTime > rTime) {
                    return -1;
                } else if (lTime < rTime) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        updateNotebookMap();
    }

    @Override
    public NoteAdapter.NoteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        NoteHolder holder = new NoteAdapter.NoteHolder(view);
        return holder;
    }

    private void updateNotebookMap() {
        List<NotebookInfo> notebooks = AppDataBase.getAllNotebook(AccountService.getCurrent().getUserId());
        mNotebookId2TitleMaps = new HashMap<>();
        for (NotebookInfo notebook : notebooks) {
            mNotebookId2TitleMaps.put(notebook.getNotebookId(), notebook.getTitle());
        }
    }

    @Override
    public void onBindViewHolder(NoteAdapter.NoteHolder holder, int position) {
        final NoteInfo note = mData.get(position);
        if (TextUtils.isEmpty(note.getTitle())) {
            holder.titleTv.setText("Untitled");
        } else {
            holder.titleTv.setText(note.getTitle());
        }
        holder.contentTv.setText(note.getContent());
        holder.notebookTv.setText(mNotebookId2TitleMaps.get(note.getNoteBookId()));
        long updateTime = note.getUpdatedTimeVal();
        String time;
        if (updateTime >= TimeUtils.getToday().getTimeInMillis()) {
            time = TimeUtils.toTimeFormat(updateTime);
        } else if (updateTime >= TimeUtils.getYesterday().getTimeInMillis()) {
            time = String.format(Locale.US, "Yesterday %s", TimeUtils.toTimeFormat(updateTime));
        } else if (updateTime >= TimeUtils.getThisYear().getTimeInMillis()) {
            time = TimeUtils.toDateFormat(updateTime);
        } else {
            time = TimeUtils.toYearFormat(updateTime);
        }
        holder.updateTimeTv.setText(time);
        holder.dirtyTv.setVisibility(note.isDirty() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickNote(note);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mListener != null) {
                    mListener.onLongClickNote(note);
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public void delete(NoteInfo note) {
        int index = mData.indexOf(note);
        if (index >= 0) {
            mData.remove(index);
            notifyItemRemoved(index);
        }
    }

    static class NoteHolder extends RecyclerView.ViewHolder {

        View itemView;

        @BindView(R.id.tv_title)
        TextView titleTv;
        @BindView(R.id.tv_content)
        TextView contentTv;
        @BindView(R.id.tv_notebook)
        TextView notebookTv;
        @BindView(R.id.tv_update_time)
        TextView updateTimeTv;
        @BindView(R.id.tv_dirty)
        TextView dirtyTv;

        public NoteHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.itemView = itemView;
        }
    }

    public interface NoteAdapterListener {
        void onClickNote(NoteInfo note);
        void onLongClickNote(NoteInfo note);
    }
}