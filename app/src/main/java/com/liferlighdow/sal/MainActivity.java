package com.liferlighdow.sal;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "SetAsLauncher";
    private static final String PREFS_NAME = "launcher_prefs";
    private static final String KEY_TARGET_PACKAGE = "target_package";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        boolean isHomeIntent = intent != null && intent.hasCategory(Intent.CATEGORY_HOME);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String targetPackage = prefs.getString(KEY_TARGET_PACKAGE, null);

        if (targetPackage != null) {
            // If started as Home, or started as a normal main activity but with a target set
            if (isHomeIntent || (intent != null && Intent.ACTION_MAIN.equals(intent.getAction()) && !intent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
                if (launchTarget(targetPackage)) {
                    return;
                }
            }
        }
        setupSelectionUI(targetPackage);
    }

    private boolean launchTarget(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch error", e);
        }
        return false;
    }

    // Lightweight data class to avoid keeping heavy ResolveInfo objects in memory
    private static class AppEntry {
        final String label;
        final String packageName;

        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private void setupSelectionUI(String currentPackage) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText("Set As Launcher");
        title.setTextSize(20);
        root.addView(title);

        if (currentPackage != null) {
            TextView current = new TextView(this);
            current.setText("Target: " + currentPackage);
            root.addView(current);

            Button btnClear = new Button(this);
            btnClear.setText("Clear Selection");
            btnClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(KEY_TARGET_PACKAGE).commit();
                    restartActivity();
                }
            });
            root.addView(btnClear);
        }

        // Home Settings only available from API 11
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            Button btnSettings = new Button(this);
            btnSettings.setText("Home Settings");
            btnSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent("android.settings.HOME_SETTINGS"));
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                        } catch (Exception ignored) {}
                    }
                }
            });
            root.addView(btnSettings);
        }

        ListView listView = new ListView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1);
        listView.setLayoutParams(lp);
        root.addView(listView);
        setContentView(root);

        final PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> pkgAppsList = pm.queryIntentActivities(mainIntent, 0);
        final java.util.ArrayList<AppEntry> displayList = new java.util.ArrayList<AppEntry>();

        if (pkgAppsList != null) {
            String myPkg = getPackageName();
            for (int i = 0; i < pkgAppsList.size(); i++) {
                ResolveInfo info = pkgAppsList.get(i);
                if (info.activityInfo.packageName.equals(myPkg)) continue;
                
                displayList.add(new AppEntry(
                    String.valueOf(info.loadLabel(pm)),
                    info.activityInfo.packageName
                ));
            }
            // Explicitly clear the heavy list
            pkgAppsList.clear();
            pkgAppsList = null;

            Collections.sort(displayList, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });

            listView.setAdapter(new ArrayAdapter<AppEntry>(this, android.R.layout.simple_list_item_2, android.R.id.text1, displayList) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    AppEntry entry = getItem(position);
                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                    if (entry != null) {
                        text1.setText(entry.label);
                        text2.setText(entry.packageName);
                    }
                    return view;
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AppEntry entry = displayList.get(position);
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_TARGET_PACKAGE, entry.packageName).commit();
                    Toast.makeText(MainActivity.this, "Selected: " + entry.label, Toast.LENGTH_SHORT).show();
                    restartActivity();
                }
            });
        }
    }

    private void restartActivity() {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            Api11Helper.recreate(this);
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    // Use a static inner class to prevent VerifyError on API < 11
    private static class Api11Helper {
        static void recreate(Activity activity) {
            activity.recreate();
        }
    }
}
