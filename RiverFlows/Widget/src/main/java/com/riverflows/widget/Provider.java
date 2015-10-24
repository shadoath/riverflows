package com.riverflows.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.riverflows.data.Reading;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.data.Variable.CommonVariable;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CDECDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.USACEDataSource;
import com.riverflows.wsclient.UsgsCsvDataSource;
import com.riverflows.wsclient.Utils;

public class Provider extends AppWidgetProvider {
	
	public static final String TAG = "RiverFlows-Widget";
	
	public static final String ACTION_UPDATE_WIDGET = "com.riverflows.widget.UPDATE";

    public static final String ACTION_VIEW_FAVORITE = "com.riverflows.widget.VIEW_FAVORITE";

    public static final String EN_SITE_ID = "site_id";
    public static final String EN_VARIABLE_ID = "variable_id";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        Log.d(Provider.TAG, "onUpdate " + appWidgetIds.length);

        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {

            // Here we setup the intent which points to the StackViewService which will
            // provide the views for this collection.
            Intent intent = new Intent(context, Service.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.main);
            rv.setRemoteAdapter(android.R.id.list, intent);

            // The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(android.R.id.list, R.id.empty_message_area);

            // Here we setup the a pending intent template. Individuals items of a collection
            // cannot setup their own pending intents, instead, the collection as a whole can
            // setup a pending intent template, and the individual items can set a fillInIntent
            // to create unique before on an item to item basis.
            Intent viewIntent = new Intent(context, Provider.class);
            viewIntent.setAction(ACTION_VIEW_FAVORITE);
            viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            viewIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(android.R.id.list, pendingIntent);

            Intent reloadIntent = new Intent(context, Provider.class);
            reloadIntent.setAction(ACTION_UPDATE_WIDGET);
            reloadIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            reloadIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent reloadPendingIntent = PendingIntent.getBroadcast(context, 0, reloadIntent, 0);

            rv.setOnClickPendingIntent(R.id.reload_button, reloadPendingIntent);
            rv.setViewVisibility(R.id.reload_button, View.VISIBLE);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if(ACTION_UPDATE_WIDGET.equals(intent.getAction())) {

			Log.d(TAG,"received update intent");
	        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

            if(widgetId == -1) {
                Log.d(TAG, "missing widget id");
                return;
            }

            Log.d(TAG, "updating widget id " + widgetId);

            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, android.R.id.list);
		} else if(ACTION_VIEW_FAVORITE.equals(intent.getAction())) {
            Log.d(TAG,"received view intent");

            Intent viewIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.fromParts("gauge",intent.getStringExtra(EN_SITE_ID),
                            intent.getStringExtra(EN_VARIABLE_ID)));
            viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(viewIntent);
        }

        super.onReceive(context, intent);
    }

	private void showErrorMessage(Context context, RemoteViews views, String message, String buttonText, PendingIntent buttonIntent) {

        //views.setViewVisibility(R.id.spinner, View.GONE);
		views.setTextViewText(R.id.empty_message, message);
    	views.setCharSequence(R.id.empty_message_button, "setText",buttonText);
    	views.setOnClickPendingIntent(R.id.empty_message_button, buttonIntent);
    	views.setViewVisibility(R.id.empty_message_button, View.VISIBLE);
    	views.setViewVisibility(R.id.empty_message_area, View.VISIBLE);
	}
}
