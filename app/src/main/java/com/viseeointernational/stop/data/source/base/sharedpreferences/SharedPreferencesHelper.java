package com.viseeointernational.stop.data.source.base.sharedpreferences;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String TAG = SharedPreferencesHelper.class.getSimpleName();

    private static final String KEY_IS_FIRST_START = "is_first_start";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context, String name) {
        sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public boolean setIsFirstStart(boolean isFirstStart) {
        editor.putBoolean(KEY_IS_FIRST_START, isFirstStart);
        return editor.commit();
    }

    public boolean getIsFirstStart() {
        return sharedPreferences.getBoolean(KEY_IS_FIRST_START, true);
    }

}
