package com.leanote.android.ui.note.refact;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.leanote.android.R;
import com.leanote.android.model.NoteFile;

import java.io.File;

import butterknife.ButterKnife;

public class EditorFragment extends Fragment implements Editor.EditorListener {

    protected EditorFragmentListener mListener;
    private Editor mEditor;

    public EditorFragment() {
    }

    public static EditorFragment getNewInstance(boolean isMarkdown, @NonNull EditorFragmentListener listener) {
        EditorFragment fragment = new EditorFragment();
        fragment.mListener = listener;
        fragment.mEditor = new RichTextEditor(fragment);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_editor, container, false);
        WebView webView = (WebView) view.findViewById(R.id.web_editor);
        mEditor.init(webView);
        ButterKnife.bind(this, view);
        return view;
    }

    public void setTitle(String title) {
        mEditor.setTitle(title);
    }

    public void setContent(String content) {
        mEditor.setContent(content);
    }

    public String getTitle() {
        return mEditor.getTitle();
    }

    public String getContent() {
        return mEditor.getContent();
    }

    @Override
    public void onPageLoaded() {
        mListener.onInitialized();
    }

    public interface EditorFragmentListener {
        NoteFile createImage(File file);

        NoteFile createAttach(File file);

        void onInitialized();
    }
}
