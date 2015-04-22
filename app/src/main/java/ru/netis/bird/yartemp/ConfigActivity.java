package ru.netis.bird.yartemp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by bird on 19.03.2015
 */
public class ConfigActivity extends Activity {

    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;
    SharedPreferences sp;
    Context context;
    AppWidgetManager am;
    boolean resultUnit = false;
    boolean resultPeriod = false;

    final String LOG_TAG = "myLogs";
    final static boolean DEBUG = true;


    public final static String YARTEMP_PREF = "yartemp_pref";
    public final static String YARTEMP_DEGREES = "yartemp_degrees";
    public final static String YARTEMP_PERIOD = "yartemp_period";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(LOG_TAG, "ConfigActivity onCreate [?]");

        // извлекаем ID конфигурируемого виджета
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // и проверяем его корректность
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        if (DEBUG) Log.d(LOG_TAG, "ConfigActivity onCreate [" + widgetID + "]");

        // формируем intent ответа
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        // отрицательный ответ
        setResult(RESULT_CANCELED, resultValue);
        // положительный ответ
        setResult(RESULT_OK, resultValue);

        sp = getSharedPreferences(YARTEMP_PREF, MODE_PRIVATE);

        context = getBaseContext();
        am = AppWidgetManager.getInstance(context);

        setContentView(R.layout.config_layout);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(LOG_TAG, "ConfigActivity onDestroy [" + widgetID + "]");

        YarTemp.updateWidget(context, am, sp, widgetID);
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.d(LOG_TAG, "ConfigActivity onStop [" + widgetID + "]");
        super.onStop();
    }

    public void onClickUnit(View v) {
        if (DEBUG) Log.d(LOG_TAG, "ConfigActivity onClickUnit [" + widgetID + "]");

        Dialog dialog = onCreateUnitDialog();
        dialog.show();
    }

    public void onClickRefresh(View v) {
        if (DEBUG) Log.d(LOG_TAG, "ConfigActivity onClickRefresh [" + widgetID + "]");

        Dialog dialog = onCreatePeriodDialog();
        dialog.show();
//        onPause();
    }

    public void setSharedPreferences(int unit) {
        if ((DEBUG)) Log.d(LOG_TAG, "ConfigActivity setSharedPreferences [" + widgetID + "] " +
                "Записываем в Preferences единицы измерения = "
                + (unit == Constants.CELSIUS ? context.getString(R.string.degree_celsius) : context.getString(R.string.degree_fahrenheit)));
        // Записываем значения с экрана в Preferences
        Editor editor = sp.edit();
        editor.putInt(YARTEMP_DEGREES + widgetID, unit);
        editor.apply();
    }

    public void setSharedPreferences(String refresh) {
        if ((DEBUG)) Log.d(LOG_TAG, "ConfigActivity setSharedPreferences [" + widgetID + "] " +
                "Записываем в Preferences интерва обновления = " + refresh);
        // Записываем значения с экрана в Preferences
        Editor editor = sp.edit();
        editor.putString(YARTEMP_PERIOD + widgetID, refresh);//.toString();
        editor.apply();
    }


    public Dialog onCreateUnitDialog() {

        int degreeUnit;

        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Source of the data in the Dialog
        final CharSequence[] array = {context.getString(R.string.degree_celsius), context.getString(R.string.degree_fahrenheit)};

        final SharedPreferences sp = getSharedPreferences(ConfigActivity.YARTEMP_PREF, MODE_PRIVATE);

        degreeUnit = sp.getInt(ConfigActivity.YARTEMP_DEGREES + widgetID, Constants.CELSIUS);
        // Set the dialog title
        builder.setTitle(context.getString(R.string.degree_unit_conf))
                .setCancelable(false)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(array, degreeUnit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DEBUG) Toast.makeText(
                                getApplicationContext(),
                                context.getString(R.string.degree_unit_conf)
                                        + " " + array[which],
                                Toast.LENGTH_SHORT).show();
                    }
                })

                // Set the action buttons
                .setPositiveButton(context.getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the result somewhere
                        // or return them to the component that opened the dialog
                        ListView lv = ((AlertDialog) dialog).getListView();
                        setSharedPreferences(lv.getCheckedItemPosition());
                        resultUnit = true;
                        dialog.cancel();
                    }
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }

    public Dialog onCreatePeriodDialog() {

        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Source of the data in the Dialog
        final String[] array = getResources().getStringArray(R.array.refresh_time_text);
        final String[] value = getResources().getStringArray(R.array.refresh_time_value);

        final SharedPreferences sp = getSharedPreferences(ConfigActivity.YARTEMP_PREF, MODE_PRIVATE);

        String refreshValue = sp.getString(ConfigActivity.YARTEMP_PERIOD + widgetID, array[2]);
        int refreshID = 2;
        for (int i = 0; i <= array.length; i++) {
            if (array[i].equals(refreshValue)) {
                refreshID = i;
                break;
            }
        }
        // Set the dialog title
        builder.setTitle(context.getString(R.string.time_update_conf))
                .setCancelable(false)
                        // Specify the list array, the items to be selected by default (null for none),
                        // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(array, refreshID, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DEBUG) Toast.makeText(
                                getApplicationContext(),
                                context.getString(R.string.time_update_conf)
                                        + " " + array[which],
                                Toast.LENGTH_SHORT).show();
                    }
                })

                        // Set the action buttons
                .setPositiveButton(context.getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the result somewhere
                        // or return them to the component that opened the dialog
                        ListView lv = ((AlertDialog) dialog).getListView();
                        setSharedPreferences(value[lv.getCheckedItemPosition()]);
                        resultPeriod = true;
                        // положительный ответ
                        setResult(RESULT_OK, resultValue);
                        dialog.cancel();
                    }
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }
}
