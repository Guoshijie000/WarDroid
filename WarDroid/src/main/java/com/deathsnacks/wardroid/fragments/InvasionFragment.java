package com.deathsnacks.wardroid.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.deathsnacks.wardroid.R;
import com.deathsnacks.wardroid.adapters.InvasionListViewAdapter;
import com.deathsnacks.wardroid.adapters.SeparatedListAdapter;
import com.deathsnacks.wardroid.utils.Http;
import com.deathsnacks.wardroid.utils.PreferenceUtils;
import com.deathsnacks.wardroid.utils.httpclasses.Invasion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Admin on 23/01/14.
 */
public class InvasionFragment extends Fragment {
    private static final String TAG = "InvasionFragment";
    private View mRefreshView;
    private ListView mInvasionView;
    private InvasionRefresh mTask;
    private SeparatedListAdapter mAdapter;
    private Handler mHandler;
    private View mNoneView;
    private boolean mUpdate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_invasions, container, false);
        mNoneView = rootView.findViewById(R.id.invasions_none);
        mRefreshView = rootView.findViewById(R.id.invasion_refresh);
        mInvasionView = (ListView) rootView.findViewById(R.id.list_invasions);
        mInvasionView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (view.getTag() == null)
                    return;
                final Invasion invasion = ((InvasionListViewAdapter.ViewHolder) view.getTag()).invasion;
                SharedPreferences mPreferences2 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                List<String> ids2 =
                        new ArrayList<String>(Arrays.asList(PreferenceUtils
                                .fromPersistedPreferenceValue(mPreferences2.getString("invasion_completed_ids", ""))));
                if (ids2.contains(invasion.getId())) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(String.format("%s (%s)", invasion.getNode(), invasion.getRegion()))
                            .setMessage(getActivity().getString(R.string.invasion_mark_incomplete))
                            .setPositiveButton(getActivity().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                    List<String> ids = new ArrayList<String>(Arrays.asList(PreferenceUtils
                                            .fromPersistedPreferenceValue(mPreferences.getString("invasion_completed_ids", ""))));
                                    ids.remove(invasion.getId());
                                    SharedPreferences.Editor mEditor = mPreferences.edit();
                                    mEditor.putString("invasion_completed_ids", PreferenceUtils.toPersistedPreferenceValue(ids.toArray(new String[ids.size()])));
                                    mEditor.commit();
                                    mAdapter.notifyDataSetChanged();
                                    dialogInterface.cancel();
                                }
                            })
                            .setNegativeButton(getActivity().getString(android.R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            }).show();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(String.format("%s (%s)", invasion.getNode(), invasion.getRegion()))
                            .setMessage(getActivity().getString(R.string.invasion_mark_complete))
                            .setPositiveButton(getActivity().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                    List<String> ids = new ArrayList<String>(Arrays.asList(PreferenceUtils
                                            .fromPersistedPreferenceValue(mPreferences.getString("invasion_completed_ids", ""))));
                                    ids.add(invasion.getId());
                                    SharedPreferences.Editor mEditor = mPreferences.edit();
                                    mEditor.putString("invasion_completed_ids", PreferenceUtils.toPersistedPreferenceValue(ids.toArray(new String[ids.size()])));
                                    mEditor.commit();
                                    mAdapter.notifyDataSetChanged();
                                    dialogInterface.cancel();
                                }
                            })
                            .setNegativeButton(getActivity().getString(android.R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            }).show();
                }
            }
        });
        mHandler = new Handler();
        setHasOptionsMenu(true);
        mUpdate = true;
        if (savedInstanceState != null) {
            String pc = savedInstanceState.getString("invasions_pc");
            String ps4 = savedInstanceState.getString("invasions_ps4");
            String xbox = savedInstanceState.getString("invasions_xbox");
            long time = savedInstanceState.getLong("time");
            if (pc != null || ps4 != null || xbox != null) {
                Log.d(TAG, "saved instance");
                mUpdate = false;
                mAdapter = new SeparatedListAdapter(getActivity(), mNoneView);
                if (pc != null) {
                    mAdapter.addSection("PC", new InvasionListViewAdapter(getActivity(), new ArrayList<String>(Arrays.asList(pc.split("\\n"))), mNoneView, false));
                }
                if (ps4 != null) {
                    mAdapter.addSection("PS4", new InvasionListViewAdapter(getActivity(), new ArrayList<String>(Arrays.asList(ps4.split("\\n"))), mNoneView, false));
                }
                if (xbox != null) {
                    mAdapter.addSection("Xbox One", new InvasionListViewAdapter(getActivity(), new ArrayList<String>(Arrays.asList(xbox.split("\\n"))), mNoneView, false));
                }
                if (mAdapter.getAdapterCount() == 0) {
                    mNoneView.setVisibility(View.VISIBLE);
                } else {
                    mNoneView.setVisibility(View.GONE);
                }
                mInvasionView.setAdapter(mAdapter);
                mInvasionView.onRestoreInstanceState(savedInstanceState.getParcelable("invasion_lv"));
                if (System.currentTimeMillis() - time > 120 * 1000) {
                    refresh(false);
                }
            }
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter == null)
            return;
        outState.putParcelable("invasion_lv", mInvasionView.onSaveInstanceState());
        InvasionListViewAdapter pc = (InvasionListViewAdapter) mAdapter.getSectionAdapter("PC");
        InvasionListViewAdapter ps4 = (InvasionListViewAdapter) mAdapter.getSectionAdapter("PS4");
        InvasionListViewAdapter xbox = (InvasionListViewAdapter) mAdapter.getSectionAdapter("Xbox One");
        if (pc != null)
            outState.putString("invasions_pc", pc.getOriginalValues());
        if (ps4 != null)
            outState.putString("invasions_ps4", ps4.getOriginalValues());
        if (xbox != null)
            outState.putString("invasions_xbox", xbox.getOriginalValues());
        outState.putLong("time", System.currentTimeMillis());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refresh(Boolean show) {
        Log.d(TAG, "Starting refresh.");
        showProgress(show);
        if (mTask == null) {
            mTask = new InvasionRefresh(getActivity());
            mTask.execute();
        }
    }

    private final Runnable mRefreshTimer = new Runnable() {
        @Override
        public void run() {
            refresh(false);
            mHandler.postDelayed(this, 60 * 1000);
        }
    };

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "we called ondestroy");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "we called onpause");
        super.onPause();
    }

    @Override
    public void onResume() {
        mHandler.postDelayed(mRefreshTimer, 60 * 1000);
        super.onResume();
    }

    @Override
    public void onStart() {
        if (mUpdate)
            refresh(true);
        mUpdate = true;
        super.onStart();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final Boolean show) {
        if (!isAdded())
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mRefreshView.setVisibility(View.VISIBLE);
            mInvasionView.setVisibility(View.VISIBLE);
            try {
                mRefreshView.animate()
                        .setDuration(shortAnimTime)
                        .alpha(show ? 1 : 0)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mRefreshView.setVisibility(show ? View.VISIBLE : View.GONE);
                            }
                        });

                mInvasionView.animate()
                        .setDuration(shortAnimTime)
                        .alpha(show ? 0 : 1)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mInvasionView.setVisibility(show ? View.GONE : View.VISIBLE);
                            }
                        });
            } catch (Exception ex) {
                mRefreshView.setVisibility(show ? View.VISIBLE : View.GONE);
                mInvasionView.setVisibility(show ? View.GONE : View.VISIBLE);
                ex.printStackTrace();
            }
            mNoneView.setVisibility(View.GONE);
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mRefreshView.setVisibility(show ? View.VISIBLE : View.GONE);
            mInvasionView.setVisibility(show ? View.GONE : View.VISIBLE);
            mNoneView.setVisibility(View.GONE);
        }
    }

    public class InvasionRefresh extends AsyncTask<Void, Void, Boolean> {
        private static final String KEY = "invasion_raw";
        private Activity activity;
        private List<String> data;
        private List<String> ps4data;
        private List<String> xboxdata;
        private boolean error;

        public InvasionRefresh(Activity activity) {
            this.activity = activity;
            error = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (activity == null)
                return false;
            try {
                SharedPreferences preferences = activity.getSharedPreferences(KEY, Context.MODE_PRIVATE);

                String cache = preferences.getString(KEY + "_cache", "_ded");
                String response;
                try {
                    response = Http.get("http://deathsnacks.com/wf/data/invasion_raw.txt", preferences, KEY);
                } catch (IOException ex) {
                    //We failed to update, but we still have a cache, hopefully.
                    ex.printStackTrace();
                    //If no cache, proceed to normally handling an exception.
                    if (cache.equals("_ded"))
                        throw ex;
                    response = cache;
                    error = true;
                }
                response = response.trim();
                data = new ArrayList<String>(Arrays.asList(response.split("\\n")));

                String cache_ps4 = preferences.getString(KEY + "_ps4_cache", "_ded");
                String response_ps4;
                try {
                    response_ps4 = Http.get("http://deathsnacks.com/wf/data/ps4/invasion_raw.txt", preferences, KEY + "_ps4");
                } catch (IOException ex) {
                    //We failed to update, but we still have a cache, hopefully.
                    ex.printStackTrace();
                    //If no cache, proceed to normally handling an exception.
                    if (cache_ps4.equals("_ded"))
                        throw ex;
                    response_ps4 = cache_ps4;
                    error = true;
                }
                response_ps4 = response_ps4.trim();
                ps4data = new ArrayList<String>(Arrays.asList(response_ps4.split("\\n")));

                String cache_xbox = preferences.getString(KEY + "_xbox_cache", "_ded");
                String response_xbox;
                try {
                    response_xbox = Http.get("http://deathsnacks.com/wf/data/xbox/invasion_raw.txt", preferences, KEY + "_xbox");
                } catch (IOException ex) {
                    //We failed to update, but we still have a cache, hopefully.
                    ex.printStackTrace();
                    //If no cache, proceed to normally handling an exception.
                    if (cache_xbox.equals("_ded"))
                        throw ex;
                    response_xbox = cache_xbox;
                    error = true;
                }
                response_xbox = response_xbox.trim();
                xboxdata = new ArrayList<String>(Arrays.asList(response_xbox.split("\\n")));
                clearIds();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private void clearIds() {
            if (activity == null)
                return;
            try {
                SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                List<String> ids = new ArrayList<String>(Arrays.asList(PreferenceUtils.fromPersistedPreferenceValue(mPreferences.getString("invasion_ids", ""))));
                List<String> newids = new ArrayList<String>();
                List<String> nowids = new ArrayList<String>();
                for (int i = 1; i < data.size(); i++) {
                    nowids.add(data.get(i).split("\\|")[0]);
                }
                for (int i = 1; i < ps4data.size(); i++) {
                    nowids.add(ps4data.get(i).split("\\|")[0]);
                }
                for (int i = 1; i < xboxdata.size(); i++) {
                    nowids.add(xboxdata.get(i).split("\\|")[0]);
                }
                for (String id : ids) {
                    if (nowids.contains(id)) {
                        newids.add(id);
                    }
                }
                SharedPreferences.Editor mEditor = mPreferences.edit();
                mEditor.putString("invasion_ids", PreferenceUtils.toPersistedPreferenceValue(newids.toArray(new String[newids.size()])));
                mEditor.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mTask = null;
            if (activity == null)
                return;
            showProgress(false);
            SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            if (success) {
                try {
                    mAdapter = new SeparatedListAdapter(activity, mNoneView);
                    if (mPreferences.getString("platform", "pc|ps4|xbox").contains("pc")) {
                        mAdapter.addSection("PC", new InvasionListViewAdapter(activity, data, mNoneView));
                    }
                    if (mPreferences.getString("platform", "pc|ps4|xbox").contains("ps4")) {
                        mAdapter.addSection("PS4", new InvasionListViewAdapter(activity, ps4data, mNoneView));
                    }
                    if (mPreferences.getString("platform", "pc|ps4|xbox").contains("xbox")) {
                        mAdapter.addSection("Xbox One", new InvasionListViewAdapter(activity, xboxdata, mNoneView));
                    }
                    if (mAdapter.getAdapterCount() == 0) {
                        mNoneView.setVisibility(View.VISIBLE);
                    } else {
                        mNoneView.setVisibility(View.GONE);
                    }
                    mInvasionView.setAdapter(mAdapter);
                    if (error) {
                        Toast.makeText(activity, R.string.error_error_occurred, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(activity, R.string.error_error_occurred, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mTask = null;
            showProgress(false);
        }
    }
}

