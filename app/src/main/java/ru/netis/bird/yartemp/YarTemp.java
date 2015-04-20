package ru.netis.bird.yartemp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by bird on 19.03.2015
 */
public class YarTemp extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp onEnabled");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp onUpdate " + Arrays.toString(appWidgetIds));

        SharedPreferences sp = context.getSharedPreferences(
                ConfigActivity.YARTEMP_PREF, Context.MODE_PRIVATE);
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, sp, id);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        Log.d(Constans.LOG_TAG, "YarTemp onReceive");

        // Извлекаем ID экземпляра
        int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Bundle extras = intent.getExtras();
        Log.d(Constans.LOG_TAG, "YarTemp onReceive");
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Log.d(Constans.LOG_TAG, "YarTemp onReceive [" + mAppWidgetId + "]");
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Читаем значения
            SharedPreferences sp = context.getSharedPreferences(
                    ConfigActivity.YARTEMP_PREF, Context.MODE_PRIVATE);

            // Проверяем, что это intent от нажатия на третью зону
            if (intent.getAction().equalsIgnoreCase(Constans.ACTION_UPDATE)) {
                Log.d(Constans.LOG_TAG, "YarTemp onReceive [" + mAppWidgetId + "] ACTION_UPDATE");
                updateWidget(context, AppWidgetManager.getInstance(context), sp, mAppWidgetId);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp onDeleted " + Arrays.toString(appWidgetIds));

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
        if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp onDisabled");
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                              SharedPreferences sp, int widgetID) {

        int degreeUnit;
        int periodUpdate;
        PendingIntent pIntent;

        Log.d(Constans.LOG_TAG, "YarTemp updateWidget [" + widgetID + "]");

        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

// текст "uploading..."
        widgetView.setTextViewText(R.id.Updated, context.getString(R.string.loading));

        // Site yartemp.com (певая зона)
        Uri uri = Uri.parse(context.getString(R.string.temp_URL));
        Intent siteIntent = new Intent(Intent.ACTION_VIEW, uri);
        pIntent = PendingIntent.getActivity(context, widgetID, siteIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.llImage, pIntent);

        // Конфигурационный экран (вторая зона)
        Intent configIntent = new Intent(context, ConfigActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        pIntent = PendingIntent.getActivity(context, widgetID, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.llText, pIntent);

        // Экран обновления (третяя зона)
        Intent updateIntent = new Intent(context, YarTemp.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetID});
        pIntent = PendingIntent.getBroadcast(context, widgetID, updateIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.llUpdate, pIntent);

        appWidgetManager.updateAppWidget(widgetID, widgetView);

        // Читаем параметры Preferences
        String periodSt = sp.getString(ConfigActivity.YARTEMP_PERIOD + widgetID, null);
        degreeUnit = sp.getInt(ConfigActivity.YARTEMP_DEGREES + widgetID, -1);

        periodUpdate = Integer.getInteger(periodSt, Constans.DEFAULT_PERIOD);
        degreeUnit = sp.getInt(ConfigActivity.YARTEMP_DEGREES + widgetID, Constans.DEFAULT_UNIT);

        if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp updateWidget [" + widgetID + "] degree unit = " + degreeUnit + " period update = " + periodUpdate);

        // Проверяем подключение к сети
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            ParseURL parseURL = new ParseURL();
            parseURL.setContext(context);
            parseURL.setAm(appWidgetManager);
            parseURL.setWidgetID(widgetID);
            parseURL.setDgreeUnit(degreeUnit);
            parseURL.execute(context.getString(R.string.temp_URL));
        } else {

            Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }

    }

    static class ParseURL extends AsyncTask<String, Void, String[]> {
        Context context;
        AppWidgetManager am;
        int widgetID;

        Element spTemp1;
        Element spTemp1a;
        String currentTempStr;
        float currentTempFl;

        Element spTemp2;
        Element spTemp2a;
        String deltaTempStr;
        float deltaTempFl;

        String currTime;

        int degreeUnit;

        void setContext(Context context) {this.context = context;}
        void setAm(AppWidgetManager am) {this.am = am;}
        void setWidgetID(int widgetID) {this.widgetID = widgetID;}
        void setDgreeUnit(int degreeUnit) {this.degreeUnit = degreeUnit;}

        @Override
        protected String[] doInBackground(String... strings) {
            String[] a = new String[]{"","",""};

            try {
                if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground Connecting to [" + strings[0] + "]");
                Document doc = Jsoup.connect(strings[0]).get();
                Log.d(Constans.LOG_TAG, "YarTemp doInBackground Connected to [" + strings[0] + "]");

                try {
                    spTemp1 = doc.getElementById("spTemp1");
                    spTemp1a = doc.getElementById("spTemp1a");
                    currentTempStr = spTemp1.text() + spTemp1a.text();
                    currentTempFl = Float.valueOf(currentTempStr);
                    if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground currentTemp = " + currentTempFl);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] currentTemp - ERROR");
                }

                try {
                    spTemp2 = doc.getElementById("spTemp2");
                    spTemp2a = doc.getElementById("spTemp2a");
                    deltaTempStr = spTemp2.text() + spTemp2a.text();
                    deltaTempFl = Float.valueOf(deltaTempStr);
                    if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] deltaTemp = " + deltaTempFl);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] deltaTemp - ERROR");
                }

                if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] degreeUnit = " + degreeUnit);
                deltaTempStr = context.getString(R.string.degree_speed); // "в час"
                if (degreeUnit == Constans.FAHRENHEIT) {
                    // Переводим °C в °F
                    currentTempStr = context.getString(R.string.degrees_fahrenheit); // " °F"
                    float sing = deltaTempFl > 0f ? 1f : -1f;
                    currentTempFl = currentTempFl*5.0f/9.0f + 32.0f;
                    deltaTempFl = sing * (deltaTempFl*5.0f/9.0f);   // + 32.0f);
                    a[0] = String.format("%3.0f %s", RoundSing(currentTempFl, 3), currentTempStr);
                    a[1] = String.format("%+2.0f %s %s", RoundSing(deltaTempFl,2), currentTempStr, deltaTempStr);
                } else {
                    currentTempStr = context.getString(R.string.degrees_celsius); // " °C"
                    a[0] = String.format("%2.0f %s", RoundSing(currentTempFl, 3), currentTempStr);
                    a[1] = String.format("%+1.0f %s %s", RoundSing(deltaTempFl, 2), currentTempStr, deltaTempStr);
                    if (Constans.DEBUG) Log.d(Constans.LOG_TAG, Float.toString(RoundSing(deltaTempFl, 2)));
                }
                currTime = getCurrentTime();
                a[2] = context.getString(R.string.time_text) + " " + currTime; // "Обновлено в " + currTime

                if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground [" +widgetID + "]  " + a[0] + " | " + a[1] + " | " +a[2]);

            } catch (Throwable t) {
                t.printStackTrace();
                if (Constans.DEBUG) Log.d(Constans.LOG_TAG, "YarTemp doInBackground [" +widgetID + "] network ERROR");
            }

            return a;
        }

        private String getCurrentTime() {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            return String.format("%02d:%02d", hour, minute); // ЧЧ:ММ - формат времени
        }

        @Override
        protected void onPostExecute(String s[]) {
            super.onPostExecute(s);

            RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            widgetView.setTextViewText(R.id.Temp,s[0]);
            widgetView.setTextViewText(R.id.deltaTemp,s[1]);
            widgetView.setTextViewText(R.id.Updated, s[2]);

            am.updateAppWidget(widgetID, widgetView);
        }
    }

    static float RoundSing(float x, int n){
        if (x == 0f) return 0f;
        int digits = n - (int)Math.log10((double)x) - 1;
        if (x < 1) digits++;
        return (float)(Math.round(x * Math.pow(10, digits)) / Math.pow(10, digits));    }
}
