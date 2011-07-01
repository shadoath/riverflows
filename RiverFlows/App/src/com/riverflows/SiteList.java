package com.riverflows;

import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.riverflows.data.Favorite;
import com.riverflows.data.Series;
import com.riverflows.data.Site;
import com.riverflows.data.SiteData;
import com.riverflows.data.SiteId;
import com.riverflows.data.Variable;
import com.riverflows.db.FavoritesDaoImpl;
import com.riverflows.db.SitesDaoImpl;
import com.riverflows.wsclient.DataSourceController;

public abstract class SiteList extends ListActivity {
	
	private static final String TAG = SiteList.class.getSimpleName();
	
	public static final String PK_MASTER_SITE_LIST_LAST_LOAD_TIME = "masterSiteListLastLoadTime";
	
	/**
	 * reload the master site list every 2 weeks
	 */
	public static final long MASTER_SITE_LIST_RELOAD_INTERVAL = 14 * 24 * 60 * 60 * 1000;
	
	private List<SiteData> gauges = null;
	public static final int DIALOG_ID_LOADING = 1;
	public static final int DIALOG_ID_LOADING_ERROR = 2;
	public static final int DIALOG_ID_MASTER_LOADING = 3;
	public static final int DIALOG_ID_MASTER_LOADING_ERROR = 4;
	public static final int DIALOG_ID_UPGRADE_FAVORITES = 5;
	
	private LoadSitesTask loadTask = null;
	private String errorMsg = null;
	
	private TextWatcher filterFieldWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) { }

        @Override
        public void afterTextChanged(Editable s) {
            ((SiteAdapter)getListAdapter()).getFilter().filter(s.toString());

        }
    };

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ViewChart.class);
		Site selectedStation = null;
		Variable selectedVariable = null;
		
		for(SiteData currentData: gauges) {
			if(SiteAdapter.getItemId(currentData) == id){
				selectedStation = currentData.getSite();
				
				Series data = DataSourceController.getPreferredSeries(currentData);
				if(data != null) {
					selectedVariable = data.getVariable();
				}
				break;
			}
		}
		
		if(selectedStation == null) {
			Log.w(TAG,"no such station: " + id);
			return;
		}
		
        i.putExtra(ViewChart.KEY_SITE, selectedStation);
        i.putExtra(ViewChart.KEY_VARIABLE, selectedVariable);
        startActivity(i);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.mi_home:
	    	startActivityIfNeeded(new Intent(this, Home.class), -1);
	    	return true;
	    case R.id.mi_about:
			Intent i = new Intent(this, About.class);
			startActivity(i);
	        return true;
	    case R.id.mi_reload:
	    	loadSites(true);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.site_list);
		
		/* probably not necessary anymore now that we can reload on a state-by-state basis
		//check to see if the master list needs to be reloaded
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		long lastLoadTime = prefs.getLong(PK_MASTER_SITE_LIST_LAST_LOAD_TIME, 0);
		
		if(System.currentTimeMillis() - lastLoadTime > MASTER_SITE_LIST_RELOAD_INTERVAL) {
			showDialog(DIALOG_ID_MASTER_LOADING);
			LoadMasterSiteListTask masterSiteListTask = new LoadMasterSiteListTask();
			masterSiteListTask.execute();
		}*/

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		EditText siteFilterField = (EditText)findViewById(R.id.site_filter_field);
		siteFilterField.addTextChangedListener(filterFieldWatcher);

        //see onRetainNonConfigurationInstance()
    	final Object[] data = (Object[])getLastNonConfigurationInstance();
        
    	if(data == null) {
    		//make the request for site data
    		loadSites(false);
    	} else {
    		this.loadTask = (LoadSitesTask)data[0];
    		if(this.loadTask != null) {
    			this.loadTask.setActivity(this);
        		this.errorMsg = this.loadTask.errorMsg;
    		}
    		this.gauges = (List<SiteData>)data[1];
    		displaySites();
    	}
	}
	
	@Override
	public Object[] onRetainNonConfigurationInstance() {
		return new Object[]{this.loadTask, this.gauges};
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_ID_LOADING:
			ProgressDialog dialog = new ProgressDialog(this);
	        dialog.setMessage("Loading Sites...");
	        dialog.setIndeterminate(true);
	        dialog.setCancelable(true);
	        return dialog;
		case DIALOG_ID_LOADING_ERROR:
			ErrorMsgDialog errorDialog = new ErrorMsgDialog(this, errorMsg);
			return errorDialog;
		case DIALOG_ID_MASTER_LOADING:
			ProgressDialog masterDialog = new ProgressDialog(this);
			masterDialog.setMessage("Downloading Master Site List...");
			masterDialog.setIndeterminate(true);
			masterDialog.setCancelable(true);
	        return masterDialog;
		case DIALOG_ID_MASTER_LOADING_ERROR:
			ErrorMsgDialog masterErrorDialog = new ErrorMsgDialog(this, errorMsg);
			return masterErrorDialog;
		case DIALOG_ID_UPGRADE_FAVORITES:
			ProgressDialog favoritesDialog = new ProgressDialog(this);
			favoritesDialog.setMessage("Upgrading Favorites\nthis may take a few minutes");
			favoritesDialog.setIndeterminate(true);
			favoritesDialog.setCancelable(true);
	        return favoritesDialog;
		}
		return null;
	}
	
	private class ErrorMsgDialog extends AlertDialog {

		public ErrorMsgDialog(Context context, String msg) {
			super(context);
			setMessage(msg);
		}
		
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.standard_menu, menu);
	    return true;
	}
	
	/**
	 * @param hardRefresh if true, discard persisted site data as well
	 */
	public void loadSites(boolean hardRefresh) {
		showDialog(DIALOG_ID_LOADING);
		
		this.loadTask = createLoadStationsTask();
		this.loadTask.setActivity(this);
		
		if(hardRefresh) {
			this.loadTask.execute(HARD_REFRESH);
		} else {
			this.loadTask.execute();
		}
	}
	
	public void displaySites() {
		if(gauges != null) {
			setListAdapter(new SiteAdapter(getApplicationContext(), gauges));

			registerForContextMenu(getListView());
		}
		removeDialog(DIALOG_ID_LOADING);
		if(gauges == null || errorMsg != null) {
			try {
				showDialog(DIALOG_ID_LOADING_ERROR);
			} catch(BadTokenException bte) {
				if(Log.isLoggable(TAG, Log.INFO)) {
					Log.i(TAG, "can't display dialog; activity no longer active");
				}
			}
		}
	}

	/** parameter for LoadSitesTask */
	public static final Integer HARD_REFRESH = new Integer(1);
	
	public abstract class LoadSitesTask extends AsyncTask<Integer, Integer, List<SiteData>> {
		
		protected static final int STATUS_UPGRADING_FAVORITES = -1;

		public final Date loadTime = new Date();
		private String errorMsg = null;
		private SiteList activity;
		
		public void setActivity(SiteList activity) {
			this.activity = activity;
			this.activity.loadTask = this;
		}
		
		protected void setLoadErrorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
		}
		
		@Override
		protected abstract List<SiteData> doInBackground(Integer... params);
		
		@Override
		protected void onPostExecute(List<SiteData> result) {
			this.activity.gauges = result;
			this.activity.displaySites();
			this.activity.loadTask = null;
			this.activity.errorMsg = this.errorMsg;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			if(values == null || values.length == 0) {
				return;
			}
			if(values.length == 2 && values[0] == STATUS_UPGRADING_FAVORITES) {
				if(values[1] == 0) {
					this.activity.removeDialog(DIALOG_ID_LOADING);
					this.activity.showDialog(DIALOG_ID_UPGRADE_FAVORITES);
				}
				if(values[1] == 100) {
					this.activity.removeDialog(DIALOG_ID_UPGRADE_FAVORITES);
					this.activity.showDialog(DIALOG_ID_LOADING);
				}
			}
		}
	}
	
	public class LoadMasterSiteListTask extends AsyncTask<Integer, Integer, String> {
		@Override
		protected String doInBackground(Integer... params) {
			try {
				Map<SiteId,SiteData> siteData = DataSourceController.getAllSites();
				SitesDaoImpl.saveSites(getApplicationContext(), new ArrayList<SiteData>(siteData.values()));
			} catch (UnknownHostException uhe) {
				return "no network access";
			} catch(Exception e) {
				Log.e(TAG, "",e);
				return e.getMessage();
			}
			return null;
		}
		@Override
		protected void onPostExecute(String errorMsg) {
			super.onPostExecute(errorMsg);
			removeDialog(DIALOG_ID_MASTER_LOADING);
			if(errorMsg != null) {
				showDialog(DIALOG_ID_MASTER_LOADING_ERROR);
			}
		}
	}
	
	protected Date getLastLoadTime() {
		if(this.loadTask == null) {
			return null;
		}
		
		return this.loadTask.loadTime;
	}
	

	
	private class ViewVariableListener implements MenuItem.OnMenuItemClickListener {

		private Site selectedStation = null;
		private Variable selectedVariable = null;
		
		public ViewVariableListener(Site selectedStation, Variable selectedVariable) {
			this.selectedStation = selectedStation;
			this.selectedVariable = selectedVariable;
		}
		
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			
			Intent i = new Intent(getBaseContext(), ViewChart.class);
			
	        i.putExtra(ViewChart.KEY_SITE, selectedStation);
	        i.putExtra(ViewChart.KEY_VARIABLE, selectedVariable);
	        startActivity(i);
	        return true;
		}
	}
	
	private class AddToFavoritesListener implements MenuItem.OnMenuItemClickListener{

		private Site selectedStation = null;
		private Variable selectedVariable = null;
		
		public AddToFavoritesListener(Site selectedStation, Variable selectedVariable) {
			this.selectedStation = selectedStation;
			this.selectedVariable = selectedVariable;
		}

		
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			
			if(item.isChecked()) {
				FavoritesDaoImpl.deleteFavorite(getApplicationContext(), selectedStation.getSiteId(), selectedVariable);
				item.setChecked(false);
			} else {
				FavoritesDaoImpl.createFavorite(getApplicationContext(), new Favorite(selectedStation, selectedVariable.getId()));
				item.setChecked(true);
			}

			String confirmation =  MessageFormat.format(getString(R.string.add_favorite_confirmation), selectedVariable.getName(), selectedStation.getName());
			
			Toast.makeText(getApplicationContext(), confirmation, Toast.LENGTH_SHORT).show();
			
			return true;
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		SiteAdapter adapter = (SiteAdapter)((ListView)v).getAdapter();
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		
		SiteData siteData = adapter.getItem(info.position);
		
		Variable[] supportedVars = siteData.getSite().getSupportedVariables();
		
		SubMenu submenu = menu.addSubMenu(ContextMenu.NONE, supportedVars.length, supportedVars.length, "Add To Favorites");
		
		for(int a = 0; a < supportedVars.length; a++) {
			MenuItem viewVariableItem = menu.add(ContextMenu.NONE,a,a,supportedVars[a].getName() + ", " + supportedVars[a].getCommonVariable().getUnit());
			viewVariableItem.setOnMenuItemClickListener(new ViewVariableListener(siteData.getSite(), supportedVars[a]));
			
			MenuItem addFavoriteItem = submenu.add(ContextMenu.NONE,a,a,supportedVars[a].getName() + ", " + supportedVars[a].getCommonVariable().getUnit());
			addFavoriteItem.setCheckable(true);
			addFavoriteItem.setChecked(FavoritesDaoImpl.isFavorite(getApplicationContext(), siteData.getSite().getSiteId(), supportedVars[a]));
			addFavoriteItem.setOnMenuItemClickListener(new AddToFavoritesListener(siteData.getSite(), supportedVars[a]));
		}
	}
	
	protected abstract LoadSitesTask createLoadStationsTask();
}
