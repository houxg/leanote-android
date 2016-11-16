package com.leanote.android.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.leanote.android.R;
import com.leanote.android.db.AppDataBase;
import com.leanote.android.model.NewAccount;
import com.leanote.android.networking.retrofitapi.ApiProvider;
import com.leanote.android.service.AccountService;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observer;
import rx.schedulers.Schedulers;

public class NewSignInActivity extends BaseActivity implements TextWatcher {

    private static final String TAG = "NewSignInActivity";

    private static final String LEANOTE_HOST = "https://leanote.com";
    private static final String EXT_IS_CUSTOM_HOST = "ext_is_custom_host";
    private static final String EXT_HOST = "ext_host";

    @BindView(R.id.et_email)
    EditText mEmailEt;
    @BindView(R.id.et_password)
    EditText mPasswordEt;
    @BindView(R.id.tv_sign_in)
    View mSignInBtn;
    @BindView(R.id.tv_custom_host)
    TextView mCustomHostBtn;
    @BindView(R.id.et_custom_host)
    EditText mHostEt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        ButterKnife.bind(this);
        mEmailEt.addTextChangedListener(this);
        mPasswordEt.addTextChangedListener(this);
        refreshHostSetting(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXT_IS_CUSTOM_HOST, (Boolean) mCustomHostBtn.getTag());
        outState.putString(EXT_HOST, mHostEt.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        boolean isCustomHost = savedInstanceState.getBoolean(EXT_IS_CUSTOM_HOST);
        refreshHostSetting(isCustomHost);
        mHostEt.setText(savedInstanceState.getString(EXT_HOST));
    }

    @OnClick(R.id.tv_custom_host)
    void switchHost(View v) {
        refreshHostSetting(!(boolean) mCustomHostBtn.getTag());

    }

    private void refreshHostSetting(boolean isCustomHost) {
        if (isCustomHost) {
            mCustomHostBtn.setText("Use Leanote.com");
            mHostEt.setText("");
        } else {
            mCustomHostBtn.setText("Use custom host");
            mHostEt.setText(LEANOTE_HOST);
        }
        mHostEt.setVisibility(isCustomHost ? View.VISIBLE : View.GONE);
        mCustomHostBtn.setTag(isCustomHost);
    }


    @OnClick(R.id.tv_sign_in)
    void signIn() {
        String email = mEmailEt.getText().toString();
        String password = mPasswordEt.getText().toString();
        final String host = mHostEt.getText().toString().trim();
        ApiProvider.getInstance().init(host);
        AccountService.login(email, password)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<NewAccount>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(NewAccount accountModel) {
                        NewAccount localAccount = AppDataBase.getAccount(accountModel.getEmail(), host);
                        if (localAccount == null) {
                            accountModel.setHost(host);
                            accountModel.insert();
                        } else {
                            localAccount.setAccessToken(accountModel.getAccessToken());
                            localAccount.setUserId(accountModel.getUserId());
                            localAccount.setUserName(accountModel.getUserName());
                            localAccount.update();
                        }
                        Intent intent = MainActivity.getOpenIntent(NewSignInActivity.this, true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String email = mEmailEt.getText().toString();
        String password = mPasswordEt.getText().toString();
        mSignInBtn.setEnabled(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password));
    }
}
