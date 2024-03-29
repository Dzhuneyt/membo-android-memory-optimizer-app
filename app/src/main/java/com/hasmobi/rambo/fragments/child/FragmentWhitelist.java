package com.hasmobi.rambo.fragments.child;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.hasmobi.rambo.R;
import com.hasmobi.rambo.adapters.InstalledAppAdapter;
import com.hasmobi.rambo.adapters.SingleProcess;
import com.hasmobi.rambo.lib.DDebug;
import com.hasmobi.rambo.supers.DFragment;
import com.hasmobi.rambo.utils.SingleInstalledApps;

import java.util.ArrayList;
import java.util.List;

/**
 * @TODO
 */
public class FragmentWhitelist extends DFragment {
	List<SingleProcess> listOfProcesses;

	BroadcastReceiver br = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		init();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_running_processes, null);
	}

	private void registerIntents() {
		if (br == null) {
			br = new BroadcastReceiver() {
				@Override
				public void onReceive(Context c, Intent i) {
					// app installed/uninstalled
					setupListView();
				}
			};

			c.registerReceiver(br,
					new IntentFilter(Intent.ACTION_PACKAGE_ADDED));
			c.registerReceiver(br, new IntentFilter(
					Intent.ACTION_PACKAGE_REMOVED));
		}
	}

	private void unregisterIntents() {
		try {
			c.unregisterReceiver(br);
		} catch (RuntimeException e) {
			DDebug.log(getClass().toString(), "unregisterIntents()", e);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		init();
	}

	@Override
	public void onPause() {
		super.onPause();

		unregisterIntents();
	}

	protected void init() {
		registerIntents();

		setupListView();
	}

	/**
	 * An Async class that updates the running processes list, called via
	 * setupListView()
	 */
	class listUpdater extends AsyncTask<Void, Void, Void> {

		private List<SingleInstalledApps> listOfApps = new ArrayList<SingleInstalledApps>();

		@Override
		protected Void doInBackground(Void... v) {
			if (isCancelled()) {
				return null;
			}

			List<ApplicationInfo> installedApps = null;
			PackageManager pm = c.getPackageManager();

			try {
				installedApps = pm.getInstalledApplications(0);
			} catch (Exception e) {
				DDebug.log(getClass().toString(), "listUpdater async task", e);
			}

			if (installedApps != null && installedApps.size() > 0) {
				for (ApplicationInfo appInfo : installedApps) {
					// boolean systemApp = (appInfo.flags &
					// ApplicationInfo.FLAG_SYSTEM) != 0;
					// if (!systemApp)
					listOfApps.add(new SingleInstalledApps(appInfo));

					// Debugger.log(appInfo.packageName);
					// Debugger.log(appInfo.loadLabel(pm) + "");
					// Debugger.log("--------------");
				}
			}

			return null;

		}

		@Override
		protected void onPostExecute(Void nothing) {
			if (!fragmentVisible)
				return;

			// Remember scrolled position
			final ListView lv = (ListView) getView().findViewById(
					R.id.lvRunningProcesses);
			final TextView tvPlaceholder = (TextView) getView().findViewById(
					R.id.tvRunningProcessesPlaceholder);

			if (lv == null) {
				// Something went wrong. Try to refresh the list immediately
				setupListView();
				return;
			}

			final int index = lv.getFirstVisiblePosition();
			final View v = lv.getChildAt(0);
			final int top = (v == null) ? 0 : v.getTop();

			if (listOfApps.size() > 0) {
				// Populate the actual ListView

				InstalledAppAdapter adapter = new InstalledAppAdapter(
						getActivity(), listOfApps);

				lv.setAdapter(adapter);

				// Replace placeholder "Loading..." text with the ListView
				hideView(tvPlaceholder);
				showView(lv);

				// Restore scroll position
				try {
					lv.setSelectionFromTop(index, top);
				} catch (Exception e) {

				}
			} else {
				DDebug.log(getClass().toString(), "No running processes found");
				if (tvPlaceholder != null)
					tvPlaceholder.setText("No installed apps");
			}

		}
	}

	/**
	 * Start an AsyncTask to update the list
	 */
	private void setupListView() {
		if (fragmentVisible) {
			DDebug.log(getClass().toString(), "Updating whitelist of apps");
			new listUpdater().execute();
		}
	}

}
