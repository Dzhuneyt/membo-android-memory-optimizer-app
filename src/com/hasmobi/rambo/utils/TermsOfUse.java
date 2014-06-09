package com.hasmobi.rambo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.hasmobi.lib.DApp;
import com.hasmobi.rambo.R;
import com.hasmobi.rambo.supers.DFragmentActivity;

public class TermsOfUse {

	public static final String PREF_NAME_ANALYTICS = "tos_analytics_accepted";

	static String PREF_NAME = "last_tos_accept_version_code";

	DFragmentActivity c;

	Context context;

	private String TOS_DIALOG_FRAGMENT_NAME = "fragment_tos_dialog";

	public TermsOfUse(DFragmentActivity c) {
		this.c = c;
		this.context = c.getBaseContext();
	}

	/**
	 * Check if the TOS for the current version code of the app have been
	 * accepted already
	 * 
	 * @return
	 */
	public boolean accepted() {
		SharedPreferences p = Prefs.instance(c);
		int lastAcceptedVersion = p.getInt(PREF_NAME, 0);
		if (lastAcceptedVersion > 0) {
			int currentVerCode = 0;
			try {
				currentVerCode = c.getPackageManager().getPackageInfo(
						c.getPackageName(), 0).versionCode;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}

			if (currentVerCode > 0) {
				if (currentVerCode > lastAcceptedVersion) {
					return false;
				} else {
					// Last accepted TOS app version matches the current version
					return true;
				}
			} else {
				// We can't determinate the current app version. This will most
				// likely persist, so don't force the user to accept the TOS on
				// every app start
				return true;
			}
		}
		return false;
	}

	/**
	 * Shows the TOS dialog (using an AsyncTask while loading the TOS content so
	 * the UI thread is not blocked)
	 */
	public void show() {
		DialogFragment d = new TosDialogFragment();

		FragmentManager fm = c.getSupportFragmentManager();

		if (fm != null) {
			FragmentTransaction ft = fm.beginTransaction();
			Fragment prev = fm.findFragmentByTag(TOS_DIALOG_FRAGMENT_NAME);

			if (prev != null) {
				ft.remove(prev);
			}
			ft.addToBackStack(null);

			// Create and show the TOS dialog.
			d.show(ft, TOS_DIALOG_FRAGMENT_NAME);
		}
	}

	static public class TosDialogFragment extends DialogFragment implements
			OnClickListener {
		// Becomes true when the TOS dialog is accepted with a
		// checked "I accept to provide anonymous usage statistics"
		boolean analyticsAccepted = false;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View v = inflater.inflate(R.layout.tos_content, null, false);

			getDialog().setCanceledOnTouchOutside(false);
			this.setCancelable(false);

			getDialog().setTitle("Terms of Use");

			new Thread(new Runnable() {
				public void run() {
					String rawTosContent = getTosContent();
					if (rawTosContent.length() > 0) {
						final Spanned tosContent = Html.fromHtml(rawTosContent);
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								final TextView tvTosHolder = (TextView) v
										.findViewById(R.id.tvTosHolder);
								tvTosHolder.setText(tosContent);
							}
						});
					}
				}
			}).start();

			Button bAccept = (Button) v.findViewById(R.id.bAcceptTos);
			if (bAccept != null)
				bAccept.setOnClickListener(this);

			return v;
		}

		/**
		 * Read the raw txt file with the TOS as a String
		 * 
		 * @return String
		 */
		private String getTosContent() {
			// read tos.txt raw file
			StringBuffer sb = new StringBuffer();
			try {
				InputStream ins = getActivity().getResources().openRawResource(
						R.raw.tos);
				BufferedReader br = new BufferedReader(new InputStreamReader(
						ins));

				String line = null;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					sb.append(line + "\n");
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			}

			return sb.toString();

		}

		public void onClick(View clicked) {
			switch (clicked.getId()) {
			case R.id.bAcceptTos: // "TOS accepted" button clicked

				if (getView() == null)
					return;

				// See if the user agrees to share his
				// anonymous usage statistic with us (Google
				// Analytics) and save it in
				// SharedPreferences
				CheckBox cbAnalytics = (CheckBox) getView().findViewById(
						R.id.cbAnalyticsAgree);
				analyticsAccepted = cbAnalytics.isChecked();

				if (cbAnalytics != null) {
					getActivity().getSharedPreferences("settings", 0).edit()
							.putBoolean(PREF_NAME_ANALYTICS, analyticsAccepted)
							.commit();

					if (analyticsAccepted) {
						// Initialize Google Analytics tracking
						// for the first time, now that the user
						// agreed to participate
						GoogleAnalytics analytics = GoogleAnalytics
								.getInstance(getActivity());

						if (Values.DEBUG_MODE)
							analytics.setDryRun(true);

						Tracker t = analytics.newTracker(R.xml.global_tracker);
						t.setScreenName(getActivity().getClass()
								.getSimpleName());

						// Send a screen view.
						t.send(new HitBuilders.AppViewBuilder().build());
					}
				}

				final int currentAppVersionCode = DApp
						.getCurrentAppVersionCode(getActivity());
				markAccepted(currentAppVersionCode);

				// Close the TOS dialog
				dismiss();
				break;
			}

		}

		/**
		 * Mark the TOS associated with the provided version code as accepted
		 * (writes a flag in SharedPreferences)
		 * 
		 * @param acceptAppVersion
		 */
		public void markAccepted(int acceptAppVersion) {
			Prefs p = new Prefs(getActivity());
			p.save(PREF_NAME, acceptAppVersion);
		}
	}
}
