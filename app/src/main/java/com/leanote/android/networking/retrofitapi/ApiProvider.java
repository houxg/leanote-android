package com.leanote.android.networking.retrofitapi;


import com.leanote.android.model.AccountHelper;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

public class ApiProvider {

    private Retrofit mLeanoteRetrofit;

    private static class SingletonHolder {
        private final static ApiProvider INSTANCE = new ApiProvider();
    }

    public static ApiProvider getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private ApiProvider() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        HttpUrl url = request.url();
                        String path = url.encodedPath();
                        HttpUrl newUrl = url;
                        if (shouldAddTokenToQuery(path)) {
                            newUrl = url.newBuilder()
                                    .addQueryParameter("token", AccountHelper.getDefaultAccount().getAccessToken())
                                    .build();
                        }
                        Request newRequest = request.newBuilder()
                                .url(newUrl)
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .addNetworkInterceptor(interceptor)
                .build();
        mLeanoteRetrofit = new Retrofit.Builder()
                .baseUrl("https://leanote.com/api/")
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(new LeaResponseConverterFactory())
                .build();
    }

    private static boolean shouldAddTokenToQuery(String path) {
        return !path.startsWith("/api/auth/login")
//                && !path.startsWith("/api/note/updateNote")
                && !path.startsWith("/api/auth/register");

    }

    public AuthApi getAuthApi() {
        return mLeanoteRetrofit.create(AuthApi.class);
    }

    public NoteApi getNoteApi() {
        return mLeanoteRetrofit.create(NoteApi.class);
    }

    public UserApi getUserApi() {
        return mLeanoteRetrofit.create(UserApi.class);
    }

    public NotebookApi getNotebookApi() {
        return mLeanoteRetrofit.create(NotebookApi.class);
    }

}
