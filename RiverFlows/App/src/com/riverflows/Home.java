package com.riverflows;

import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

import com.riverflows.db.CachingHttpClientWrapper;
import com.riverflows.db.DatasetsDaoImpl;
import com.riverflows.db.DbMaintenance;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.RiverGaugesDb;
import com.riverflows.wsclient.AHPSXmlDataSource;
import com.riverflows.wsclient.CDECDataSource;
import com.riverflows.wsclient.CODWRDataSource;
import com.riverflows.wsclient.DataSourceController;
import com.riverflows.wsclient.UsgsCsvDataSource;

public class Home extends TabActivity {
	
	public static final String TAG = "RiverFlows";
	
	public static final String PREFS_FILE = "com.riverflows.prefs";
	public static final String PREF_TEMP_UNIT = "tempUnit";
	
	/**
	 * 20 minutes
	 */
	public static final long CACHE_TTL = 20 * 60 * 1000;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    SharedPreferences settings = getPreferences(0);
        boolean widgetAdShown = settings.getBoolean("widgetAdShown", false);
        if(!widgetAdShown) {
        	startActivity(new Intent(this,WidgetAd.class));
        	Editor prefsEditor = settings.edit();
        	prefsEditor.putBoolean("widgetAdShown", true);
        	prefsEditor.commit();
        }
	    
		DataSourceController.setHttpClientWrapper(new CachingHttpClientWrapper(
				getApplicationContext(), getCacheDir(), CACHE_TTL, "text/plain"));
		DataSourceController.getDataSource("AHPS").setHttpClientWrapper(new CachingHttpClientWrapper(
				getApplicationContext(), getCacheDir(), CACHE_TTL, "text/xml"));
	    
	    Logger.getLogger("").setLevel(Level.WARNING);
	    
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.main);

	    Resources res = getResources();
	    TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, StateSelect.class);
	    spec = tabHost.newTabSpec("browse").setIndicator("Browse by State",
	                      res.getDrawable(R.drawable.ic_tab_browse))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, Favorites.class);
	    spec = tabHost.newTabSpec("favorites").setIndicator("Favorites",
	                      res.getDrawable(R.drawable.ic_tab_favorites))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    try {
	    	DbMaintenance.upgradeIfNecessary(getApplicationContext());
		} catch(SQLiteException sqle) {
			showDialog(DIALOG_ID_MIGRATION_ERROR);
			return;
		}
	    
	    if(FavoritesDaoImpl.hasFavorites(getCurrentActivity())) {
	    	tabHost.setCurrentTab(1);
	    } else {
	    	tabHost.setCurrentTab(0);
	    }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	    
		//do in background to avoid hanging the main thread if deleteExpiredDatasets() takes some time.
		new Thread() {
			@Override
			public void run() {
			    DatasetsDaoImpl.deleteExpiredDatasets(getApplicationContext(), getCacheDir());
			    
			    RiverGaugesDb.closeHelper();
			}
		}.start();
	}
	
	public static Integer getAgencyIconResId(String siteAgency) {
		if(UsgsCsvDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.usgs;
        } else if(AHPSXmlDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.ahps;
        } else if(CODWRDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.codwr;
        } else if(CDECDataSource.AGENCY.equals(siteAgency)) {
        	return R.drawable.cdec;
        }
		return null;
	}
	
	public static Intent getWidgetUpdateIntent() {
		//refers to com.riverflows.widget.Provider.ACTION_UPDATE_WIDGET
		Intent i = new Intent("com.riverflows.widget.UPDATE");
		i.setClassName("com.riverflows.widget", "com.riverflows.widget.Provider");
		return i;
	}
	
	public static final int DIALOG_ID_MIGRATION_ERROR = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_MIGRATION_ERROR:
			AlertDialog alert = new AlertDialog.Builder(this).create();
			alert.setMessage("Sorry- There an error occurred while updating your favorites database. You will have to uninstall and reinstall RiverFlows to fix this.");
			return alert;
		}
		return null;
	}
}
