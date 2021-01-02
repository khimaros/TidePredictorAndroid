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

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by lutusp on 9/20/17.
 */

final public class ConfigurationManager {
    TidePredictorActivity activity;
    TidePredictorApplication app;
    boolean isNearestList = false;
    //boolean ignoreEvents = true;

    public ConfigurationManager(TidePredictorActivity m) {
        activity = m;
        app = (TidePredictorApplication) activity.getApplication();
    }

    protected void write_controls() {
        Spinner sp = (Spinner) activity.findViewById(R.id.time_zone_spinner);
        sp.setSelection((int) app.configValues.timeZone + 14);
        RadioGroup rg = (RadioGroup) activity.findViewById(R.id.radioClock);
        rg.check((app.configValues.ampmFlag) ? R.id.radioAMPM : R.id.radio24HR);
        rg = (RadioGroup) activity.findViewById(R.id.radioHeight);
        rg.check((app.configValues.heightUnits == 1) ? R.id.radioFeet : R.id.radioMeters);
        rg = (RadioGroup) activity.findViewById(R.id.radioVelocity);
        rg.check((app.configValues.velocityUnits == 2) ? R.id.radioKnots : (app.configValues.velocityUnits == 3) ? R.id.radioMPH : R.id.radioMS);
        CheckBox cb = (CheckBox) activity.findViewById(R.id.show_timeline_checkbox);
        cb.setChecked(app.configValues.timeLine);
        cb = (CheckBox) activity.findViewById(R.id.show_gridlines_checkbox);
        cb.setChecked(app.configValues.chartGrid);
        cb = (CheckBox) activity.findViewById(R.id.show_bold_checkbox);
        cb.setChecked(app.configValues.boldText);
        cb = (CheckBox) activity.findViewById(R.id.show_thick_curve_checkbox);
        cb.setChecked(app.configValues.thickLine);
        cb = (CheckBox) activity.findViewById(R.id.show_sitedata_checkbox);
        cb.setChecked(app.configValues.siteLabel);
        cb = (CheckBox) activity.findViewById(R.id.show_date_checkbox);
        cb.setChecked(app.configValues.dateText);
        cb = (CheckBox) activity.findViewById(R.id.show_sundata_checkbox);
        cb.setChecked(app.configValues.sunText);
        cb = (CheckBox) activity.findViewById(R.id.show_tidedata_checkbox);
        cb.setChecked(app.configValues.tideList);
        cb = (CheckBox) activity.findViewById(R.id.enable_data_writes_checkbox);
        cb.setChecked(app.configValues.dataFileCreation);
        cb = (CheckBox) activity.findViewById(R.id.enable_gps_checkbox);
        cb.setChecked(app.configValues.useNearestGps);
        //Log.e("writeControls", "writes: " + index);
        app.favorites_list = new ArrayList();
        String s = app.configValues.favorite_sites.toString();
        if (s.length() > 0) {
            s.replaceAll("[\\[\\] ]", "");
            String[] vals = s.split(",");
            for (String key : vals) {
                try {
                    app.favorites_list.add(Integer.parseInt(key));
                } catch (Exception e) {
                }
            }
        }
        update_spinner(R.id.favorites_spinner, app.favorites_list);
        update_spinner(R.id.nearest_spinner, app.nearest_sites);
    }

    protected void read_controls() {
        boolean changed = false;
        Spinner sp = (Spinner) activity.findViewById(R.id.time_zone_spinner);
        int position = sp.getSelectedItemPosition() - 14;
        changed |= (position != app.configValues.timeZone);
        app.configValues.timeZone = position;
        RadioGroup rg = (RadioGroup) activity.findViewById(R.id.radioClock);
        int selected = rg.getCheckedRadioButtonId();
        boolean ampm = selected == R.id.radioAMPM;
        changed |= (ampm != app.configValues.ampmFlag);
        app.configValues.ampmFlag = ampm;
        rg = (RadioGroup) activity.findViewById(R.id.radioHeight);
        selected = rg.getCheckedRadioButtonId();
        int heightUnits = (selected == R.id.radioFeet) ? 1 : 0;
        changed |= (app.configValues.heightUnits != heightUnits);
        app.configValues.heightUnits = heightUnits;
        rg = (RadioGroup) activity.findViewById(R.id.radioVelocity);
        selected = rg.getCheckedRadioButtonId();
        int velocityUnits = (selected == R.id.radioKnots) ? 2 : (selected == R.id.radioMPH) ? 3 : 4;
        changed |= (app.configValues.velocityUnits != velocityUnits);
        app.configValues.velocityUnits = velocityUnits;
        CheckBox cb = (CheckBox) activity.findViewById(R.id.show_timeline_checkbox);
        changed |= (cb.isChecked() != app.configValues.timeLine);
        app.configValues.timeLine = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_gridlines_checkbox);
        changed |= (cb.isChecked() != app.configValues.chartGrid);
        app.configValues.chartGrid = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_bold_checkbox);
        changed |= (cb.isChecked() != app.configValues.boldText);
        app.configValues.boldText = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_thick_curve_checkbox);
        changed |= (cb.isChecked() != app.configValues.thickLine);
        app.configValues.thickLine = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_sitedata_checkbox);
        changed |= (cb.isChecked() != app.configValues.siteLabel);
        app.configValues.siteLabel = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_date_checkbox);
        changed |= (cb.isChecked() != app.configValues.dateText);
        app.configValues.dateText = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_sundata_checkbox);
        changed |= (cb.isChecked() != app.configValues.sunText);
        app.configValues.sunText = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.show_tidedata_checkbox);
        changed |= (cb.isChecked() != app.configValues.tideList);
        app.configValues.tideList = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.enable_data_writes_checkbox);
        changed |= (cb.isChecked() != app.configValues.dataFileCreation);
        app.configValues.dataFileCreation = cb.isChecked();
        cb = (CheckBox) activity.findViewById(R.id.enable_gps_checkbox);
        changed |= (cb.isChecked() != app.configValues.useNearestGps);
        app.configValues.useNearestGps = cb.isChecked();
        if (changed) {
            app.serialize();
            activity.execDrawChart(-1, -1, false);
        }
        if (app.favorites_list != null) {
            String fav = app.favorites_list.toString();
            fav = fav.replaceAll("[\\[\\] ]", "");
            app.configValues.favorite_sites = fav;
        }
    }

    protected void update_spinner(int id, ArrayList<Integer> ai) {
        if (ai == null || app.titleArray == null) {
            //Log.e("Monitor", "update_spinner fail!: " + ai);
            return;
        }
        //Log.e("Monitor", "update_spinner ok!");
        final MySpinner sp = (MySpinner) activity.findViewById(id);
        if (sp != null) {
            isNearestList = (id == R.id.nearest_spinner);
            final ArrayList<String> sf = new ArrayList();
            if (ai.size() > 0) {
                Iterator<Integer> aii = ai.iterator();
                int i = 0;
                while (aii.hasNext()) {
                    int iss = aii.next();
                    String title = app.titleArray.get(iss);
                    if (isNearestList) {
                        GeoPosition geo = app.nearest_sites_geo.get(i);
                        title += String.format(" | %.1f nm %.1f°", geo.rad, geo.brg);
                    }
                    //Log.e("Monitor","update_spinner: [" + title + "]");
                    sf.add(title);
                    i += 1;
                }
            }
            //Log.e("Monitor", "update_spinner2: apparent success");
            write_spinner_content(sp, sf);
        } else {
            Log.e("Error", "null spinner reference in ConfigurationManager.update_spinner");
        }
    }

    protected void write_spinner_content(final MySpinner sp, final ArrayList<String> sf) {
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        //Log.e("Monitor", "write_spinner_content1 ok");
                        sp.phonyEvent = true;
                        try {
                            ArrayAdapter<String> spa = new ArrayAdapter<String>(activity, R.layout.support_simple_spinner_dropdown_item, sf);
                            spa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            sp.setAdapter(spa);
                            //Log.e("Monitor", "write_spinner_content2: " + spa);
                            //sp.invalidate();
                            // force display of default item
                        } catch (Exception e) {
                            Log.e("Error", "write_spinner_content: " + e.toString());
                        }
                    }
                }
        );
    }

    protected void read_one_control(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.radio24HR:
                app.configValues.ampmFlag = false;
                break;
            case R.id.radioAMPM:
                app.configValues.ampmFlag = true;
                break;
            case R.id.radioMeters:
                app.configValues.heightUnits = 0;
                break;
            case R.id.radioFeet:
                app.configValues.heightUnits = 1;
                break;
            case R.id.radioKnots:
                app.configValues.velocityUnits = 2;
                break;
            case R.id.radioMPH:
                app.configValues.velocityUnits = 3;
                break;
            case R.id.radioMS:
                app.configValues.velocityUnits = 4;
                break;
        }
        app.serialize();
        updateChart();
    }

    protected void processEvents(View v) {
        read_one_control(v);
    }

    protected void updateChart() {
        activity.execDrawChart(-1, -1, false);
    }
}
