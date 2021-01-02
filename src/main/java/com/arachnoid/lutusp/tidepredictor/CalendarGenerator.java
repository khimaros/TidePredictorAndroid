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

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by lutusp on 9/19/17.
 */

final public class CalendarGenerator {
    TidePredictorActivity main;
    TidePredictorApplication app;

    String css;

    public CalendarGenerator(TidePredictorActivity m) {
        main = m;
        app = (TidePredictorApplication) main.getApplication();
        StringBuilder sb = new StringBuilder();
        sb.append("<style type=\"text/css\">\n");
        sb.append("body {\n");
        // this is required to display correctly on large and small displays
        sb.append("width:1000px;\n");
        sb.append("}\n");
        sb.append("body * {\n");
        sb.append("font-family:monospace;\n");
        sb.append("}\n");
        sb.append("table {\n");
        sb.append("border-collapse: collapse;\n");
        sb.append("}\n");
        sb.append("table,th,td {\n");
        sb.append("border:1px solid black;\n");
        sb.append("}\n");
        sb.append(".preweek {\n");
        sb.append("background:#f0f0f0;\n");
        sb.append("}\n");
        sb.append("th {\n");
        sb.append("background:#fffff0;\n");
        sb.append("}\n");
        sb.append("td {\n");
        sb.append("background:#f8fff8;\n");
        sb.append("vertical-align:text-top;\n");
        sb.append("padding:4px;\n");
        sb.append("}\n");
        sb.append(".dom,.today {\n");
        sb.append("display:block;");
        sb.append("background:#c0e0ff;\n");
        sb.append("padding:4px;");
        sb.append("margin-bottom:4px;");
        sb.append("}\n");
        sb.append(".today {\n");
        sb.append("background:#fff0e0;\n");
        sb.append("}\n");
        sb.append("</style>\n");
        css = sb.toString();
    }

    String plusSign(int v) {
        String s = (v > 0) ? "+" : "";
        return s + v;
    }

    String drawCalendar() {
        String page = drawCalendarCore(false);
        //try {
        //    String path = main.dataPrefix + "/testpage.html";
        //    BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        //    writer.write(page);
        //    writer.close();
        //} catch (Exception e) {

        //}
        return page;
    }

    String drawCalendarCore(boolean export_form) {
        if (!app.tideComp.siteSet.valid) {
            return "";
        }
        StringBuffer page = new StringBuffer();
        page.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        page.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
        StringBuffer body = new StringBuffer();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        GregorianCalendar calTime = new GregorianCalendar(
                app.chartCal.get(Calendar.YEAR),
                app.chartCal.get(Calendar.MONTH), 1, 0, 0, 0);
        boolean sameyear = app.chartCal.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        int month = calTime.get(Calendar.MONTH);
        int firstday = calTime.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        GregorianCalendar tempTime = (GregorianCalendar) calTime.clone();
        int d = tempTime.getActualMaximum(Calendar.DAY_OF_MONTH);
        int weeks = ((firstday + d - 1) / 7) + 1;
        int this_doy = now.get(Calendar.DAY_OF_YEAR);
        String dateString = TideConstants.monthNames[month] + " " + app.chartCal.get(Calendar.YEAR);
        StringBuffer textb = new StringBuffer();
        if (app.tideComp.isDST(tempTime)) {
            textb.append(app.tideComp.siteSet.name + " (GMT" + plusSign((int) app.tideComp.siteSet.tz + 1) + ") (Daylight Time)");
        } else {
            textb.append(app.tideComp.siteSet.name + " (GMT" + plusSign((int) app.tideComp.siteSet.tz) + ")");
        }
        textb.append(", " + dateString);
        textb.append(". Units: " + app.tideComp.getUnitsTag(app.tideComp.siteSet));
        String title = textb.toString();
        body.append("<head>");
        body.append("<title>" + title + "</title>");
        body.append(css);
        body.append("</head>");
        body.append("<body>\n");
        body.append("<div align=\"center\">");
        body.append("<p><b>" + title + "</b></p>\n");
        body.append("<table width=\"100%\">\n");
        StringBuffer row = new StringBuffer();
        for (String dow : TideConstants.dowNames) {
            row.append("<th><div align=\"center\"><b>" + dow + "</b></div></th>\n");
        }
        body.append("<tr>" + row.toString() + "</tr>\n");
        row.setLength(0);
        // begin row in prior month
        for (int i = 0; i < firstday; i++) {
            row.append("<td class=\"preweek\">&nbsp;</td>");
        }
        while (calTime.get(Calendar.MONTH) == month) {
            int dow = calTime.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            if (dow == 0) {
                row.append("</tr>\n<tr>\n");
            }
            int dom = calTime.get(Calendar.DAY_OF_MONTH);
            int doy = calTime.get(Calendar.DAY_OF_YEAR);
            int year = calTime.get(Calendar.YEAR);
            String dateTag = String.format("%s.%s.%s", month, dom, year);
            String js = (export_form) ? "" : "onClick=\"Android.processNewDate('" + dateTag + "');\"";
            String className = (doy == this_doy && sameyear && !export_form) ? "today" : "dom";
            row.append("<td " + js + "><div align=\"center\"><b class=\"" + className + "\">- " + dom + " -</b></div>");
            String ctl = calendarTideList(calTime.getTime(), false);
            row.append(ctl);
            row.append("</td>\n");
            calTime.add(Calendar.DAY_OF_MONTH, 1);
        }
        firstday = calTime.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        // complete row in next month
        for (; firstday % 7 != 0; firstday++) {
            row.append("<td class=\"preweek\">&nbsp;</td>");
        }
        body.append("<tr>" + row.toString() + "</tr>\n");
        body.append("</table>\n");
        body.append("</div>\n");
        if (export_form) {
            String copy = String.format("<p><div align=\"center\"><b>From <a href=\"http://arachnoid.com/android/TidePredictor\">TidePredictor</a>, Copyright &copy; 2017, <a href=\"http://arachnoid.com/administration\">P. Lutus</a></b></div></p>");
            body.append(copy);
        }
        body.append("</body>\n");
        page.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" + body.toString() + "</html>");
        return page.toString();
    }


    String calendarTideList(Date d, boolean isPrinting) {
        long a, b;
        a = d.getTime() / 1000;
        b = a + 86400;
        long prevt = app.tideComp.getNextEventTime(app.tideComp.siteSet, a, false); // previous
        long postt = app.tideComp.getNextEventTime(app.tideComp.siteSet, b, true); // next
        ArrayList data = app.tideComp.predictTideEvents(app.tideComp.siteSet, prevt, postt, a, b, null);

        String text;
        StringBuffer out = new StringBuffer();

        int n = data.size();
        n = (n < 5) ? 5 : n;

        for (int i = 0; i < data.size(); i++) {
            text = app.tideComp.formatDataString(i, data, false, true, "", isPrinting, false, true, "");
            out.append(text + "<br/>");
        }
        return out.toString();
    }

    protected void saveCalendar() {
        main.createDataDirectories();
        String page = drawCalendarCore(true);
        String name = app.tideComp.siteSet.name;
        String date = main.graphView.formatTitleDate(app.chartCal);
        name = (name + "_" + date).replaceAll("[^A-Za-z0-9]", "_");
        String path = String.format("%s/%s.html", main.publicDocumentDir, name);
        if (!main.stringToFile(page, path)) {
            main.permissionsDialog();
        } else {
            Toast.makeText(main, "This calendar has been saved in the Documents directory",
                    Toast.LENGTH_LONG).show();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            Uri uri = Uri.parse("file://" + path);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setType("text/html");
            main.startActivity(Intent.createChooser(sendIntent, "This is a tide calendar from TidePredictor"));
        }
    }

}
