package com.leanote.android.ui.note.refact;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.leanote.android.R;
import com.leanote.android.util.CollectionUtils;
import com.yuyh.library.imgsel.ImgSelActivity;
import com.yuyh.library.imgsel.ImgSelConfig;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EditorFragment extends Fragment implements Editor.EditorListener {

    private static final String TAG = "EditorFragment";
    private static final String ARG_IS_MARKDOWN = "arg_is_markdown";
    private static final String ARG_ENABLE_EDIT = "arg_enable_edit";
    protected static final int REQ_SELECT_IMAGE = 879;


    private EditorFragmentListener mListener;
    private Editor mEditor;

    @BindView(R.id.ll_tools)
    View mToolContainer;

    @BindView(R.id.btn_bold)
    ImageButton mBoldBtn;
    @BindView(R.id.btn_italic)
    ImageButton mItalicBtn;
    @BindView(R.id.btn_order_list)
    ImageButton mOrderListBtn;
    @BindView(R.id.btn_unorder_list)
    ImageButton mUnorderListBtn;
    @BindView(R.id.web_editor)
    WebView mWebView;

    private boolean mIsEditingEnabled = true;

    public EditorFragment() {
    }

    public static EditorFragment getNewInstance(boolean isMarkdown, boolean enableEditing) {
        EditorFragment fragment = new EditorFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_IS_MARKDOWN, isMarkdown);
        arguments.putBoolean(ARG_ENABLE_EDIT, enableEditing);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof EditorFragmentListener) {
            mListener = (EditorFragmentListener) getActivity();
        } else {
            throw new IllegalArgumentException("Current activity is not the EditorFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_editor, container, false);
        ButterKnife.bind(this, view);

        Bundle arguments = savedInstanceState == null ? getArguments() : savedInstanceState;
        mIsEditingEnabled = arguments.getBoolean(ARG_ENABLE_EDIT, false);
        boolean isMarkdown = arguments.getBoolean(ARG_IS_MARKDOWN, true);

        mToolContainer.setVisibility(mIsEditingEnabled ? View.VISIBLE : View.GONE);

        if (isMarkdown) {
            mEditor = new MarkdownEditor(this);
        } else {
            mEditor = new RichTextEditor(this);
        }
        mEditor.init(mWebView);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_IS_MARKDOWN, mEditor instanceof MarkdownEditor);
        outState.putBoolean(ARG_ENABLE_EDIT, mToolContainer.getVisibility() == View.VISIBLE);
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

    @OnClick(R.id.btn_img)
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

    @OnClick(R.id.btn_link)
    void insertLink() {
        DialogUtils.editLink(getActivity(), "", "", new DialogUtils.ChangedListener() {
            @Override
            public void onChanged(String title, String link) {
                Log.i(TAG, "title=" + title + ", url=" + link);
                mEditor.insertLink(title, link);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SELECT_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null
                && mListener != null) {
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

    @OnClick(R.id.btn_order_list)
    void toggleOrderList() {
        mEditor.toggleOrderList();
    }

    @OnClick(R.id.btn_unorder_list)
    void toggleUnorderList() {
        mEditor.toggleUnorderList();
    }

    @OnClick(R.id.btn_bold)
    void toggleBold() {
        mEditor.toggleBold();
    }

    @OnClick(R.id.btn_italic)
    void toggleItalic() {
        mEditor.toggleItalic();
    }

    public void setEditingEnabled(boolean enabled) {
        mIsEditingEnabled = enabled;
        mEditor.setEditingEnabled(enabled);
        //TODO: add slide animation
        mToolContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPageLoaded() {
        mEditor.setEditingEnabled(mIsEditingEnabled);
        if (mListener != null) {
            mListener.onInitialized();
        }
    }

    @Override
    public void onClickedLink(String title, String url) {
        if (mIsEditingEnabled) {
            DialogUtils.editLink(getActivity(), title, url, new DialogUtils.ChangedListener() {
                @Override
                public void onChanged(String title, String link) {
                    mEditor.updateLink(title, link);
                }
            });
        } else {
            //TODO: go to link
        }
    }

    @Override
    public void onStyleChanged(final Editor.Style style, final boolean enabled) {
        mBoldBtn.post(new Runnable() {
            @Override
            public void run() {
                switch (style) {
                    case BOLD:
                        setStyleButton(mBoldBtn, enabled, R.drawable.format_bar_button_bold_highlighted, R.drawable.format_bar_button_bold);
                        break;
                    case ITALIC:
                        setStyleButton(mItalicBtn, enabled, R.drawable.format_bar_button_italic_highlighted, R.drawable.format_bar_button_italic);
                        break;
                    case ORDER_LIST:
                        setStyleButton(mOrderListBtn, enabled, R.drawable.format_bar_button_ol_highlighted, R.drawable.format_bar_button_ol);
                        break;
                    case UNORDER_LIST:
                        setStyleButton(mUnorderListBtn, enabled, R.drawable.format_bar_button_ul_highlighted, R.drawable.format_bar_button_ul);
                        break;
                }
            }
        });
    }

    private void setStyleButton(ImageButton button, boolean enabled, int enabledRes, int disabledRes) {
        if (enabled) {
            button.setBackgroundResource(enabledRes);
        } else {
            button.setBackgroundResource(disabledRes);
        }
    }

    public interface EditorFragmentListener {
        Uri createImage(String filePath);

        Uri createAttach(String filePath);

        void onInitialized();
    }
}
