package com.leanote.android.ui.main;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.service.AccountService;

import java.util.HashMap;
import java.util.List;
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
        updateNotebookMap();
        notifyDataSetChanged();
    }

    public void loadFromLocal(long notebookLocalId) {
        mData = AppDataBase.getNotesFromNotebook(AccountService.getCurrent().getUserId(), notebookLocalId);
        updateNotebookMap();
        notifyDataSetChanged();
    }

    public void load(List<NoteInfo> source) {
        mData = source;
        updateNotebookMap();
        notifyDataSetChanged();
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
        holder.titleTv.setText(note.getTitle());
        holder.contentTv.setText(note.getContent());
        holder.notebookTv.setText(mNotebookId2TitleMaps.get(note.getNoteBookId()));
        holder.updateTimeTv.setText(note.getUpdatedTime());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickNote(note);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
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

        public NoteHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.itemView = itemView;
        }
    }

    public interface NoteAdapterListener {
        void onClickNote(NoteInfo note);
    }
}