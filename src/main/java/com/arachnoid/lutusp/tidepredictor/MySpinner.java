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
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;


/**
 * Created by lutusp on 9/20/17.
 */

final public class MySpinner extends android.support.v7.widget.AppCompatSpinner {

    TidePredictorActivity main;
    TidePredictorApplication app;
    boolean phonyEvent = false;

    public MySpinner(Context context, final AttributeSet as) {
        super(context, as);
        //Log.e("Monitor", "mySpinner:constructor: " + context);
        main = (TidePredictorActivity) context;
        app = (TidePredictorApplication) main.getApplication();

        setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                //Log.e("Monitor", "mySpinner:onItemSelected:a: phonyevent: " + phonyEvent);
                if (phonyEvent) {
                    phonyEvent = false;
                    return;
                }
                if (parentView == main.findViewById(R.id.favorites_spinner) || parentView == main.findViewById(R.id.nearest_spinner)) {

                    try {
                        String s = (String) parentView.getItemAtPosition(position);
                        // the nearest-sites spinner entries have an appended
                        // distance after a pipe symbol, so ...
                        String[] fields = s.split(" \\|");
                        // fields[0] always contains either the split result or the entire string
                        int targetIndex = app.reverseArray.get(fields[0]);
                        //Log.e("Monitor", "mySpinner:onItemSelected:b: " + targetIndex);
                        main.execDrawChart(targetIndex, main.TAB_CHART, true);
                    } catch (Exception e) {
                        //Log.e("Error", "mySpinner:onItemSelected: " + e.toString());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //Log.e("Monitor", "mySpinner:nothingSelected: ");
                //onItemSelected(parentView);
            }
        });
    }
}
