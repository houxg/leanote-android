package com.leanote.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.db.LeanoteDbManager;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.model.NotebookInfo;
import com.leanote.android.networking.SSLCertsViewActivity;
import com.leanote.android.networking.SelfSignedSSLCertsManager;
import com.leanote.android.ui.lea.LeaActivity;
import com.leanote.android.ui.note.EditNotebookActivity;
import com.leanote.android.ui.note.NotePreviewActivity;
import com.leanote.android.ui.note.NotesInNotebookActivity;
import com.leanote.android.ui.note.NoteEditActivity;
import com.leanote.android.ui.post.BlogHomeActivity;
import com.leanote.android.ui.search.SearchActivity;
import com.leanote.android.util.AppLog;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by binnchx on 8/27/15.
 */
public class ActivityLauncher {

    private static final String ARG_DID_SLIDE_IN_FROM_RIGHT = "did_slide_in_from_right";

    public static void showSignInForResult(Activity activity) {
        Intent intent = new Intent(activity, NewSignInActivity.class);
        activity.startActivityForResult(intent, RequestCodes.ADD_ACCOUNT);
    }

    public static void newAccountForResult(Activity activity) {
        Intent intent = new Intent(activity, SignUpActivity.class);
        activity.startActivityForResult(intent, SignInActivity.CREATE_ACCOUNT_REQUEST);
    }

    public static void viewSSLCerts(Context context) {
        try {
            Intent intent = new Intent(context, SSLCertsViewActivity.class);
            SelfSignedSSLCertsManager selfSignedSSLCertsManager = SelfSignedSSLCertsManager.getInstance(context);
            String lastFailureChainDescription =
                    selfSignedSSLCertsManager.getLastFailureChainDescription().replaceAll("\n", "<br/>");
            intent.putExtra(SSLCertsViewActivity.CERT_DETAILS_KEYS, lastFailureChainDescription);
            context.startActivity(intent);
        } catch (GeneralSecurityException e) {
            AppLog.e(AppLog.T.API, e);
        } catch (IOException e) {
            AppLog.e(AppLog.T.API, e);
        }
    }

    public static void addNewNoteForResult(Activity context) {

        // Create a new post object
        NoteInfo newNote = new NoteInfo();
        NotebookInfo notebook = AppDataBase.getRecentNoteBook(AccountHelper.getDefaultAccount().getUserId());
        newNote.setNoteBookId(notebook.getNotebookId());
        newNote.save();
        Intent intent = NoteEditActivity.getOpenIntent(context, newNote.getId());
        context.startActivityForResult(intent, RequestCodes.EDIT_NOTE);
    }

    public static void editNoteForResult(Activity activity, long noteId) {
        Intent intent = NoteEditActivity.getOpenIntent(activity, noteId);
        activity.startActivityForResult(intent, RequestCodes.EDIT_NOTE);
    }

    public static void editNotebookForResult(Activity activity, long localNotebookId) {
        Intent intent = new Intent(activity.getApplicationContext(), EditNotebookActivity.class);
        intent.putExtra(EditNotebookActivity.EXTRA_NEW_NOTEBOOK_ID, localNotebookId);
        intent.putExtra(EditNotebookActivity.EXTRA_IS_NEW_NOTEBOOK, false);
        activity.startActivityForResult(intent, RequestCodes.EDIT_NOTEBOOK);
    }

    public static void viewNotebookForResult(Activity activity, Long localNotebookId) {
        Intent intent = new Intent(activity.getApplicationContext(), NotesInNotebookActivity.class);
        intent.putExtra(EditNotebookActivity.EXTRA_LOCAL_NOTEBOOK_ID, localNotebookId);
        activity.startActivityForResult(intent, RequestCodes.VIEW_NOTEBOOK);
    }


    public static void slideOutToRight(Activity activity) {
        if (activity != null
                && activity.getIntent() != null
                && activity.getIntent().hasExtra(ARG_DID_SLIDE_IN_FROM_RIGHT)) {
            activity.overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_right);
        }
    }

    public static void addNewNotebookForResult(Activity context) {
        NotebookInfo newNotebook = new NotebookInfo();
        newNotebook.setUserId(AccountHelper.getDefaultAccount().getUserId());
        //WordPress.wpDB.savePost(newPost);
        LeanoteDbManager.getInstance().addNotebook(newNotebook);
        Intent intent = new Intent(context, EditNotebookActivity.class);
        intent.putExtra(EditNotebookActivity.EXTRA_NEW_NOTEBOOK_ID, newNotebook.getId());
        intent.putExtra(EditNotebookActivity.EXTRA_IS_NEW_NOTEBOOK, true);
        context.startActivityForResult(intent, RequestCodes.NEW_NOTEBOOK);
    }

    public static void previewNoteForResult(Activity activity, Long id) {
        Intent intent = NotePreviewActivity.getOpenIntent(activity, id);
        slideInFromRightForResult(activity, intent, RequestCodes.PREVIEW_NOTE);
    }

    public static void slideInFromRightForResult(Activity activity, Intent intent, int requestCode) {
        intent.putExtra(ARG_DID_SLIDE_IN_FROM_RIGHT, true);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                activity,
                R.anim.activity_slide_in_from_right,
                R.anim.do_nothing);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, options.toBundle());
    }

    public static void visitBlog(Activity activity){
        Intent intent = new Intent(activity,BlogHomeActivity.class);
        String url = String.format("%s/blog/%s",
                AccountHelper.getDefaultAccount().getHost(),
                AccountHelper.getDefaultAccount().getUserName());

        intent.putExtra("url", url);
        activity.startActivity(intent);
    }

    public static void startSearchForResult(Activity context, Integer type) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra("type", type);
        context.startActivityForResult(intent, RequestCodes.SEARCH_NOTE);
    }

    public static void startLeaForResult(Activity context) {
        Intent intent = new Intent(context, LeaActivity.class);
        intent.putExtra("url", "http://lea.leanote.com");
        context.startActivityForResult(intent, RequestCodes.START_LEA);
    }

}
