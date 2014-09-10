package com.hasmobi.rambo.fragments.child;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hasmobi.rambo.R;
import com.hasmobi.rambo.lib.DDebug;
import com.hasmobi.rambo.supers.DFragment;
import com.hasmobi.rambo.utils.Prefs;
import com.hasmobi.rambo.utils.RamManager;
import com.hasmobi.rambo.utils.Values;
import com.hasmobi.rambo.utils.custom_views.PieView;

public class FragmentPie extends DFragment {
	long totalRam = 0, freeRam = 0;

	Context context;

	// SharedPreferences prefs;

	private Handler handler;
	private Runnable r;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = getActivity().getBaseContext();

		pieStartCountLogger();
	}

	/**
	 * Some utility checks and actions on each app start
	 */
	private void pieStartCountLogger() {
		final SharedPreferences prefs = Prefs.instance(context);

		int pieStartCount = prefs.getInt("pie_start_count", 0);

		if (pieStartCount == 0) {
			pieFirstStart();
		}

		prefs.edit().putInt("pie_start_count", pieStartCount + 1).commit();
	}

	private void pieFirstStart() {

		// Initial core apps whitelist
		final String[] defaultExcluded = { "system_process",
				"com.hasmobi.rambo", "com.android.phone",
				"com.android.systemui", "android.process.acore",
				"com.android.launcher" };
		final SharedPreferences.Editor excludedList = getActivity()
				.getSharedPreferences("excluded_list", 0).edit();
		for (int i = 0; i + 1 < defaultExcluded.length; i++) {
			// Put them in SharedPreferences
			excludedList.putBoolean(defaultExcluded[i], true);
		}
		excludedList.commit();
		DDebug.log(getClass().toString(), "Default exclude list populated");
	}

	@Override
	public void onResume() {
		super.onResume();

		startUpdating();
	}

	@Override
	public void handleBroadcast(Context c, Intent i) {
		if (fragmentVisible) {
			startUpdating();
		}

		super.handleBroadcast(c, i);
	}

	/**
	 * Start updating the RAM every N seconds
	 */
	private void startUpdating() {
		try {
			// Cancel any previously running RAM updater instances
			handler.removeCallbacks(r);
		} catch (NullPointerException e) {
		}

		final int updateInterval = Values.PIE_UPDATE_INTERVAL; // milliseconds

		final RamManager ramManager = new RamManager(c);

		handler = new Handler();
		r = new Runnable() {
			public void run() {
				if (fragmentVisible) {

					totalRam = ramManager.getTotalRam();
					freeRam = ramManager.getFreeRam();

					// Redraw the pie chart
					try {
						PieView pie = (PieView) getView()
								.findViewById(R.id.pie);
						pie.setRam(totalRam, freeRam);
					} catch (Exception e) {
						DDebug.log(getClass().toString(), "Can not update pie");
					}

					if (freeRam > 0 && totalRam > 0) {
						// Do something with the updated values here if needed
					} else {
						DDebug.log(getClass().toString(),
								"Free or Total RAM not set at the moment. One of them is zero");
					}

					handler.postDelayed(r, updateInterval);
				}
			}
		};
		handler.post(r);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (handler != null && r != null)
			handler.removeCallbacks(r);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.xml.fragment_pie, null);
	}
}