package com.leanote.android.ui;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.NotebookHolder> {

    private Stack<String> mStack;
    private List<NotebookInfo> mData;
    private NotebookAdapterListener mListener;

    public void setListener(NotebookAdapterListener listener) {
        mListener = listener;
    }

    public void init() {
        mData = AppDataBase.getRootNotebooks(AccountHelper.getDefaultAccount().getUserId());
        mStack = new Stack<>();
        notifyDataSetChanged();
    }

    @Override
    public NotebookAdapter.NotebookHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notebook, parent, false);
        NotebookAdapter.NotebookHolder holder = new NotebookAdapter.NotebookHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(NotebookAdapter.NotebookHolder holder, int position) {
        final NotebookInfo notebook = mData.get(position);
        holder.titleTv.setText(notebook.getTitle());

        String notebookId = notebook.getNotebookId();
        boolean isSuper = isSuper(notebookId);
        boolean isSuperOrRoot = isSuper | mStack.isEmpty();
        boolean hasChild = hasChild(notebookId);
        holder.placeholder.setVisibility(isSuperOrRoot ? View.GONE : View.VISIBLE);
        holder.navigator.setVisibility(hasChild ? View.VISIBLE : View.INVISIBLE);
        holder.navigator.setText(isSuper ? "-" : "+");
        holder.navigator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: arrow animation
                if (isSuper(notebook.getNotebookId())) {
                    listUpper();
                } else {
                    listChild(notebook);
                }
            }
        });
        holder.titleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickedNotebook(notebook);
                }
            }
        });
    }

    boolean isSuper(String notebookId) {
        if (mStack.isEmpty()) {
            return false;
        } else {
            return mStack.peek().equals(notebookId);
        }
    }

    boolean hasChild(String notebookId) {
        return CollectionUtils.isNotEmpty(AppDataBase.getChildNotebook(notebookId, AccountHelper.getDefaultAccount().getUserId()));
    }

    private void listUpper() {
        int childrenSize = mData.size();
        mData = new ArrayList<>();
        notifyItemRangeRemoved(0, childrenSize);

        mStack.pop();
        if (mStack.isEmpty()) {
            mData = AppDataBase.getRootNotebooks(AccountHelper.getDefaultAccount().getUserId());
        } else {
            String parentId = mStack.peek();
            mData.add(AppDataBase.getNotebookByServerId(parentId));
            mData.addAll(AppDataBase.getChildNotebook(parentId, AccountHelper.getDefaultAccount().getUserId()));
        }
        notifyItemRangeInserted(0, mData.size());
    }

    private void listChild(NotebookInfo notebook) {
        int index = mData.indexOf(notebook);
        for (int i = 0; i < index; i++) {
            mData.remove(0);
        }
        notifyItemRangeRemoved(0, index);
        int size = mData.size() - 1;
        for (int i = 0; i < size; i++) {
            mData.remove(1);
        }
        notifyItemRangeRemoved(1, size);
        notifyItemChanged(0);

        mStack.push(notebook.getNotebookId());
        List<NotebookInfo> children = AppDataBase.getChildNotebook(notebook.getNotebookId(), AccountHelper.getDefaultAccount().getUserId());
        int childrenSize = children.size();
        mData.addAll(children);
        notifyItemRangeInserted(1, childrenSize);
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public interface NotebookAdapterListener {
        void onClickedNotebook(NotebookInfo notebook);
    }

    static class NotebookHolder extends RecyclerView.ViewHolder {
        View itemView;
        @BindView(R.id.navigator)
        TextView navigator;
        @BindView(R.id.tv_title)
        TextView titleTv;
        @BindView(R.id.placeholder)
        View placeholder;

        public NotebookHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ButterKnife.bind(this, itemView);
        }
    }
}