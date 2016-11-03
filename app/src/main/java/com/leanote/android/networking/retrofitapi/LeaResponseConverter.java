package com.leanote.android.networking.retrofitapi;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.leanote.android.networking.retrofitapi.model.BaseResponse;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

public class LeaResponseConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final TypeAdapter<T> adapter;

    LeaResponseConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override public T convert(ResponseBody value) throws IOException {
        String jsonString = value.string();
        try {
            return adapter.fromJson(jsonString);
        } catch (Exception ex) {
            Log.i("LeaResponseConverter", ex.getMessage());
            BaseResponse response = gson.fromJson(jsonString, BaseResponse.class);
            throw new LeaFailure(response);
        }finally {
            value.close();
        }
    }
}