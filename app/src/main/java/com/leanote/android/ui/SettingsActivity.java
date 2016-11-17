package com.leanote.android.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.leanote.android.R;
import com.leanote.android.model.NewAccount;
import com.leanote.android.networking.retrofitapi.model.BaseResponse;
import com.leanote.android.service.AccountService;
import com.leanote.android.util.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SettingsActivity extends BaseActivity {

    private final String[] mEditors = new String[]{"RichText", "Markdown"};

    @BindView(R.id.tv_editor)
    TextView mEditorTv;
    @BindView(R.id.tv_image_size)
    TextView mImageSizeTv;
    @BindView(R.id.iv_avatar)
    ImageView mAvatarIv;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTv;
    @BindView(R.id.tv_email)
    TextView mEmailTv;
    @BindView(R.id.tv_host)
    TextView mHostTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolBar((Toolbar) findViewById(R.id.toolbar));
        ButterKnife.bind(this);
        refresh();
    }

    @OnClick(R.id.ll_editor)
    void selectEditor() {
        new AlertDialog.Builder(this)
                .setTitle("Choose editor")
                .setSingleChoiceItems(mEditors, AccountService.getCurrent().getDefaultEditor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NewAccount account = AccountService.getCurrent();
                        account.setDefaultEditor(which);
                        account.update();
                        mEditorTv.setText(mEditors[which]);
                    }
                })
                .setCancelable(true)
                .show();
    }

    @OnClick(R.id.ll_log_out)
    void clickedLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure to log out?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        AccountService.logout();
                        Intent intent = new Intent(SettingsActivity.this, NewSignInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.rl_avatar)
    void clickedAvatar() {

    }

    @OnClick(R.id.ll_user_name)
    void clickedUserName() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sigle_edittext, null);
        final EditText mUserNameEt = (EditText) view.findViewById(R.id.edit);
        mUserNameEt.setText(AccountService.getCurrent().getUserName());
        new AlertDialog.Builder(this)
                .setTitle("Change username")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String username = mUserNameEt.getText().toString();
                        mUserNameTv.setText(username);
                        changeUsername(username);
                    }
                })
                .show();
    }

    @OnClick(R.id.ll_change_password)
    void clickedPassword() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_passowrd, null);
        final EditText mOldPasswordEt = (EditText) view.findViewById(R.id.et_old_password);
        final EditText mNewPasswordEt = (EditText) view.findViewById(R.id.et_new_password);
        new AlertDialog.Builder(this)
                .setTitle("Change password")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        changePassword(mOldPasswordEt.getText().toString(), mNewPasswordEt.getText().toString());
                    }
                })
                .show();
    }

    private void changeUsername(final String username) {
        AccountService.changeUserName(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.showToast(SettingsActivity.this, "Network error");
                        mUserNameTv.setText(AccountService.getCurrent().getUserName());
                    }

                    @Override
                    public void onNext(BaseResponse baseResponse) {
                        if (baseResponse.isOk()) {
                            NewAccount account = AccountService.getCurrent();
                            account.setUserName(username);
                            account.update();
                        } else {
                            mUserNameTv.setText(AccountService.getCurrent().getUserName());
                            ToastUtils.showToast(SettingsActivity.this, "Change username failed");
                        }
                    }
                });
    }

    private void changePassword(String oldPassword, String newPassword) {
        AccountService.changePassword(oldPassword, newPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.showToast(SettingsActivity.this, "Change password failed");
                    }

                    @Override
                    public void onNext(BaseResponse baseResponse) {
                        if (!baseResponse.isOk()) {
                            ToastUtils.showToast(SettingsActivity.this, "Change password failed");
                        }
                    }
                });
    }

    private void refresh() {
        NewAccount current = AccountService.getCurrent();
        mEditorTv.setText(mEditors[current.getDefaultEditor()]);
        mUserNameTv.setText(current.getUserName());
        mEmailTv.setText(current.getEmail());
        mHostTv.setText(current.getHost());
        Glide.with(this)
                .load(current.getAvatar())
                .centerCrop()
                .into(mAvatarIv);
    }
}
