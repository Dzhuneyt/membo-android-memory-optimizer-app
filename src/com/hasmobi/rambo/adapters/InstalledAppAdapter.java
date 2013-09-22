package com.hasmobi.rambo.adapters;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hasmobi.rambo.R;
import com.hasmobi.rambo.utils.Debugger;
import com.hasmobi.rambo.utils.SingleInstalledApp;

public class InstalledAppAdapter extends ArrayAdapter<String> {

	private final Activity context;
	private final List<SingleInstalledApp> objects;

	SharedPreferences excluded_list;

	PackageManager pm = null;

	public InstalledAppAdapter(final Activity context, List objects) {
		super(context, R.layout.single_process_layout_inflater, objects);
		this.context = context;
		this.objects = objects;

		excluded_list = context.getSharedPreferences("excluded_list", 0);

		if (pm == null)
			pm = context.getPackageManager();

		Comparator<SingleInstalledApp> myComparator = new Comparator<SingleInstalledApp>() {
			public int compare(SingleInstalledApp first,
					SingleInstalledApp second) {
				String one = first.ai.loadLabel(pm).toString();
				String two = second.ai.loadLabel(pm).toString();
				return one.compareTo(two);
			}
		};
		Collections.sort(this.objects, myComparator);
		this.notifyDataSetChanged();

		// Comparator<SingleProcess> myComparator = new
		// Comparator<SingleProcess>() {
		// public int compare(SingleProcess first, SingleProcess second) {
		// Float i = first.memoryUsage;
		// Float x = second.memoryUsage;
		// return x.compareTo(i);
		// }
		// };
		// Collections.sort(this.objects, myComparator);
		// this.notifyDataSetChanged();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		SingleProcessView v = null;

		if (rowView == null) {
			// Get a new instance of the row layout view
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(
					R.layout.single_installed_app_layout_inflater, null);

			// Hold the view objects in an object,
			// so they don't need to be re-fetched
			v = new SingleProcessView();
			v.name = (TextView) rowView.findViewById(R.id.tvName);
			v.processWrapper = (LinearLayout) rowView
					.findViewById(R.id.llProcessWrapper);
			v.icon = (ImageView) rowView.findViewById(R.id.app_icon);

			v.bWhitelist = (Button) rowView.findViewById(R.id.bWhitelist);

			// Cache the view objects in the tag,
			// so they can be re-accessed later
			rowView.setTag(v);
		} else {
			// Restore view object from cache
			v = (SingleProcessView) rowView.getTag();
		}

		PackageManager pm = context.getPackageManager();

		// Get the process data
		final SingleInstalledApp currentApp = objects.get(position);

		// Transfer the data to the view
		v.name.setText(currentApp.ai.loadLabel(pm).toString()); // App name
		v.icon.setBackgroundDrawable(currentApp.ai.loadIcon(pm));

		boolean excluded = excluded_list.getBoolean(currentApp.ai.packageName,
				false);

		final String addToWhitelist = context.getResources().getString(
				R.string.whitelist_app_add);
		final String removeFromWhitelist = context.getResources().getString(
				R.string.whitelist_app_remove);

		if (excluded) {
			v.bWhitelist.setText(removeFromWhitelist);
		} else {
			v.bWhitelist.setText(addToWhitelist);
		}

		v.bWhitelist.setOnClickListener(new OnClickListener() {

			public void onClick(View view) {
				Debugger.log("whitelist request");

				// Whitelist/Blacklist the clicked package and update its button
				// label
				boolean excluded = excluded_list.getBoolean(
						currentApp.ai.packageName, false);
				SharedPreferences.Editor edit = excluded_list.edit();
				try {
					if (excluded) {
						edit.remove(currentApp.ai.packageName);
						((Button) view).setText(addToWhitelist);
					} else {
						edit.putBoolean(currentApp.ai.packageName, true);
						((Button) view).setText(removeFromWhitelist);
					}
				} catch (Exception e) {
					Debugger.log("Can not whitelist/blacklist package");
					Debugger.log(e.getMessage());
					Debugger d = new Debugger(context);
					d.longToast("This app can not be "
							+ (excluded ? "removed from blacklist"
									: "added to blacklist")
							+ ". Please contact us at feedback@hasmobi.com if this error persists.");
				}
				edit.commit();
				notifyDataSetChanged();
			}

		});

		// Is the app in the whitelist?

		return rowView;
	}

	protected static class SingleProcessView {
		protected TextView name;
		protected LinearLayout processWrapper;
		protected ImageView icon;
		protected Button bWhitelist;
	}

}