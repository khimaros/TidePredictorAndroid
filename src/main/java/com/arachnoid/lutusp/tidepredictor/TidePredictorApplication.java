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

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

/**
 * Created by lutusp on 9/17/17.
 */

/*
This class exists primarily to preserve resources that take a lot of time to create
and that would otherwise be discarded when the device is rotated or an activity is dismissed.
 */

final public class TidePredictorApplication extends android.app.Application {
    TideComp tideComp = null;
    SunComp sunComp = null;
    ConfigValues configValues = null;
    ArrayList<String> harmonicArray = null; // the full,raw database
    LinkedHashMap<Integer, String> indexArray = null; // a map of integers to full descriptive strings
    ArrayList<Integer> favorites_list;
    ArrayList<Integer> nearest_sites;
    ArrayList<GeoPosition> nearest_sites_geo;
    LinkedHashMap<Integer, String> titleArray = null; // a map of integers to title-only strings
    LinkedHashMap<String, Integer> reverseArray = null; // a map of title-only strings to integers
    GregorianCalendar chartCal;
    String sitePage = "";
    String PROGRAM_VERSION = "";
    private static TidePredictorApplication singleton;
    String configurationSerialPath = "TidePredictorConfiguration.obj";
    TidePredictorActivity currentActivity = null;


    public TidePredictorApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        configValues = new ConfigValues();
        sunComp = new SunComp(this);
        tideComp = new TideComp(this);
        nearest_sites = new ArrayList();
        nearest_sites_geo = new ArrayList();
        try {
            PROGRAM_VERSION = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (chartCal == null) {
            chartCal = new GregorianCalendar();
        }
    }

    protected void deSerialize() {
        Object obj = deSerializeCore(configurationSerialPath);
        if(obj != null) {
            configValues = (ConfigValues) obj;
        }
    }

    protected Object deSerializeCore(String path) {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        Object result = null;
        try {
            fis = openFileInput(path);
            if (fis != null && fis.available() > 0) {
                in = new ObjectInputStream(fis);
                result = in.readObject();
                in.close();
            }
        } catch (Exception e) {
            Log.e("ERROR","app:deSerializeCore: " + e.toString());
        }
        return result;
    }

    protected void serialize() {
        serializeCore(configurationSerialPath,configValues);
        //serializeCore(harmonicsSerialPath);
    }

    protected void serializeCore(String path,Object data) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = openFileOutput(path, Context.MODE_PRIVATE);
            out = new ObjectOutputStream(fos);
            out.writeObject(data);
            out.close();
        } catch (Exception e) {
            Log.e("ERROR", "app:serialize: " + e.toString());
        }
    }
}
