package com.leanote.android.ui.search;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.util.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by binnchx on 12/10/15.
 */
public class SearchFragment extends Fragment implements SearchView.OnQueryTextListener {

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    private RecyclerView mRecyclerView;
    private SearchAdapter mAdapter;
    private List mdatas;
    private List allDatas;
    private int type = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_search, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mdatas = new ArrayList<>();
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            type = extras.getInt("type");
            if (type == Constant.NOTEBOOK_SEARCH) {
                allDatas = AppDataBase.getAllNotebook(AccountHelper.getDefaultAccount().getUserId());
            } else if (type == Constant.BLOG_SEARCH) {
                allDatas = AppDataBase.getNoteisBlogList(AccountHelper.getDefaultAccount().getUserId());
            } else {
                allDatas = AppDataBase.getAllNotes(AccountHelper.getDefaultAccount().getUserId());
            }
        } else {
            allDatas = AppDataBase.getAllNotes(AccountHelper.getDefaultAccount().getUserId());
        }


        mAdapter = new SearchAdapter(getActivity(), mdatas);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        if (type == Constant.NOTEBOOK_SEARCH) {
            searchView.setQueryHint(getString(R.string.search_notebook_hint));
        } else if (type == Constant.BLOG_SEARCH) {
            searchView.setQueryHint(getString(R.string.search_blog_hint));
        } else {
            searchView.setQueryHint(getString(R.string.search_note_hint));
        }

        searchView.setIconifiedByDefault(false);

        searchView.setOnQueryTextListener(this);
    }


    @Override
    public boolean onQueryTextChange(String query) {

        final List<NoteInfo> filteredModelList = filter(allDatas, query);
        mAdapter.animateTo(filteredModelList);
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private List filter(List models, String query) {
        query = query.toLowerCase();

        final List filteredModelList = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            return filteredModelList;
        }

        for (Object model : models) {
            if (model instanceof NotebookInfo) {
                NotebookInfo notebook = (NotebookInfo) model;
                final String title = notebook.getTitle();
                if (!TextUtils.isEmpty(title) && title.contains(query)) {
                    filteredModelList.add(model);
                }
            } else {
                NoteInfo note = (NoteInfo) model;
                final String content = note.getContent().toLowerCase();
                final String title = note.getTitle().toLowerCase();

                if ((!TextUtils.isEmpty(title) && title.contains(query))
                        || (!TextUtils.isEmpty(content) && content.contains(query))) {

                    filteredModelList.add(model);
                }
            }


        }
        return filteredModelList;
    }


}
