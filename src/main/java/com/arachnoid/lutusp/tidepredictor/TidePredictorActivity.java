/*
***************************************************************************
*   Copyright (C) 2017 by Paul Lutus                                      *
*   http://arachnoid.com/administration                                   *
*                                                                         *
*   This program is free software; you can redistribute it and/or modify  *
*   it under the terms of the GNU General Public License as published by  *
*   the Free Software Foundation; either version 2 of the License, or     *
*   (at your option) any later version.                                   *
*                                                                         *
*   This program is distributed in the hope that it will be useful,       *
*   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
*   GNU General Public License for more details.                          *
*                                                                         *
*   You should have received a copy of the GNU General Public License     *
*   along with this program; if not, write to the                         *
*   Free Software Foundation, Inc.,                                       *
*   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
***************************************************************************/


package com.arachnoid.lutusp.tidepredictor;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;

final public class TidePredictorActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    TidePredictorApplication app;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    protected ViewPager mViewPager;


    // return to current day after five minutes
    // of no changes
    long returnToCurrentTimeDelayMillis = 5 * 60000;
    long redrawInterval = 1 * 60000; // one minute
    long most_recent_interaction = -1;
    String configPath;
    public InitFileHandler initFileHandler;
    final Handler handler;
    String search_string = null;
    MyWebView stationList = null;
    MyTideGraphicView graphView = null;
    CalendarView calendarView = null;
    CalendarGenerator calGen = null;
    ConfigurationManager configurationManager;
    //MyWebView myWebView = null;
    boolean firstRun = true;
    File publicGraphicDir;
    File publicDocumentDir;
    ProgressBar progressBar;
    TextView progressBarLabel = null;
    MySpinner nearestSpinner;

    boolean showingChart = false;
    boolean validLayout = false;
    boolean fullScreen = false;


    int gridColor = Color.rgb(128, 192, 128);//Color.green.darker();
    int lineColor = Color.rgb(0, 0, 255);//.blue;
    int zeroColor = Color.rgb(192, 192, 192);//.gray
    int currentColor = Color.rgb(128, 0, 0);//.red;
    int backgroundColor = Color.rgb(255, 255, 255);//.white;
    int textColor = Color.rgb(0, 0, 0);//.black;

    final int TAB_SITES = 0;
    final int TAB_TOOLS = 1;
    final int TAB_CHART = 2;
    final int TAB_CALENDAR = 3;

    final int STATUS_NO_FIX = 0;
    final int STATUS_OLD_FIX = 1;
    final int STATUS_NEW_FIX = 2;
    LocationListener locationListener = null;
    int MY_GPS_REQ = 1234;
    boolean acquiringGPSPosition = false;
    LocationManager locationManager = null;

    public TidePredictorActivity() {
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.e("OP PHASE", "onCreate (TidePredictorActivity)");
        //main = this;

        app = (TidePredictorApplication) getApplication();

        app.currentActivity = this;

        //Log.e("onCreate", "enter");


        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();

        ab.setDisplayShowHomeEnabled(true);
        ab.setLogo(R.mipmap.ic_launcher_foreground);
        ab.setDisplayUseLogoEnabled(true);
        ab.setTitle("  TidePredictor " + app.PROGRAM_VERSION);

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // prevent pointless loss of views
        mViewPager.setOffscreenPageLimit(4);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        initFileHandler = new InitFileHandler(configPath);

        configurationManager = new ConfigurationManager(this);

        calGen = new CalendarGenerator(this);

        search_string = "";

        // these are executed only once for
        // the lifetime of the activity

        redrawLoop(redrawInterval);

    }

    protected void afterResume() {
        // this assures that the layout is complete, everything created
        mViewPager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mViewPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                layoutComplete();
            }
        });
    }


    protected void layoutComplete() {
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBarLabel = (TextView) findViewById(R.id.progress_bar_label);
        stationList = (MyWebView) findViewById(R.id.station_list_webview);
        nearestSpinner = (MySpinner) findViewById(R.id.nearest_spinner);
        readConfig();
        if (app.harmonicArray == null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    app.tideComp.verifyIndex();
                }
            };
            Thread t = new Thread(runnable);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setupTabChangeListener();
        }
        stationList.readContent();

        changeTabs(app.configValues.currentTab);
        setup_search_entry_handler();
        validLayout = true;
        execDrawChart(app.configValues.lastDisplayedSite, -1, false);
    }

    protected void setupTabChangeListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //if(app.configValues != null) {
                // this exits full screen mode on a tab change
                //fullScreen = true;
                //toggleFullScreen(null);
                writeConfig();
                //}
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    protected void createDataDirectories() {
        publicGraphicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/TidePredictor");
        if (!publicGraphicDir.exists()) {
            boolean result = publicGraphicDir.mkdirs();
            //Log.e("Monitor", "Making of graphic dir: " + result);
        }

        publicDocumentDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/TidePredictor");
        if (!publicDocumentDir.exists()) {
            boolean result = publicDocumentDir.mkdirs();
            //Log.e("Monitor", "Making of docs dir: " + result);
        }
    }

    protected void readConfig() {
        app.deSerialize();
        changeTabs(app.configValues.currentTab);
        configurationManager.write_controls();
    }

    protected void writeConfig() {
        if (validLayout) {
            app.configValues.currentTab = mViewPager.getCurrentItem();
            configurationManager.read_controls();
            app.serialize();
        }
    }

    private void redrawLoop(final long interval) {
        mViewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (app.harmonicArray != null) {
                    execDrawChart(-1, -1, false);
                }
                redrawLoop(interval);
            }
        }, interval);
    }

    public void processEvents(View v) {
        //Log.e("processEvents", "in main: ");
        configurationManager.processEvents(v);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        WebView wv = (WebView) this.findViewById(R.id.station_list_webview);
        if (wv != null) {
            wv.saveState(outState);
        }
    }

    String copyAssetToString(String source, String encoding) {
        StringBuilder os = new StringBuilder();
        try {
            InputStream is = getAssets().open(source);
            Reader ir = new InputStreamReader(is, encoding);
            char[] buf = new char[4096];
            int len;
            while ((len = ir.read(buf)) > 0) {
                os.append(buf, 0, len);
            }
            ir.close();
        } catch (Exception e) {
            Log.e("CopyATS Error:", e.toString());
        }
        return os.toString();
    }

    protected void setup_search_entry_handler() {
        final EditText search_box = (EditText) findViewById(R.id.search_string);
        search_box.setInputType(InputType.TYPE_CLASS_TEXT);
        search_box.setOnKeyListener(new View.OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    //if the enter key was pressed, then hide the keyboard and do whatever needs doing.
                    InputMethodManager imm = (InputMethodManager) TidePredictorActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(search_box.getApplicationWindowToken(), 0);
                    webViewSearch(v);

                    return true;
                }
                return false;
            }
        });
    }

    public void webViewSearch(View v) {
        //Log.e("webViewSearch", "loading url ...");
        //stationList.loadUrl("javascript:getSelection();");
        //if (search_string.length() > 0) return;
        if (app.sitePage.length() < 1000) {
            app.tideComp.parseTreeSetup();
        } else {
            hideKeyboard();
            EditText et = (EditText) findViewById(R.id.search_string);
            String s = et.getText().toString();
            //WebView wv = (WebView) findViewById(R.id.station_list_webview);
            // expand all
            webViewOpenAll(null);
            if (search_string == null || !s.equals(search_string)) {
                stationList.findAllAsync(s);
                if (firstRun) {
                    // just show this once per activity invocation
                    Toast.makeText(this, "Touch your choice or press search for more",
                            Toast.LENGTH_SHORT).show();
                    firstRun = false;
                }
            }
            search_string = s;
        }

    }

    public void webViewSearchLeft(View v) {
        hideKeyboard();
        WebView wv = (WebView) findViewById(R.id.station_list_webview);
        wv.findNext(false);
    }

    public void execDrawChart(int is, final int index, boolean changeTabs) {
        //Log.e("Monitoring", "execDrawChart: " + is + "," + index + "," + changeTabs);
        if (is != -1) {
            // if a new location, then reset the calendar
            graphView.setChartCalendar();
            most_recent_interaction = -1;
            app.tideComp.processSiteData(is);
            updateFavoriteList(is);
        }
        graphView.invalidate();
        calendarView.loadCalendarDisplay();
        if (changeTabs) {
            changeTabs(index);
        }
    }

    protected void updateFavoriteList(int is) {
        if (app.favorites_list.contains(is)) {
            app.favorites_list.remove((Object) is);
        }
        // put new items at top of list
        app.favorites_list.add(0, is);
        // throw away old items from bottom
        while (app.favorites_list.size() > 50) {
            app.favorites_list.remove(app.favorites_list.size() - 1);
        }
        //Log.e("Monitor","updateFavoriteList: " + is + "," + app.favorites_list);
        configurationManager.update_spinner(R.id.favorites_spinner, app.favorites_list);
    }

    protected void changeTabs(final int tab) {
        if (tab >= 0) {
            //mViewPager.postDelayed(new Runnable() {
            //    @Override
            //    public void run() {
            mViewPager.setCurrentItem(tab, true);
            writeConfig();
            //    }
            //}, 100);
        }
    }

    protected void webViewOpenAll(View v) {
        hideKeyboard();
        WebView wv = (WebView) findViewById(R.id.station_list_webview);
        wv.loadUrl("javascript:expandCollapseTree(true);");
        wv.findNext(true);
    }

    public void webViewCloseAll(View v) {
        hideKeyboard();
        WebView wv = (WebView) findViewById(R.id.station_list_webview);
        wv.loadUrl("javascript:expandCollapseTree(false);");
        wv.findNext(true);
    }

    public void calViewYearRight(View v) {
        graphView.navHandler(3, TAB_CALENDAR);
    }

    public void calViewMonthRight(View v) {
        graphView.navHandler(2, TAB_CALENDAR);
    }

    public void calViewHome(View v) {
        graphView.navHandler(0, TAB_CALENDAR);
    }

    public void calViewMonthLeft(View v) {
        graphView.navHandler(-2, TAB_CALENDAR);
    }

    public void calViewYearLeft(View v) {
        graphView.navHandler(-3, TAB_CALENDAR);
    }


    public void chartViewYearRight(View v) {
        graphView.navHandler(3, TAB_CHART);
    }

    public void chartViewMonthRight(View v) {
        graphView.navHandler(2, TAB_CHART);
    }

    public void chartViewDayRight(View v) {
        graphView.navHandler(1, TAB_CHART);
    }

    public void chartViewHome(View v) {
        graphView.navHandler(0, TAB_CHART);
    }

    public void chartViewDayLeft(View v) {
        graphView.navHandler(-1, TAB_CHART);
    }

    public void chartViewMonthLeft(View v) {
        graphView.navHandler(-2, TAB_CHART);
    }

    public void chartViewYearLeft(View v) {
        graphView.navHandler(-3, TAB_CHART);
    }

    protected void hideKeyboard() {

        View v = findViewById(android.R.id.content);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public Context getContext() {
        return this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.help_item) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://arachnoid.com/android/TidePredictor"));
            startActivity(browserIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    long getCurrentTime() {
        return new GregorianCalendar().getTimeInMillis();
    }

    /**
     * Returns a new instance of fragment for the given section
     * number.
     */
    public PlaceHolderFragment newFragment(int sectionNumber) {
        //Log.e("newFragment: ", "" + sectionNumber);
        PlaceHolderFragment fragment = new PlaceHolderFragment();
        Bundle args = new Bundle();
        args.putInt("index", sectionNumber);
        // always use default setArguments()
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment.
            return newFragment(position);
        }

        @Override
        public int getCount() {
            // Total pages in tab list
            return 4;
        }

        // These are the tab labels
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Sites";
                case 1:
                    return "tools";
                case 2:
                    return "Chart";
                case 3:
                    return "Calendar";
            }
            return null;
        }

    }

    protected void draw_box(float xa, float ya, float xb, float yb, Canvas g, Paint gp) {
        float[] lines = new float[]{xa, ya, xa, yb, xa, yb, xb, yb, xb, yb, xb, ya, xb, ya, xa, ya};
        g.drawLines(lines, gp);
    }

    public void findNearestSites(final View v) {
        if (app.configValues.useNearestGps) {
            showProgressBar(true, "Acquiring\nGPS Fix ...");
            findGPSNearestSites();
        } else {
            String record = app.indexArray.get(app.configValues.lastDisplayedSite);
            if (record == null) {
                Toast.makeText(this, "Please display a chart first, or enable GPS positioning",
                        Toast.LENGTH_LONG).show();
            } else {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        findChartNearestSitesThread();
                    }
                };
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                closeFindNearest();
            }
        }
    }

    public void findChartNearestSitesThread() {
        // Use the present chart as the center
        String record = app.indexArray.get(app.configValues.lastDisplayedSite);
        GeoPosition here = new GeoPosition(app.configValues.lastDisplayedSite, record);
        createNearestList(here);
    }

    protected void createNearestList(GeoPosition here) {
        //Log.e("Monitor", "createNearestList: " + here);
        ArrayList<GeoPosition> geo = new ArrayList();
        for (int index : app.indexArray.keySet()) {
            GeoPosition pos = new GeoPosition(index, app.indexArray.get(index));
            pos.comp_radius(here);
            geo.add(pos);
        }
        int count = 0;
        Collections.sort(geo);
        app.nearest_sites = new ArrayList();
        app.nearest_sites_geo = new ArrayList();
        for (GeoPosition p : geo) {
            app.nearest_sites.add(p.index);
            app.nearest_sites_geo.add(p);
            count += 1;
            if (count >= 200) {
                break;
            }
        }
    }

    protected void closeFindNearest() {
        //Log.e("Monitor", "closeFindNearest");
        configurationManager.update_spinner(R.id.nearest_spinner, app.nearest_sites);
        showProgressBar(false, "");
        // always show the default selection
        if (nearestSpinner != null && app.reverseArray != null) {
            String sel = (String) nearestSpinner.getSelectedItem();
            if (sel != null) {
                String[] fields = sel.split(" \\|");
                int targetIndex = app.reverseArray.get(fields[0]);
                execDrawChart(targetIndex, TAB_CHART, true);
            }
        }
    }

    protected void showProgressBar(boolean visible, String title) {
        int vis = (visible) ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(vis);
        progressBarLabel.setVisibility(vis);
        progressBarLabel.setText(title);
    }

    public void showTimeLine(View v) {
        app.configValues.timeLine = !app.configValues.timeLine;
        app.serialize();
    }

    public void showGridLines(View v) {
        app.configValues.chartGrid = !app.configValues.chartGrid;
        app.serialize();
    }

    public void showThickCurve(View v) {
        app.configValues.thickLine = !app.configValues.thickLine;
        app.serialize();
    }

    public void showBoldText(View v) {
        app.configValues.boldText = !app.configValues.boldText;
        app.serialize();
    }

    public void showDateText(View v) {
        app.configValues.dateText = !app.configValues.dateText;
        app.serialize();
    }

    public void showSiteData(View v) {
        app.configValues.siteLabel = !app.configValues.siteLabel;
        app.serialize();
    }

    public void showSunData(View v) {
        app.configValues.sunText = !app.configValues.sunText;
        app.serialize();
    }

    public void showTideData(View v) {
        app.configValues.tideList = !app.configValues.tideList;
        app.serialize();
    }

    public void resetOptionDefaults(View v) {
        FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                // must preserve some settings
                String favorites = app.configValues.favorite_sites;
                int lds = app.configValues.lastDisplayedSite;
                int tab = app.configValues.currentTab;
                app.configValues = new ConfigValues();
                app.configValues.favorite_sites = favorites;
                app.configValues.lastDisplayedSite = lds;
                app.configValues.currentTab = tab;
                configurationManager.write_controls();
            }

            public void no_function() {
            }
        };
        actionDialog("Reset Program Defaults", "This resets all program settings to their defaults. Okay to proceed?", f);
    }

    public void enableDataWrites(View v) {
        app.configValues.dataFileCreation = !app.configValues.dataFileCreation;
        if (app.configValues.dataFileCreation) {
            FunctionInterface f = new FunctionInterface() {
                @Override
                public void yes_function() {
                }

                public void no_function() {
                    app.configValues.dataFileCreation = false;
                    configurationManager.write_controls();
                }
            };
            actionDialog("Data File Creation", "Instead of showing a calendar setting dialog, if enabled this feature allows a long press on the chart or calendar displays to create a file of that display that can be shared with others.", f);
        }
    }

    public void enableNearestGps(View v) {
        app.configValues.useNearestGps = !app.configValues.useNearestGps;
        if (app.configValues.useNearestGps) {
            FunctionInterface f = new FunctionInterface() {
                @Override
                public void yes_function() {
                }

                public void no_function() {
                    app.configValues.useNearestGps = false;
                    configurationManager.write_controls();
                }
            };
            actionDialog("Generate Nearest Locations List using GPS Position", "Rather than relying on the presently displayed chart's location, if enabled this feature searches for nearby sites based on a GPS position.", f);
        }
    }

    public void clearFavoritesList(View v) {
        FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                if (app.configValues.lastDisplayedSite >= 0) {
                    app.configValues.favorite_sites = "" + app.configValues.lastDisplayedSite;
                } else {
                    app.configValues.favorite_sites = "";
                }
                configurationManager.write_controls();
            }

            public void no_function() {

            }
        };
        actionDialog("Clear Favorites List", "This erases the favorites list. Okay to proceed?", f);

    }

    public void actionDialog(String title, String message, final FunctionInterface f) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.mipmap.ic_dialog_launcher_foreground)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                f.yes_function();
                            }
                        })
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                f.no_function();
                            }
                        })
                .show();
    }

    protected void setFileWritePermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected void permissionsDialog() {
        FunctionInterface f = new FunctionInterface() {
            @Override
            public void yes_function() {
                setFileWritePermissions();
            }

            public void no_function() {

            }
        };

        actionDialog("Enable Storage for TidePredictor", "To use the file saving feature, TidePredictor needs permission to save files. In the next screen, choose \"Permissions\" and enable the \"Storage\" option. Press OK to proceed.", f);
    }

    protected void messageDialog(String title, String message) {
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setTitle(title);
        ad.setIcon(R.mipmap.ic_dialog_launcher_foreground);
        ad.setMessage(message);
        ad.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        ad.show();
    }

    @Override
    protected void onDestroy() {
        //Log.e("OP PHASE", "onDestroy");
        //writeConfig();
        super.onDestroy();
    }

    @Override
    protected void onStop() {

        //Log.e("OP PHASE", "onStop");
        super.onStop();
        writeConfig();
    }

    @Override
    protected void onPause() {
        //Log.e("OP PHASE", "onPause");
        super.onPause();
        writeConfig();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.e("OP PHASE", "onResume");
        afterResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.e("OP PHASE", "onStart");
    }

    protected boolean stringToFile(String s, String path) {
        try {
            FileWriter fw = new FileWriter(path);
            fw.write(s);
            fw.close();
        } catch (Exception e) {
            Log.e("StringToFile", e.toString());
            return false;
        }
        return true;
    }

    public void toggleFullScreen(View unused) {
        //Log.e("Monitor", "toggleFullScreen");
        fullScreen = !fullScreen;
        View v = findViewById(R.id.appbar_layout);
        v.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
        if (fullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void findGPSNearestSites() {
        //Log.e("Monitor", "findGPSNearestSites entry");
        acquiringGPSPosition = true;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_GPS_REQ);
            } else {
                locationManager = (LocationManager) this
                        .getSystemService(Context.LOCATION_SERVICE);

                // Define a listener that responds to location updates
                locationListener = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        gotLocation(location);
                    }

                    public void onStatusChanged(String provider, int status,
                                                Bundle extras) {
                    }

                    public void onProviderEnabled(String provider) {
                    }

                    public void onProviderDisabled(String provider) {
                    }

                };

                locationManager.requestLocationUpdates(
                        // one second interval, zero meters change
                        LocationManager.GPS_PROVIDER, 1000, 0, locationListener
                );

                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (loc != null) {
                    gotLocation(loc);
                }
            }
        } catch (Exception e) {
            Log.e("Error", "Location Setup: " + e.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == MY_GPS_REQ) {
            findGPSNearestSites();
        }
    }

    protected void gotLocation(Location loc) {
        //Log.e("Monitor", "gotLocation entry");
        if (acquiringGPSPosition) {
            acquiringGPSPosition = false;
            //Log.e("Monitor", "gotLocation have position");
            if (locationManager != null && locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            GeoPosition geo = new GeoPosition(loc.getLatitude(), loc.getLongitude());
            createNearestList(geo);
            closeFindNearest();
        }
    }
}
