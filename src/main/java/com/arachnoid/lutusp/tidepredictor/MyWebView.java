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
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by lutusp on 9/17/17.
 */

final public class MyWebView extends WebView {

    TidePredictorActivity main;
    TidePredictorApplication app;
    boolean fresh;

    public MyWebView(Context m) {
        super(m);
        main = (TidePredictorActivity) m;
        app = (TidePredictorApplication) main.getApplication();


    }

    // WebView invocation always uses this constructor
    public MyWebView(Context m, AttributeSet as) {
        super(m, as);
        main = (TidePredictorActivity) m;
        app = (TidePredictorApplication) main.getApplication();
        addJavascriptInterface(new WebAppInterface(main), "Android");
        WebSettings ws = getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setBuiltInZoomControls(true);
    }

    protected void readContent() {
        //Log.e("Monitor","MyWebView:readContent()");
        if (main.stationList == this) {
            if (app.sitePage == null || app.sitePage.length() < 1000) {
                app.tideComp.parseTreeSetup();
            }
            loadDataWithBaseURL("", app.sitePage, "text/html", "UTF-8", "");
        }
    }
}
