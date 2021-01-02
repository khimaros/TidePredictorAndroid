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
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by lutusp on 9/17/17.
 */

final public class WebAppInterface {
    TidePredictorActivity main;
    TidePredictorApplication app;

    /**
     * Instantiate the interface and set the context
     */
    WebAppInterface(Context c) {
        main = (TidePredictorActivity) c;
        app = (TidePredictorApplication) main.getApplication();
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(main, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void processClick(final String s) {
        //Log.e("Monitor","webAppInterface:processClick:" + s);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run () {
                main.execDrawChart(Integer.parseInt(s),main.TAB_CHART,true);
            }
        });
    }

    @JavascriptInterface
    public void processNewDate(String s) {
        //Log.e("Monitor","webAppInterface:processNewDate:" + s);
        String[] date = s.split("\\.");
        int m = Integer.parseInt(date[0]);
        int d = Integer.parseInt(date[1]);
        int y = Integer.parseInt(date[2]);
        app.chartCal.set(Calendar.YEAR,y);
        app.chartCal.set(Calendar.MONTH,m);
        app.chartCal.set(Calendar.DAY_OF_MONTH,d);
        // prevent automatic chart time reset
        main.most_recent_interaction = main.getCurrentTime();
        //main.execDrawChart(-1,1,true);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run () {
                main.execDrawChart(-1,main.TAB_CHART,true);
            }
        });
    }

    @JavascriptInterface
    public void jsCallback(String s) {
        Log.e("JavaScript callback", s);
    }
}
