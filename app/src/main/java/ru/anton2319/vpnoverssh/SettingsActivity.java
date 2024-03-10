package ru.anton2319.vpnoverssh;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import ru.anton2319.vpnoverssh.data.AppInfo;
import ru.anton2319.vpnoverssh.data.utils.AppInfoAdapter;
import ru.anton2319.vpnoverssh.utils.AppInfoExtractor;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "Settings";
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class VPNFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.vpn_preferences, rootKey);
        }
    }

    public static class SSHFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.ssh_preferences, rootKey);
        }
    }

    public static class AppsFragment extends Fragment {
        private SharedPreferences sharedPreferences;
        private RecyclerView recyclerView;
        private AppInfoAdapter adapter;
        private List<AppInfo> appInfoList;
        private ExecutorService executor = Executors.newSingleThreadExecutor();
        private SwipeRefreshLayout mSwipeRefreshLayout;

        private Future<List<AppInfo>> getApps(Context context) {
            return executor.submit(() -> {
                AppInfoExtractor appInfoExtractor = new AppInfoExtractor(context.getPackageManager());
                return appInfoExtractor.getAllInstalledApps();
            });
        }

        private Future<Set<String>> getSelectedApps(SharedPreferences sharedPreferences) {
            return executor.submit(() -> {
                // https://stackoverflow.com/a/14034804/11945017
                return new HashSet<String>(sharedPreferences.getStringSet("included_apps", new HashSet<String>()));
            });
        }

        private Future<List<AppInfo>> getAppsFuture;
        private Future<Set<String>> getSelectedAppsFuture;

        public AppsFragment() {
            super(R.layout.include_apps_list_fragment);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            recyclerView = getView().findViewById(R.id.included_apps_recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mSwipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipeContainer_included_apps_recyclerview);
            mSwipeRefreshLayout.setRefreshing(true);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    getAppsFuture = getApps(getActivity());
                    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    getSelectedAppsFuture = getSelectedApps(sharedPreferences);
                    update();
                }
            });
        }

        @Override
        public void onPause() {
            if(adapter != null) {
                sharedPreferences.edit().putStringSet("included_apps", adapter.getSelectedApps()).apply();
            }
            super.onPause();
        }

        public void update() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    while (!getAppsFuture.isDone() && getSelectedAppsFuture.isDone()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        adapter = new AppInfoAdapter(getAppsFuture.get());
                        adapter.setSelectedApps(getSelectedAppsFuture.get());
                    } catch (ExecutionException | InterruptedException e) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        throw new RuntimeException(e);
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.setAdapter(adapter);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            });
        }
    }
}