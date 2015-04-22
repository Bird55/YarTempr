package ru.netis.bird.yartemp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;

/**
 * Created by bird on 22.04.2015
 */
public class UpdateWidget {
    Context context;
    AppWidgetManager appWidgetManager;
    SharedPreferences sp;
    int widgetID;

    int degreeUnit;
    int periodUpdate;
    PendingIntent pIntent;

    UpdateWidget(Context context, AppWidgetManager appWidgetManager,
                             SharedPreferences sp, int widgetID) {

        this.context = context;
        this.appWidgetManager = appWidgetManager;
        this.sp = sp;
        this.widgetID = widgetID;

        Log.d(Constants.LOG_TAG, "YarTemp updateWidget [" + widgetID + "]");

        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

// текст "uploading..."
        widgetView.setTextViewText(R.id.Updated, context.getString(R.string.loading));

        // Site yartemp.com (певая зона)
        Uri uri = Uri.parse(context.getString(R.string.temp_site_URL));
        Intent siteIntent = new Intent(Intent.ACTION_VIEW, uri);
        pIntent = PendingIntent.getActivity(context, widgetID, siteIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.llImage, pIntent);

        // Конфигурационный экран (вторая зона)
        Intent configIntent = new Intent(context, ConfigActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        pIntent = PendingIntent.getActivity(context, widgetID, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.llText, pIntent);

        // Экран обновления (третья зона)
        Intent updateIntent = new Intent(context, YarTemp.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetID});
        pIntent = PendingIntent.getBroadcast(context, widgetID, updateIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.llUpdate, pIntent);

        appWidgetManager.updateAppWidget(widgetID, widgetView);

        // Читаем параметры Preferences
        String periodSt = sp.getString(ConfigActivity.YARTEMP_PERIOD + widgetID, null);
        periodUpdate = Integer.getInteger(periodSt, Constants.DEFAULT_PERIOD);
        degreeUnit = sp.getInt(ConfigActivity.YARTEMP_DEGREES + widgetID, Constants.DEFAULT_UNIT);

        if (Constants.DEBUG) Log.d(Constants.LOG_TAG, "YarTemp updateWidget [" + widgetID + "] degree unit = " + degreeUnit + " period update = " + periodUpdate);

        // Проверяем подключение к сети
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            ParseURL parseURL = new ParseURL();
            parseURL.setContext(context);
            parseURL.setAm(appWidgetManager);
            parseURL.setWidgetID(widgetID);
            parseURL.setDegreeUnit(degreeUnit);
            parseURL.execute(context.getString(R.string.temp_data_URL));
        } else {

            Toast.makeText(context, context.getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }

    }

    static class ParseURL extends AsyncTask<String, Void, String[]> {
        Context context;
        AppWidgetManager am;
        int widgetID;

        String currentTempStr;
        float currentTempFl;

        String deltaTempStr;
        float deltaTempFl;

        String currTime;
        int degreeUnit;

        void setContext(Context context) {this.context = context;}
        void setAm(AppWidgetManager am) {this.am = am;}
        void setWidgetID(int widgetID) {this.widgetID = widgetID;}
        void setDegreeUnit(int degreeUnit) {this.degreeUnit = degreeUnit;}

        @Override
        protected String[] doInBackground(String... strings) {
            String[] a = new String[]{"", "", ""};
            BufferedReader reader = null;
            StringBuilder buffer = new StringBuilder();
            if (Constants.DEBUG)
                Log.d(Constants.LOG_TAG, "YarTemp doInBackground Connecting to [" + strings[0] + "]");
            try {
                URL site = new URL(strings[0]);
                reader = new BufferedReader(new InputStreamReader(site.openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    assert reader != null;
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String[] fields = buffer.toString().split(";");

            currentTempFl = Float.valueOf(fields[0]);
            if (Constants.DEBUG)
                Log.d(Constants.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] currentTemp = " + currentTempFl);
            deltaTempFl = Float.valueOf(fields[2]);
            if (Constants.DEBUG)
                Log.d(Constants.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] deltaTemp = " + deltaTempFl);

            if (Constants.DEBUG)
                Log.d(Constants.LOG_TAG, "YarTemp doInBackground [" + widgetID + "] degreeUnit = " + degreeUnit);
            deltaTempStr = context.getString(R.string.degree_speed); // "в час"
            if (degreeUnit == Constants.FAHRENHEIT) {
                // Переводим °C в °F
                currentTempStr = context.getString(R.string.degrees_fahrenheit); // " °F"
                float sing = deltaTempFl > 0f ? 1f : -1f;
                currentTempFl = currentTempFl * 5.0f / 9.0f + 32.0f;
                deltaTempFl = sing * (deltaTempFl * 5.0f / 9.0f);   // + 32.0f);
                a[0] = String.format("%3.0f %s", RoundSing(currentTempFl, 3), currentTempStr);
                a[1] = String.format("%+2.0f %s %s", RoundSing(deltaTempFl, 2), currentTempStr, deltaTempStr);
            } else {
                currentTempStr = context.getString(R.string.degrees_celsius); // " °C"
                a[0] = String.format("%2.0f %s", RoundSing(currentTempFl, 3), currentTempStr);
                a[1] = String.format("%+1.0f %s %s", RoundSing(deltaTempFl, 2), currentTempStr, deltaTempStr);
                if (Constants.DEBUG)
                    Log.d(Constants.LOG_TAG, Float.toString(RoundSing(deltaTempFl, 2)));
            }

            currTime = getCurrentTime();
            a[2] = context.getString(R.string.time_text) + " " + currTime; // "Обновлено в " + currTime

            if (Constants.DEBUG)
                Log.d(Constants.LOG_TAG, "YarTemp doInBackground [" + widgetID + "]  " + a[0] + " | " + a[1] + " | " + a[2]);

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
