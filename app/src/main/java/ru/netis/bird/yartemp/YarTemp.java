package ru.netis.bird.yartemp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by bird on 19.03.2015
 */
public class YarTemp extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "YarTemp onEnabled");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "YarTemp onUpdate " + Arrays.toString(appWidgetIds));

        SharedPreferences sp = context.getSharedPreferences(
                ConfigActivity.YARTEMP_PREF, Context.MODE_PRIVATE);
        for (int id : appWidgetIds) {
            UpdateWidget updateWidget = new UpdateWidget(context, appWidgetManager, sp, id);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        Log.d(Constants.LOG_TAG, "YarTemp onReceive");

        // Извлекаем ID экземпляра
        int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Bundle extras = intent.getExtras();
        Log.d(Constants.LOG_TAG, "YarTemp onReceive");
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Log.d(Constants.LOG_TAG, "YarTemp onReceive [" + mAppWidgetId + "]");
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Читаем значения
            SharedPreferences sp = context.getSharedPreferences(
                    ConfigActivity.YARTEMP_PREF, Context.MODE_PRIVATE);

            // Проверяем, что это intent от нажатия на третью зону
            if (intent.getAction().equalsIgnoreCase(Constants.ACTION_UPDATE)) {
                Log.d(Constants.LOG_TAG, "YarTemp onReceive [" + mAppWidgetId + "] ACTION_UPDATE");
                UpdateWidget updateWidget = new UpdateWidget(context, AppWidgetManager.getInstance(context), sp, mAppWidgetId);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "YarTemp onDeleted " + Arrays.toString(appWidgetIds));

        // Удаляем Preferences
        Editor editor = context.getSharedPreferences(
                ConfigActivity.YARTEMP_PREF, Context.MODE_PRIVATE).edit();
        for (int widgetID : appWidgetIds) {
            editor.remove(ConfigActivity.YARTEMP_DEGREES + widgetID);
            editor.remove(ConfigActivity.YARTEMP_PERIOD + widgetID);
        }
        editor.apply();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "YarTemp onDisabled");
    }

}
