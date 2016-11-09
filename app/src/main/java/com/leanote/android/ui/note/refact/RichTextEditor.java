package com.leanote.android.ui.note.refact;


import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.leanote.android.editor.OnJsEditorStateChangedListener;
import com.leanote.android.editor.Utils;

import org.json.JSONObject;

import java.util.Map;

import static android.view.View.SCROLLBARS_OUTSIDE_OVERLAY;

public class RichTextEditor extends Editor implements OnJsEditorStateChangedListener {

    private static final String TAG = "RichTextEditor";
    private static final String JS_CALLBACK_HANDLER = "nativeCallbackHandler";
    private WebView mWebView;

    public RichTextEditor(EditorListener listener) {
        super(listener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void init(WebView view) {
        mWebView = view;
        mWebView.setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new EditorClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.addJavascriptInterface(new JsCallbackHandler(this), JS_CALLBACK_HANDLER);
        mWebView.loadUrl("file:///android_asset/android-editor.html");
    }

    private void execJs(final String script) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                mWebView.evaluateJavascript(script, null);
            }
        });
    }

    @Override
    public void setEditingEnabled(boolean enabled) {

    }

    @Override
    public void setTitle(String title) {
        execJs("ZSSEditor.getField('zss_field_title').setPlainText('" +
                Utils.escapeHtml(title) + "');");
    }

    @Override
    public String getTitle() {
        return new JsRunner().get(mWebView, "ZSSEditor.getField('zss_field_title').getHTML()");
    }

    @Override
    public void setContent(String content) {
        execJs("ZSSEditor.getField('zss_field_content').setHTML('" +
                Utils.escapeHtml(content) + "');");
    }

    @Override
    public String getContent() {
        return new JsRunner().get(mWebView, "ZSSEditor.getField('zss_field_content').getHTML()");
    }


    @Override
    public void onDomLoaded() {
        execJs("ZSSEditor.getField('zss_field_content').setMultiline('true');");
        Log.i(TAG, "onDomLoaded");
    }

    @Override
    public void onSelectionChanged(Map<String, String> selectionArgs) {
        Log.i(TAG, "onSelectionChanged(), data=" + new Gson().toJson(selectionArgs));
    }

    @Override
    public void onSelectionStyleChanged(Map<String, Boolean> changeSet) {
        Log.i(TAG, "onSelectionStyleChanged(), data=" + new Gson().toJson(changeSet));
    }

    @Override
    public void onMediaTapped(String mediaId, String url, JSONObject meta, String uploadStatus) {
    }

    @Override
    public void onLinkTapped(String url, String title) {
        Log.i(TAG, "onLinkTapped(), title=" + title + ", url=" + url);

        DialogUtils.editLink(mWebView.getContext(), title, url, new DialogUtils.ChangedListener() {
            @Override
            public void onChanged(String title, String link) {
                Log.i(TAG, "change, title=" + title + ", link=" + link);
            }
        });
    }

    @Override
    public void onGetHtmlResponse(Map<String, String> responseArgs) {
        Log.i(TAG, "onSelectionChanged(), data=" + new Gson().toJson(responseArgs));
    }
}
