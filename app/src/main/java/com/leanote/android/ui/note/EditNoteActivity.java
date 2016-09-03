package com.leanote.android.ui.note;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.leanote.android.R;
import com.leanote.android.db.LeanoteDbManager;
import com.leanote.android.editor.EditorFragment;
import com.leanote.android.editor.EditorFragmentAbstract;
import com.leanote.android.editor.Utils;
import com.leanote.android.model.AccountHelper;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.networking.NetworkRequest;
import com.leanote.android.networking.NetworkUtils;
import com.leanote.android.ui.RequestCodes;
import com.leanote.android.ui.media.LeaMediaUtils;
import com.leanote.android.ui.note.service.NoteUploadService;
import com.leanote.android.util.AppLog;
import com.leanote.android.util.DeviceUtils;
import com.leanote.android.util.LeaHtml;
import com.leanote.android.util.MediaFile;
import com.leanote.android.util.MediaUtils;
import com.leanote.android.util.StringUtils;
import com.leanote.android.util.ToastUtils;
import com.leanote.android.util.helper.ImageUtils;
import com.leanote.android.util.helper.MediaGalleryImageSpan;
import com.leanote.android.widget.LeaViewPager;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EditNoteActivity extends AppCompatActivity
        implements EditorFragmentAbstract.EditorFragmentListener {

    public static final String EXTRA_NOTEID = "noteId";
    public static final String EXTRA_IS_NEW_NOTE = "isNewNote";

    public static final String EXTRA_IS_QUICKPRESS = "isQuickPress";
    public static final String EXTRA_SAVED_AS_LOCAL_DRAFT = "savedAsLocalDraft";
    public static final String STATE_KEY_CURRENT_NOTE = "stateKeyCurrentPost";
    public static final String STATE_KEY_ORIGINAL_NOTE = "stateKeyOriginalPost";
    public static final String STATE_KEY_EDITOR_FRAGMENT = "editorFragment";

    // Context menu positioning
    private static final int SELECT_PHOTO_MENU_POSITION = 0;
    private static final int CAPTURE_PHOTO_MENU_POSITION = 1;

    private static int PAGE_CONTENT = 0;
    private static int PAGE_SETTINGS = 1;
    private static int PAGE_PREVIEW = 2;

    // Moved from EditPostContentFragment
    public static final String NEW_MEDIA_GALLERY = "NEW_MEDIA_GALLERY";
    public static final String NEW_MEDIA_GALLERY_EXTRA_IDS = "NEW_MEDIA_GALLERY_EXTRA_IDS";
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA = "NEW_MEDIA_POST_ID";

    private String mMediaCapturePath = "";

    private boolean mIsNewNote;

    private boolean mHasSetNoteContent;

    private NoteInfo mNote;

    private NoteInfo mOriginalNote;

    private EditorFragment mEditorFragment;


    private EditNoteSettingsFragment mEditNoteSettingsFragment;

    private EditNotePreviewFragment mEditNotePreviewFragment;

    SectionsPagerAdapter mSectionsPagerAdapter;

    LeaViewPager mViewPager;

    private int mMaxThumbWidth = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        FragmentManager fragmentManager = getFragmentManager();
        Bundle extras = getIntent().getExtras();
        String action = getIntent().getAction();

        if (savedInstanceState == null) {
            if (extras != null) {
                // Load post from the postId passed in extras
                long localNoteId = extras.getLong(EXTRA_NOTEID, 0L);
                mIsNewNote = extras.getBoolean(EXTRA_IS_NEW_NOTE);

                mNote = LeanoteDbManager.getInstance().getLocalNoteById(localNoteId);
                mOriginalNote = LeanoteDbManager.getInstance().getLocalNoteById(localNoteId);

                mEditorFragment = new EditorFragment();
            } else {
                // A postId extra must be passed to this activity
                showErrorAndFinish(R.string.note_not_found);
                return;
            }
        } else {
            if (savedInstanceState.containsKey(STATE_KEY_ORIGINAL_NOTE)) {
                try {
                    mNote = (NoteInfo) savedInstanceState.getSerializable(STATE_KEY_CURRENT_NOTE);
                    mOriginalNote = (NoteInfo) savedInstanceState.getSerializable(STATE_KEY_ORIGINAL_NOTE);
                } catch (ClassCastException e) {
                    Log.e("editor error", ":", e);
                    mNote = null;
                }
            }
            mEditorFragment = (EditorFragment) fragmentManager.getFragment(savedInstanceState, STATE_KEY_EDITOR_FRAGMENT);
        }

//        if (mHasSetNoteContent = mEditorFragment != null) {
//            mEditorFragment.setImageLoader(Leanote.imageLoader);
//        }


        // Ensure we have a valid post
        if (mNote == null) {
            showErrorAndFinish(R.string.note_not_found);
            return;
        }


        setTitle(StringUtils.unescapeHTML("leanote"));

        mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (LeaViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPagingEnabled(false);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                invalidateOptionsMenu();
                if (position == PAGE_CONTENT) {
                    //setTitle(AccountHelper.getDefaultAccount().getUserName());
                    setTitle("leanote");
                } else if (position == PAGE_SETTINGS) {
                    setTitle(R.string.note_settings);
                } else if (position == PAGE_PREVIEW) {
                    setTitle(R.string.preview_note);
                    AppLog.i("call... save note");
                    saveNote(true);
                    if (mEditNotePreviewFragment != null) {
                        mEditNotePreviewFragment.loadNote();
                    }
                }
            }
        });

    }

    @Override
    public void onEditorFragmentInitialized() {
        fillContentEditorFields();
    }

    @Override
    public void onSettingsClicked() {
        mViewPager.setCurrentItem(PAGE_SETTINGS);
    }

    @Override
    public void onAddMediaClicked() {

    }

    @Override
    public void onFeaturedImageChanged(int mediaId) {

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, SELECT_PHOTO_MENU_POSITION, 0, getResources().getText(R.string.select_photo));
        if (DeviceUtils.getInstance().hasCamera(this)) {
            menu.add(0, CAPTURE_PHOTO_MENU_POSITION, 0, getResources().getText(R.string.media_add_popup_capture_photo));
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SELECT_PHOTO_MENU_POSITION:
                launchPictureLibrary();
                return true;
            case CAPTURE_PHOTO_MENU_POSITION:
                launchCamera();
                return true;
            default:
                return false;
        }
    }


    @Override
    public void saveMediaFile(MediaFile mediaFile) {
        LeanoteDbManager.getInstance().saveMediaFile(mediaFile);
    }

    private void launchCamera() {
        LeaMediaUtils.launchCamera(this, new LeaMediaUtils.LaunchCameraCallback() {
            @Override
            public void onMediaCapturePathReady(String mediaCapturePath) {
                mMediaCapturePath = mediaCapturePath;
                AppLockManager.getInstance().setExtendedTimeout();
            }
        });
    }


    private void launchPictureLibrary() {
        try {
            LeaMediaUtils.launchPictureLibrary(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //AppLockManager.getInstance().setExtendedTimeout();
    }


    private int getMaximumThumbnailWidthForEditor() {
        if (mMaxThumbWidth == 0) {
            mMaxThumbWidth = ImageUtils.getMaximumThumbnailWidthForEditor(this);
        }
        return mMaxThumbWidth;
    }


    private class LoadNoteContentTask extends AsyncTask<Void, Spanned, Spanned> {

        @Override
        protected Spanned doInBackground(Void... params) {
            String content = fetchNoteContent(getNote().getNoteId());

            if (org.apache.commons.lang.StringUtils.isEmpty(content)) {
                return new SpannableString("");
            }
            return LeaHtml.fromHtml(content, EditNoteActivity.this, getNote(), getMaximumThumbnailWidthForEditor());
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            if (spanned != null) {
                mEditorFragment.setContent(spanned);
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || (requestCode == RequestCodes.TAKE_PHOTO)) {
            switch (requestCode) {
//                case MediaPickerActivity.ACTIVITY_REQUEST_CODE_MEDIA_SELECTION:
//                    if (resultCode == MediaPickerActivity.ACTIVITY_RESULT_CODE_MEDIA_SELECTED) {
//                        new HandleMediaSelectionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
//                                data);
//                    } else if (resultCode == MediaPickerActivity.ACTIVITY_RESULT_CODE_GALLERY_CREATED) {
//                        handleGalleryResult(data);
//                    }
//                    break;
//                case MediaGalleryActivity.REQUEST_CODE:
//                    if (resultCode == Activity.RESULT_OK) {
//                        handleMediaGalleryResult(data);
//                    }
//                    break;
//                case MediaGalleryPickerActivity.REQUEST_CODE:
//                    AnalyticsTracker.track(Stat.EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY);
//                    if (resultCode == Activity.RESULT_OK) {
//                        handleMediaGalleryPickerResult(data);
//                    }
//                    break;
                case RequestCodes.PICTURE_LIBRARY:
                    AppLog.i("add photo callback...");
                    Uri imageUri = data.getData();
                    fetchMedia(imageUri);

                    break;
                case RequestCodes.TAKE_PHOTO:
                    if (resultCode == Activity.RESULT_OK) {
                        try {
                            File f = new File(mMediaCapturePath);
                            Uri capturedImageUri = Uri.fromFile(f);
                            if (!addMedia(capturedImageUri)) {
                                ToastUtils.showToast(this, R.string.gallery_error, ToastUtils.Duration.SHORT);
                            }
//                            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
//                                    + Environment.getExternalStorageDirectory())));

                            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
                                    + Environment.getExternalStorageDirectory())));

                        } catch (RuntimeException e) {
                            AppLog.e(AppLog.T.POSTS, e);
                        }
                    } else if (TextUtils.isEmpty(mEditorFragment.getContent())) {
                        // TODO: check if it was mQuickMediaType > -1
                        // Quick Photo was cancelled, delete post and finish activity

                        //WordPress.wpDB.deletePost(getPost());
                        finish();
                    }
                    break;

            }
        }
    }


    private void fetchMedia(Uri mediaUri) {
        if (URLUtil.isNetworkUrl(mediaUri.toString())) {
            // Create an AsyncTask to download the file
            new DownloadMediaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaUri);
        } else {
            // It is a regular local image file
            if (!addMedia(mediaUri)) {
                Toast.makeText(EditNoteActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    private class DownloadMediaTask extends AsyncTask<Uri, Integer, Uri> {
        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return MediaUtils.downloadExternalMedia(EditNoteActivity.this, imageUri);
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(EditNoteActivity.this, R.string.download, Toast.LENGTH_SHORT).show();
        }

        protected void onPostExecute(Uri newUri) {
            if (newUri != null) {
                addMedia(newUri);
            } else {
                Toast.makeText(EditNoteActivity.this, getString(R.string.error_downloading_image), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    private boolean addMedia(Uri imageUri) {
        AppLog.i("start to add media");
        if (imageUri != null && !MediaUtils.isInMediaStore(imageUri) && !imageUri.toString().startsWith("/")) {
            imageUri = MediaUtils.downloadExternalMedia(this, imageUri);
        }

        if (imageUri == null) {
            return false;
        }

        String mediaTitle;
        //only support image
        mediaTitle = ImageUtils.getTitleForWPImageSpan(this, imageUri.getEncodedPath());

        //
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(new ObjectId().toString());
        mediaFile.setNoteID(getNote().getNoteId());
        mediaFile.setTitle(mediaTitle);

        String filePath = Utils.getImageRealPath(imageUri);
        if (filePath != null && filePath.startsWith("file://")) {
            filePath = filePath.replace("file://", "");
        }
        mediaFile.setFilePath(filePath);

        String leanoteImageUrl = String.format("%s/api/file/getImage?fileId=%s",
                AccountHelper.getDefaultAccount().getHost(),
                mediaFile.getId());

        mediaFile.setFileURL(leanoteImageUrl);
        //更新到db
        LeanoteDbManager.getInstance().saveMediaFile(mediaFile);
        mEditorFragment.appendMediaFile(mediaFile, mediaFile.getFilePath(),null);

        return true;
    }


    private String fetchNoteContent(String noteId) {
        NoteInfo note = LeanoteDbManager.getInstance().getLocalNoteByNoteId(noteId);
        if (note != null && org.apache.commons.lang.StringUtils.isNotEmpty(note.getContent())) {
            return note.getContent();
        }

        String noteApi = String.format("%s/api/note/getNoteContent?noteId=%s&token=%s",
                AccountHelper.getDefaultAccount().getHost(), noteId,
                AccountHelper.getDefaultAccount().getAccessToken());


        String response;
        String content = null;
        try {
            response = NetworkRequest.syncGetRequest(noteApi);
            JSONObject json = new JSONObject(response);
            content = json.getString("Content");
            LeanoteDbManager.getInstance().saveNoteContent(noteId, content);

        } catch (Exception e) {
            AppLog.e(AppLog.T.API, "fetch note content error", e);
            ToastUtils.showToast(this, R.string.fetch_note_content_fail);
        }
        return content;
    }


    private void fillContentEditorFields() {
        // Needed blog settings needed by the editor
        if (AccountHelper.getDefaultAccount().getUserId() != null) {
            mEditorFragment.setFeaturedImageSupported(true);
            //mEditorFragment.setBlogSettingMaxImageWidth(WordPress.getCurrentBlog().getMaxImageWidth());
        }

        // Set post title and content
        NoteInfo note = getNote();
        if (note != null) {
            if (!TextUtils.isEmpty(note.getContent()) && !mHasSetNoteContent) {
                mHasSetNoteContent = true;
                if (!mIsNewNote && org.apache.commons.lang.StringUtils.isEmpty(note.getContent())) {
                    // Load local post content in the background, as it may take time to generate images
                    new LoadNoteContentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else if (!TextUtils.isEmpty(note.getContent())){
                    String content = note.getContent().replaceAll("\uFFFC", "").replace("<img&nbsp;", "<img")
                            .replace("</p><br>", "");
                    mEditorFragment.setContent(content);
                }
            }
            if (!TextUtils.isEmpty(note.getTitle())) {
                mEditorFragment.setTitle(note.getTitle());
            }
            // TODO: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            mEditorFragment.setLocalDraft(note.getUsn() == 0);
        }


    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    // TODO: switch between legacy and new editor here (AB test?)
                    return new EditorFragment();
                //return new LegacyEditorFragment();
                case 1:
                    return new EditNoteSettingsFragment();
                default:
                    return new EditNotePreviewFragment();
            }

        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);

            switch (position) {
                case 0:
                    mEditorFragment = (EditorFragment) fragment;
                    break;
                case 1:
                    mEditNoteSettingsFragment = (EditNoteSettingsFragment) fragment;
                    break;
                case 2:
                    mEditNotePreviewFragment = (EditNotePreviewFragment) fragment;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }


    private void showErrorAndFinish(int errorMessageId) {
        Toast.makeText(this, getResources().getText(errorMessageId), Toast.LENGTH_LONG).show();
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_note, menu);

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves both post objects so we can restore them in onCreate()
        AppLog.i("call--- onSaveInstanceState");
        saveNote(true);
        outState.putSerializable(STATE_KEY_CURRENT_NOTE, mNote);
        outState.putSerializable(STATE_KEY_ORIGINAL_NOTE, mOriginalNote);

        if (mEditorFragment != null) {
            getFragmentManager().putFragment(outState, STATE_KEY_EDITOR_FRAGMENT, mEditorFragment);
        }
    }

    private void updateNoteObject(boolean isAutosave) {
        if (mNote == null) {
            AppLog.e(AppLog.T.POSTS, "Attempted to save an invalid Post.");
            return;
        }

        // Update post object from fragment fields
        if (mEditorFragment != null) {
            updateNoteContent(isAutosave);
        }
        if (mEditNoteSettingsFragment != null) {
            mEditNoteSettingsFragment.updateNoteSettings();
        }
    }

    @Override
    public void onBackPressed() {

        if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
            mViewPager.setCurrentItem(PAGE_CONTENT);
            invalidateOptionsMenu();
            return;
        }

        if (mEditorFragment != null && !mEditorFragment.onBackPressed()) {
            saveAndFinish();
        }
    }


    private void updateNoteContent(boolean isAutoSave) {
        NoteInfo note = getNote();

        if (note == null) {
            return;
        }
        String title = StringUtils.notNullStr((String) mEditorFragment.getTitle());
        note.setTitle(title);

        AppLog.i("orginal content:" + note.getContent() + "---end");
        AppLog.i("new content:" + mEditorFragment.getSpannedContent() + "---end");
        if (note.getContent().equals(mEditorFragment.getContent())) {
            AppLog.i("note content has no any change.");
            return;
        }
        SpannableStringBuilder noteContent;
        if (mEditorFragment.getSpannedContent() != null) {
            // needed by the legacy editor to save local drafts
            try {
                noteContent = new SpannableStringBuilder(mEditorFragment.getSpannedContent());
            } catch (IndexOutOfBoundsException e) {
                // A core android bug might cause an out of bounds exception, if so we'll just use the current editable
                // See https://code.google.com/p/android/issues/detail?id=5164
                noteContent = new SpannableStringBuilder(StringUtils.notNullStr((String) mEditorFragment.getContent()));
            }
        } else {
            noteContent = new SpannableStringBuilder(StringUtils.notNullStr((String) mEditorFragment.getContent()));
        }

        String content = null;


        //note 可用usn是否为0表示isLocalDraft
        //本地草稿笔记如何更新note的fileId?
        if (note.getUsn() == 0) {
            // remove suggestion spans, they cause craziness in WPHtml.toHTML().
            CharacterStyle[] characterStyles = noteContent.getSpans(0, noteContent.length(), CharacterStyle.class);
            for (CharacterStyle characterStyle : characterStyles) {
                if (characterStyle instanceof SuggestionSpan) {
                    noteContent.removeSpan(characterStyle);
                }
            }
            content = LeaHtml.toHtml(noteContent);
            // replace duplicate <p> tags so there's not duplicates, trac #86
            content = content.replace("<p><p>", "<p>");
            content = content.replace("</p></p>", "</p>");
            content = content.replace("<br><br>", "<br>");
            // sometimes the editor creates extra tags
            content = content.replace("</strong><strong>", "").replace("</em><em>", "").replace("</u><u>", "")
                    .replace("</strike><strike>", "").replace("</blockquote><blockquote>", "");
        } else {
            if (!isAutoSave) {
                // Add gallery shortcode
                MediaGalleryImageSpan[] gallerySpans = noteContent.getSpans(0, noteContent.length(),
                        MediaGalleryImageSpan.class);
                for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
                    int start = noteContent.getSpanStart(gallerySpan);
                    noteContent.removeSpan(gallerySpan);
                    noteContent.insert(start, LeaHtml.getGalleryShortcode(gallerySpan));
                }
            }


        }

        //获取图片id并更新到note表的fileid字段
        AppLog.i("save note content:" + noteContent);
//        LeaImageSpan[] imageSpans = noteContent.getSpans(0, noteContent.length(), LeaImageSpan.class);
//        if (imageSpans.length != 0) {
//            List<String> fileIds = new ArrayList<>();
//            for (LeaImageSpan wpIS : imageSpans) {
//                MediaFile mediaFile = wpIS.getMediaFile();
//                AppLog.i("mediafile:" + mediaFile);
//                if (mediaFile == null)
//                    continue;
//
//                mediaFile.setFileName(wpIS.getImageSource().toString());
//                //mediaFile.setFilePath(wpIS.getImageSource().toString());
//
//                LeanoteDbManager.getInstance().saveMediaFile(mediaFile);
//                AppLog.i("get media id:" + mediaFile.getId());
//                fileIds.add(mediaFile.getId());
//
//                int tagStart = noteContent.getSpanStart(wpIS);
//                if (!isAutoSave) {
//                    noteContent.removeSpan(wpIS);
//
//                    // network image has a mediaId
//                    if (mediaFile.getMediaId() != null && mediaFile.getMediaId().length() > 0) {
//                        noteContent.insert(tagStart, LeaHtml.getContent(wpIS));
//                    } else {
//                        // local image for upload
//                        //http://leanote.com/api/file/getImage?fileId=24位本地LocalFileId 或
//                        noteContent.insert(tagStart,
//                                "<img android-uri=\"" + wpIS.getImageSource().toString() + "\" />");
//                    }
//                }
//            }
//
//            String fileIdStr = org.apache.commons.lang.StringUtils.join(fileIds, ",");
//            note.setFileIds(fileIdStr);
//            AppLog.i("fileids:" + fileIdStr);
//            //LeanoteDbManager.getInstance().updateNote(note);
//        } else {
//            //清空fileids
//            note.setFileIds("");
//        }



        if (note.getUsn() != 0) {
            content = noteContent.toString();
        }
        setNoteFileIds(content);
        note.setContent(content);

    }

    private void setNoteFileIds(String content) {
        //note upload 前需要更新图片链接
        if (TextUtils.isEmpty(content)) {
            AppLog.i("content is empty!");
            return;
        }
        String imageTagsPattern = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
        Pattern pattern = Pattern.compile(imageTagsPattern);
        Matcher matcher = pattern.matcher(content);

        List<String> imageTags = new ArrayList<>();
        while (matcher.find()) {
            imageTags.add(matcher.group());
        }

        if (imageTags.size() == 0) {
            mNote.setFileIds("");
        }

        for (String tag : imageTags) {
            Pattern p = Pattern.compile("src=\"([^\"]+)\"");
            Matcher m = p.matcher(tag);
            if (m.find()) {
                String imageUri = m.group(1);
                if (!"".equals(imageUri)) {
                    MediaFile mediaFile = LeanoteDbManager.getInstance().getMediaFile(imageUri);
                    if (mediaFile == null) {
                        continue;
                    }

                    String fileId = mNote.getFileIds();
                    if (TextUtils.isEmpty(fileId)) {
                        fileId = mediaFile.getId();
                    } else if (!fileId.contains(mediaFile.getId())){
                        fileId = fileId + "," + mediaFile.getId();
                    }
                    mNote.setFileIds(fileId);

                }
            }
        }


    }


    public NoteInfo getNote() {
        return mNote;
    }


    private void saveNote(boolean isAutosave) {
        saveNote(isAutosave, true);
    }

    private void saveNote(boolean isAutosave, boolean updatePost) {
        if (updatePost) {
            updateNoteObject(isAutosave);
        }


        LeanoteDbManager.getInstance().updateNote(mNote);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_save_post || itemId == android.R.id.home) {
            // If the post is new and there are no changes, don't publish
            updateNoteObject(false);

            if (mViewPager.getCurrentItem() == PAGE_SETTINGS) {
                mViewPager.setCurrentItem(PAGE_CONTENT);
                return true;
            }

//            if (!mPost.isPublishable()) {
//                ToastUtils.showToast(this, R.string.error_publish_empty_post, Duration.SHORT);
//                return false;
//            }

            if (mNote.isDirty() || mNote.hasChanges(mOriginalNote) || !mNote.isUploadSucc()) {
                saveNote(false, false);
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    ToastUtils.showToast(this, R.string.no_network_message, ToastUtils.Duration.SHORT);
                    return false;
                }

                mNote = LeanoteDbManager.getInstance().getLocalNoteById(mNote.getId());
                NoteUploadService.addNoteToUpload(mNote);
                startService(new Intent(this, NoteUploadService.class));
            }

            setResult(RESULT_OK);
            Intent intent = new Intent();
            intent.putExtra(EditNotebookActivity.EXTRA_SERVER_NOTEBOOK_ID, mNote.getNoteBookId());
            setIntent(intent);
            finish();
            return true;
        }
//        else if (itemId == R.id.menu_preview_post) {
//            mViewPager.setCurrentItem(PAGE_PREVIEW);
//        }
        else if (itemId == R.id.menu_settings_post) {
            mViewPager.setCurrentItem(PAGE_SETTINGS);
            //finish();
            return true;
        }
//        else if (itemId == android.R.id.home) {
//            if (mViewPager.getCurrentItem() > PAGE_CONTENT) {
//                mViewPager.setCurrentItem(PAGE_CONTENT);
//                //invalidateOptionsMenu();
//            } else {
//                updateNoteObject(false);
//                if (mNote.hasChanges(mOriginalNote)) {
//                    saveAndFinish();
//                }
//
//                setResult(RESULT_OK);
//                finish();
//            }
//            return true;
//        }
        return false;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem previewMenuItem = menu.findItem(R.id.menu_preview_post);
//        if (mViewPager != null && mViewPager.getCurrentItem() > PAGE_CONTENT) {
//            previewMenuItem.setVisible(false);
//        } else {
//            previewMenuItem.setVisible(true);
//        }

        return super.onPrepareOptionsMenu(menu);
    }


    private void saveAndFinish() {

        AppLog.i("call... saveAndFinish");
        saveNote(true);
        if (mEditorFragment != null && hasEmptyContentFields()) {
            // new and empty post? delete it
            if (mIsNewNote) {
                LeanoteDbManager.getInstance().deleteNote(mNote.getId());
            }
        } else if (mOriginalNote != null && !mNote.hasChanges(mOriginalNote)) {
            // if no changes have been made to the post, set it back to the original don't save it
            //WordPress.wpDB.updatePost(mOriginalPost);

        } else {
            // changes have been made, save the post and ask for the post list to refresh.
            // We consider this being "manual save", it will replace some Android "spans" by an html
            // or a shortcode replacement (for instance for images and galleries)
            mNote.setIsDirty(true);
            saveNote(false);
            Intent i = new Intent();
            i.putExtra(EXTRA_SAVED_AS_LOCAL_DRAFT, true);
            //i.putExtra(EXTRA_IS_PAGE, mIsPage);
            setResult(RESULT_OK, i);

            ToastUtils.showToast(this, R.string.editor_toast_changes_saved);
        }
        finish();
    }

    private boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(mEditorFragment.getTitle()) && TextUtils.isEmpty(mEditorFragment.getContent());
    }


}
