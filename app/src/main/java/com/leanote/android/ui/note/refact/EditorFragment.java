package com.leanote.android.ui.note.refact;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.leanote.android.R;
import com.leanote.android.util.CollectionUtils;
import com.yuyh.library.imgsel.ImgSelActivity;
import com.yuyh.library.imgsel.ImgSelConfig;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class EditorFragment extends Fragment implements Editor.EditorListener {

    private static final String TAG = "EditorFragment";
    protected static final int REQ_SELECT_IMAGE = 879;

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

    @OnClick(R.id.btn_insert_img)
    void handleInsertImage() {
        ImgSelConfig config = new ImgSelConfig.Builder(
                new com.yuyh.library.imgsel.ImageLoader() {
                    @Override
                    public void displayImage(Context context, String path, ImageView imageView) {
                        Glide.with(context).load(path).into(imageView);
                    }
                })
                .multiSelect(false)
                .backResId(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material)
                .needCrop(true)
                .cropSize(1, 1, 200, 200)
                .needCamera(true)
                .build();
        ImgSelActivity.startActivity(this, config, REQ_SELECT_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SELECT_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null) {
            List<String> pathList = data.getStringArrayListExtra(ImgSelActivity.INTENT_RESULT);
            if (CollectionUtils.isNotEmpty(pathList)) {
                String path = pathList.get(0);
                Log.i(TAG, "path=" + path);
                //create ImageObject
                Uri imageUri = mListener.createImage(path);
                //insert to note
                mEditor.insertImage("untitled", imageUri.toString());
            }
        }
    }

    @Override
    public void onPageLoaded() {
        mListener.onInitialized();
    }

    public interface EditorFragmentListener {
        Uri createImage(String filePath);

        Uri createAttach(String filePath);

        void onInitialized();
    }
}
