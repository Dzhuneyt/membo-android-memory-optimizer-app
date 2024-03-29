package com.hasmobi.rambo.supers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.hasmobi.rambo.R;
import com.hasmobi.rambo.lib.DDebug;
import com.hasmobi.rambo.lib.DResources;
import com.hasmobi.rambo.utils.Fonts;
import com.hasmobi.rambo.utils.RamManager;
import com.hasmobi.rambo.utils.TermsOfUse;
import com.hasmobi.rambo.utils.TypefaceSpan;
import com.hasmobi.rambo.utils.Values;

public class DFragment extends Fragment {

    public boolean fragmentVisible = false;

    public Context c;

    // private Tracker tracker;

    // Placeholder property ID.
    // private static final String GA_PROPERTY_ID = "UA-1704294-170";

    // Dispatch period in seconds.
    // private static final int GA_DISPATCH_PERIOD = 5;

    private BroadcastReceiver broadcast = null;

    public Tracker localFragmentTracker = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        c = getActivity().getBaseContext();

        super.onCreate(savedInstanceState);

        // All fragments should receive the global RamManager.ACTION_RAM_MANAGER
        // broadcast since they will most certainly want to do something when
        // the memory is optimized (e.g. update UI, refresh apps list, etc)
        broadcast = new BroadcastReceiver() {

            @Override
            public void onReceive(Context c, Intent i) {
                handleBroadcast(c, i);
            }

        };

        c.registerReceiver(broadcast, new IntentFilter(
                RamManager.ACTION_RAM_MANAGER));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        localFragmentTracker = ((DFragmentActivity) getActivity()).t;
        boolean analyticsEnabled = false;

        if (localFragmentTracker != null && !Values.DEBUG_MODE) {
            try {
                analyticsEnabled = c.getSharedPreferences("settings", 0)
                        .getBoolean(TermsOfUse.PREF_NAME_ANALYTICS, false);
            } catch (ClassCastException e) {
            }

            if (analyticsEnabled) {
                // Send to Analytics that the current fragment was viewed
                DDebug.log(getClass().toString(),
                        "Notifying Analytics that the fragment "
                                + getClass().toString() + " was viewed");

                localFragmentTracker.setScreenName(getClass().getSimpleName());
                localFragmentTracker.send(new HitBuilders.AppViewBuilder()
                        .build());
            }
        }
    }

    @Override
    public void onPause() {
        fragmentVisible = false;

        DDebug.log(getClass().toString(), "onPause()");
        if (broadcast != null)
            c.unregisterReceiver(broadcast);

        super.onPause();
    }

    @Override
    public void onResume() {
        fragmentVisible = true;

        if (broadcast == null) {
            broadcast = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    handleBroadcast(c, i);
                }
            };
        }

        c.registerReceiver(broadcast, new IntentFilter(
                RamManager.ACTION_RAM_MANAGER));

        super.onResume();

        DDebug.log(getClass().toString(), "onResume()");

        // Log the fragment view event to Google Analytics
        // this.tracker.set(Fields.SCREEN_NAME, getClass().getSimpleName());
        // this.tracker.send(MapBuilder.createAppView().build());
    }

    public void handleBroadcast(Context c, Intent i) {
        DDebug.log(getClass().toString(), "handleBroadcast(): " + i.getAction());
    }

    public void hideView(View v) {
        if (v != null)
            v.setVisibility(View.GONE);
    }

    protected void showView(View v) {
        if (v != null)
            v.setVisibility(View.VISIBLE);
    }

    public void log(String s) {
        DDebug.log(getClass().toString(), s);
    }

    /**
     * Make the current fragment the one passed and optionally add it to backstack
     *
     * @param fragment
     * @param addToBackStack
     */
    public void goToFragment(DFragment fragment, boolean addToBackStack) {
        if (fragment == null) {
            DDebug.log(getClass().toString(), "Can not go to fragment because it is null");
            return;
        }

        DDebug.log(getClass().toString(), "Requesting to go to fragment: " + fragment.getClass().toString());

        FragmentManager fm = getActivity().getSupportFragmentManager();

        FrameLayout fragmentHolder = (FrameLayout) getActivity().findViewById(R.id.fMain);
        if (fragmentHolder != null)
            fragmentHolder.removeAllViews();

        if (fm != null) {
            FragmentTransaction ft = fm.beginTransaction();
            if (addToBackStack) {
                DDebug.log(getClass().toString(), "Adding to back stack");
                ft.addToBackStack(null);
            }
            Fragment existing = fm.findFragmentByTag("main");
            if(existing!=null){
                ft.remove(existing);
            }

            ft.add(R.id.fMain, fragment, "main");
            ft.commit();
        } else {
            DDebug.log(getClass().toString(), "Can not go to fragment " + fragment.getClass().toString() + ". Parent view not found");
        }
    }

    public void setActionBarTitle(int stringResId) {
        if (getActivity().getActionBar() == null) {
            DDebug.log(getClass().toString(), "Actionbar not present. Unable to set its title via setActionBarTitle()");
            return;
        }

        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        SpannableString s = new SpannableString(DResources.getString(c,
                stringResId));
        s.setSpan(new TypefaceSpan(getActivity(), Fonts.ACTIONBAR_TITLE), 0,
                s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActivity().getActionBar().setTitle(s);
    }

}
