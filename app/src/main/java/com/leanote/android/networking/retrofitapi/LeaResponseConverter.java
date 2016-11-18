package com.leanote.android.networking.retrofitapi;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.leanote.android.model.NoteInfo;
import com.leanote.android.networking.retrofitapi.model.BaseResponse;
import com.leanote.android.util.CollectionUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
            T val = adapter.fromJson(jsonString);
            if (val instanceof NoteInfo) {
                ((NoteInfo)val).updateTags();
                ((NoteInfo)val).updateTime();
            } if (val instanceof List
                    && CollectionUtils.isNotEmpty((Collection) val)
                    && ((List)val).get(0) instanceof NoteInfo) {
                List<NoteInfo> notes = (List<NoteInfo>) val;
                for (NoteInfo note : notes) {
                    note.updateTags();
                    note.updateTime();
                }
            }
            return val;
        } catch (Exception ex) {
            Log.i("LeaResponseConverter", ex.getMessage());
            BaseResponse response = gson.fromJson(jsonString, BaseResponse.class);
            throw new LeaFailure(response);
        }finally {
            value.close();
        }
    }
}