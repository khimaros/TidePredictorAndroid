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
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by lutusp on 9/19/17.
 */

final public class CalendarView extends WebView {
    TidePredictorActivity activity; // parent
    TidePredictorApplication app;

    public CalendarView(Context context) {
        super(context);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (TidePredictorActivity) context;
        app = (TidePredictorApplication) activity.getApplication();

        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (app.configValues.dataFileCreation) {
                    activity.calGen.saveCalendar();
                }
                else {
                    activity.graphView.createDatePickerDialog();
                }
                return true;
            }
        });
        addJavascriptInterface(new WebAppInterface(context), "Android");
        WebSettings ws = getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setBuiltInZoomControls(true);
        activity.calendarView = this;
        loadCalendarDisplay();
    }

    public void loadCalendarDisplay() {
        String page = activity.calGen.drawCalendar();
        loadDataWithBaseURL("", page, "text/html", "UTF-8", "");
    }


}
