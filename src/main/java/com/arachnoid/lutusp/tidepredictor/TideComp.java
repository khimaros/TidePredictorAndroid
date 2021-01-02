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

import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Created by lutusp on 9/15/17.
 */

final public class TideComp {

    String heightUnitsStr[] = {"Meters", "Feet", "Knots", "MPH", "m/s"};
    //TidePredictorActivity main; // link to main class
    TidePredictorApplication app;
    public ArrayList<String> siteIndex;
    //boolean indexFileValid = false;
    //boolean indexValid = false;
    TreeMap<String, TreeMap> root = new TreeMap();
    String tideDataFile;
    SiteSet siteSet;

    TideComp(TidePredictorApplication m) {
        siteIndex = new ArrayList();
        app = m;//(TidePredictorApplication) main.getApplication();
        tideDataFile = "harmonics/harmonics_06_14_2004.txt";
        siteSet = new SiteSet();
    }

    long setDT(long t) {
        // is daylight time in effect?

        t += siteSet.tz * 3600.0; // move to local time
        t -= t % 86400; // round off to nearest day in local time

        GregorianCalendar g = new GregorianCalendar();
        g.setTime(new Date((t + 43200) * 1000));
        boolean dst = isDST(g); // use local noon

        siteSet.daylightInEffect = ((app.configValues.daylightTime == 2) | ((app.configValues.daylightTime == 1) & dst));

        return t;
    }

    String padChar(String sv, int n, String p) {
        StringBuilder s = new StringBuilder();
        n -= sv.length();
        for (int i = 0; i < n; i++) {
            s.append(p);
        }
        s.append(sv);
        return s.toString();
    }

    String padChar(int v, int n, String p) {
        return padChar("" + v, n, p);
    }

    String hmsFormat(int h, int m) {
        return "" + padChar(h, 2, "0") + ":" + padChar(m, 2, "0");
    }

    String formatSunHMS(double hour, boolean seconds) {
        String result = "";
        if (hour < 0) {
            result = "[Below]";
        } else if (hour > 24) {
            result = "[Above]";
        } else {
            int h, m, s;
            h = (int) hour;
            m = (int) (hour * 60.0);
            m %= 60;
            s = (int) (hour * 3600);
            s %= 60;
            // not going to work
            TimeBundle tb = hourAmPmFormat(h, " AM", " PM");
            if (seconds) {
                result = padChar(tb.hour, 2, "0") + ":" + padChar(m, 2, "0") + ":" + padChar(s, 2, "0") + tb.ampm;
                //result.Format("%02d:%02d:%02d%s",h,m,s,ampm);
            } else {
                result = padChar(tb.hour, 2, "0") + ":" + padChar(m, 2, "0") + tb.ampm;
                //result.Format("%02d:%02d%s",h,m,ampm);
            }
        }
        return result;
    }

    String formatSunHMS(double hour) {
        return formatSunHMS(hour, false);
    }


    TimeBundle hourAmPmFormat(int h, String am, String pm) {
        TimeBundle result = new TimeBundle(h);
        if (app.configValues.ampmFlag) {
            result.ampm = (result.hour >= 12) ? pm : am;
            result.hour %= 12;
            result.hour = (result.hour < 1) ? 12 + result.hour : result.hour;
        }
        return result;
    }

    String formatDate(GregorianCalendar pos, boolean database, boolean timeOnly, boolean includeYear, boolean roundUp, String delim) {
        String result = "";
        String yearStr = "";
        if (includeYear) {
            yearStr = "/" + pos.get(Calendar.YEAR);
        }

        if (database) { // need seconds
            StringBuilder sb = new StringBuilder();
            sb.append(pos.get(Calendar.YEAR));
            sb.append("-");
            sb.append(pos.get(Calendar.MONTH) + 1);
            sb.append("-");
            sb.append(pos.get(Calendar.DAY_OF_MONTH));
            sb.append(delim);
            sb.append(padChar(pos.get(Calendar.HOUR_OF_DAY), 2, "0"));
            sb.append(":");
            sb.append(padChar(pos.get(Calendar.MINUTE), 2, "0"));
            sb.append(":");
            sb.append(padChar(pos.get(Calendar.SECOND), 2, "0"));
            //result.Format("%d,%02d,%02d,%02d:%02d:%02d%s",pos.GetYear(),pos.GetMonth(),pos.GetDay(),hour,pos.GetMinute(),pos.GetSecond(),ampm);
            result = sb.toString();

        } else { // minutes, no seconds, so round up
            if (roundUp) {
                pos.add(Calendar.SECOND, 30);
            }
            int hour = pos.get(Calendar.HOUR_OF_DAY);
            TimeBundle tb = hourAmPmFormat(hour, " AM", " PM");
            if (timeOnly) {
                result = "" + padChar(tb.hour, 2, "0") + ":" + padChar(pos.get(Calendar.MINUTE), 2, "0") + tb.ampm;
                //result.Format("%02d:%02d%s",hour,pos.GetMinute(),ampm);
            } else {
                result = TideConstants.dowNames[pos.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY] + " " + padChar((pos.get(Calendar.MONTH) + 1), 2, "0") + "/" + padChar(pos.get(Calendar.DAY_OF_MONTH), 2, "0") + yearStr + " " + padChar(tb.hour, 2, "0") + ":" + padChar(pos.get(Calendar.MINUTE), 2, "0") + ":" + padChar(pos.get(Calendar.SECOND), 2, "0") + tb.ampm;
                //result.Format("%s %d/%02d%s %02d:%02d%s",dowNames[pos.GetDayOfWeek()-1],pos.GetMonth(),pos.GetDay(),yearStr,hour,pos.GetMinute(),ampm);
            }
        }
        return result;
    }

    String formatDataString(
            int i,
            ArrayList data,
            boolean database,
            boolean timeOnly,
            String unitsTag,
            boolean includeYear,
            boolean includeSun,
            boolean includeLHTag,
            String delim) {
        StringBuilder text = new StringBuilder();
        char[] lohi = {'L', 'H'};
        char[] ebbflow = {'E', 'F'};
        if (siteSet.current) {
            lohi = ebbflow;
        }
        long t = ((TideEvent) data.get(i)).t;
        //System.out.println(t + "," + a + "," + b);

        double normHeight = ConvertHeight(siteSet, ((TideEvent) data.get(i)).height);
        GregorianCalendar utc = new GregorianCalendar();
        int raw = utc.getTimeZone().getRawOffset();
        SimpleTimeZone utc_tz = new SimpleTimeZone(0, "");
        utc_tz.setRawOffset(raw);
        utc.setTimeZone(utc_tz);
        GregorianCalendar tt = new GregorianCalendar();
        //tt.setTimeZone(utc_tz);
        tt.setTime(new Date(t * 1000));
        long ut = t - (long) (siteSet.tz * 3600);
        utc.setTime(new Date(ut * 1000));
        String utc_st = formatDate(utc, database, timeOnly, includeYear, true, delim);
        String st = formatDate(tt, database, timeOnly, includeYear, true, delim);
        String v;
        if (((TideEvent) data.get(i)).slack) {
            if (!database) {
                v = "Slack  ";
            } else {
                v = "0.0" + delim + "S";
                //v.Format("%+6.2f %c",0.0,'S');
            }
        } else {
            if (database) {
                v = "" + formatDouble(normHeight, 2);
                //v.Format("%+6.2f",normHeight,(((TideEvent)data.get(i)).high)?lohi[1]:lohi[0]);
            } else { // display string
                v = "" + formatDouble(normHeight, 1);
                //v.Format("%+6.1f",normHeight,(((TideEvent)data.get(i)).high)?lohi[1]:lohi[0]);
            }
            if (includeLHTag) {
                v += ((database) ? delim : " ") + ((((TideEvent) data.get(i)).high) ? lohi[1] : lohi[0]);
            }
        }
        if (database) {
            if (includeSun) {
                setDT(t);
                Rts Twilight, Sun;
                Sun = app.sunComp.compRTS(siteSet, t, 0);
                Twilight = app.sunComp.compRTS(siteSet, t, 1);
                text.append(utc_st);
                text.append(delim);
                text.append(st);
                text.append(delim);
                text.append(v);
                text.append(delim);
                text.append(unitsTag);
                text.append(delim);
                text.append(formatSunHMS(Twilight.rise, true));
                text.append(delim);
                text.append(formatSunHMS(Sun.rise, true));
                text.append(delim);
                text.append(formatSunHMS(Sun.transit, true));
                text.append(delim);
                text.append(formatSunHMS(Sun.set, true));
                text.append(delim);
                text.append(formatSunHMS(Twilight.set, true));
                text.append(delim);
                text.append((siteSet.daylightInEffect) ? "1" : "0");
            /*text.Format("%s,%6s,%s,%s,%s,%s,%s,%s,%d",st,v,unitsTag
            ,formatSunHMS(Twilight.rise,true)
            ,formatSunHMS(Sun.rise,true)
            ,formatSunHMS(Sun.transit,true)
            ,formatSunHMS(Sun.set,true)
            ,formatSunHMS(Twilight.set,true)
            ,main.daylightInEffect
            );*/
            } else {
                text.append(utc_st);
                text.append(delim);
                text.append(st);
                text.append(delim);
                text.append(v);
                text.append(delim);
                text.append(unitsTag);
                //text.Format("%s,%6s,%s",st,v,unitsTag);
            }
        } else {
            int w = (includeLHTag) ? 8 : 6;
            text.append(padString(st, 8, 0) + padString(v, w, 2));
        }
        return text.toString();
    }

    // type 0 = left, 1 = center, 2 = right
    String padString(String data, int width, int type) {
        int len = data.length();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < width; i++) {
            s.append(' ');
        }
        int offset = (type == 0) ? 0
                : (type == 1) ? (width - len) / 2
                : width - len;
        offset = (offset < 0) ? 0 : offset;
        s.replace(offset, offset + len, data);
        return s.toString();
    }

    // set font to Courier
    //public void tweakFont(Component c) {
    //    c.setFont(new Font("Monospaced", c.getFont().getStyle(), c.getFont().getSize()));
    //}

    // verify that data source files have not changed
    public void verifyIndex() {
        buildNewIndex();
        setupIndex();
        // load a page that wasn't ready earlier
        //app.currentActivity.manageWebView1();
    }


    public void setupIndex() {
        if (root.entrySet().size() < 2) {
            root.clear();
            ArrayList record;
            Iterator<Integer> i = app.indexArray.keySet().iterator();
            while (i.hasNext()) {
                String s = app.indexArray.get(i.next());
                record = parseDelimLine(s, "\t");
                recurseTree(root, record, 0);
            }
        }
        parseTreeSetup();
    }

    private void recurseTree(TreeMap<String, TreeMap> node, ArrayList<String> record, int depth) {
        TreeMap t;
        if (depth < 4 && depth < record.size()) {
            String s = record.get(depth);
            if (depth == 0) {
                if (s.equals("C")) {
                    s = "Current Stations";
                }
                if (s.equals("T")) {
                    s = "Tide Stations";
                }
            }
            t = node.get(s);
            if (t == null) {
                t = new TreeMap();
                node.put(s, t);
            }
            recurseTree(t, record, depth + 1);
        }
    }

    class PersistentLong {
        public long n = 0;

        PersistentLong(long n) {
            this.n = n;
        }
    }

    protected void parseTreeSetup() {
        //Log.e("parseTree", "enter");
        if (root != null) {
            try {
                StringBuilder out = new StringBuilder();
                PersistentLong count = new PersistentLong(0);
                parseTree(root, 0, count, out);
                String insert = out.toString();
                String partition = "<!-- BEGIN data insertion block -->\n";
                String page = app.currentActivity.copyAssetToString("webpage/siteIndex.html", "UTF-8");
                String[] segs = page.split(partition);
                app.sitePage = segs[0] + partition + insert + segs[1];
                // temporary for testing
                //PrintWriter fout = new PrintWriter(main.dataPrefix + "/webpage/siteIndex.html");
                //fout.write(app.sitePage);
                //fout.close();
            } catch (Exception e) {
                Log.e("ParseTree", e.toString());
            }
        }
        //Log.e("parseTree", "exit");
    }

    private void parseTree(TreeMap<String, TreeMap> node, int level, PersistentLong count, StringBuilder out) {
        String s;
        for (String t : node.keySet()) {
            TreeMap<String, TreeMap> child = node.get(t);
            if (child != null) {
                if (level == 3) {
                    s = String.format("<span class=\"datum\" onClick=\"Android.processClick('%d');\">%s</span>\n", count.n, t);
                    out.append(s);
                    count.n += 1;
                } else {
                    s = String.format("<div class=\"expfc\">%s\n", t);
                    out.append(s);
                    s = "<div class=\"expcc\">\n";
                    out.append(s);
                    parseTree(child, level + 1, count, out);
                    out.append("</div>\n</div>\n");
                }
            } else {
                out.append(String.format("%s\n", t));
            }
        }
    }

    boolean isDST(GregorianCalendar g) {
        boolean result;
        if (app.configValues.daylightTime == 1) {
            TimeZone tz = g.getTimeZone();
            result = tz.inDaylightTime(g.getTime());
        } else {
            result = (app.configValues.daylightTime == 2);
        }
        return result;
    }

    String FormatDegMin(double v, char psign, char msign) {
        long vd, vm;
        vm = (long) (Math.abs(v) * 6000.0);
        vd = vm / 6000;
        vm %= 6000;
        return "" + vd + TideConstants.DEGREE_CHAR + " " + formatDouble((vm / 100.0), 2, false) + "' " + ((v < 0) ? msign : psign);
    }

    String formatDouble(double x, int dp, boolean needSign) {
        String plus = (needSign) ? "+" : "";
        String minus = (needSign) ? "-" : "";
        String digits = "000000000000";
        String format = "###0." + digits.substring(0, dp);
        DecimalFormat d = new DecimalFormat(plus + format + ";" + minus + format);
        return d.format(x);
    }

    String formatDouble(double x, int dp) {
        return formatDouble(x, dp, true);
    }

    String FormatLatLng(double lat, double lng) {
        String result;
        result = "Lat. " + FormatDegMin(lat, 'N', 'S') + " Lng. " + FormatDegMin(lng, 'E', 'W');
        // original:
        // result.Format("Lat. %s Lng. %s",FormatDegMin(lat,'N','S'),FormatDegMin(lng,'E','W'));
        return result;

    }

    // return a field delimited by white space
    final class FieldData {

        public String field;
        public int pos;

        FieldData(String f, int p) {
            pos = p;
            field = f;
        }
    }

    final class ScanData {

        public String line = "";
        public long pos = 0;

        ScanData(String li, long p) {
            pos = p;
            line = li;
        }

        ScanData() {
        }
    }

    int skipWS(String data, int p) {
        while (Character.isWhitespace(data.charAt((int) p)) && p < data.length()) {
            p++;
        }
        return p; // return first non-ws char
    }

    FieldData getWSField(String data, int p) {
        p = skipWS(data, p);
        int op = p;
        while (p < data.length() && !Character.isWhitespace(data.charAt((int) p))) {
            p++;
        }
        String s = data.substring(op, p);
        return new FieldData(s, p);
    }

    // parse on white space
    ArrayList parseLine(String data) {
        data = data.trim();
        ArrayList record = new ArrayList();
        int p = 0;
        while (p < data.length()) {
            FieldData f = getWSField(data, p);
            p = f.pos;
            //System.out.println("parseline: " + f.field);
            record.add(f.field);
        }
        return record;
    }

    ArrayList<String> parseDelimLine(String data, String token) {
        ArrayList<String> v = new ArrayList();
        int a = 0, b;
        int tokLen = token.length();
        while ((b = data.indexOf(token, a)) != -1) {
            String field = data.substring(a, b);
            v.add(field);
            a = b + tokLen;
        }
        if (a <= data.length()) {
            v.add(data.substring(a));
        }
        return v;
    }

    String getUnitsTag(SiteSet siteSet) {
        return heightUnitsStr[app.configValues.displayUnits];
    }

    double ConvertHeight(SiteSet siteSet, double v) {
        int conv = app.configValues.displayUnits - siteSet.units;
        // siteSet.units: 0 = meters, 1 = feet
        if (siteSet.units < 2) {
            if (conv == -1) {
                v *= TideConstants.FEET_TO_METERS;
            } else if (conv == 1) {
                v *= TideConstants.METERS_TO_FEET;
            }
        } else {
            // siteSet.units: 2 = knots
            // if zero, no conversion is necessary
            if (conv == 1) {
                v *= TideConstants.KNOTS_TO_MPH;
            } else if (conv == 2) {
                v *= TideConstants.KNOTS_TO_MS;
            }
        }
        return v;
    }

    // find string, provide post-index
    final class FindData {

        String data;
        String srch;
        int index;

        public FindData(String d, String s, int i) {
            data = d;
            srch = s;
            index = i;
        }
    }

    // find string, provide post-index

    boolean findStr(FindData d) {
        //System.out.println("findstr: looking for " + d.srch + " in " + d.data);
        boolean found = ((d.index = d.data.indexOf(d.srch)) != -1);
        if (found) {
            //System.out.println("findstr: found " + d.srch);
            d.index += d.srch.length();
        }
        return found;
    }

    int checkYear(SiteSet siteSet, int year) {
        year = (year < siteSet.startYear) ? siteSet.startYear : year;
        year = (year >= siteSet.endYear) ? siteSet.endYear - 1 : year;
        return year;
    }

    int useCalendar(Date t, int field) {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(t);
        return c.get(field);
    }

    void yearCorrect(SiteSet siteSet, int year) {
        GregorianCalendar g = new GregorianCalendar(year, 0, 1, 0, 0, 0);
        Date t = g.getTime();
        siteSet.epochTime = t.getTime() / 1000;
        int yi = year - siteSet.startYear;
        yi = (yi < 0) ? 0 : yi;
        yi = (yi >= siteSet.equMax) ? siteSet.equMax - 1 : yi;
        if (siteSet.harmBase.length >= siteSet.constituentMax) {
            for (int i = 0; i < siteSet.constituentMax; i++) {
                Harmonic temp = new Harmonic();
                temp.epoch = siteSet.harmBase[i].epoch - siteSet.equArgs[i][yi];
                temp.amplitude = siteSet.harmBase[i].amplitude * siteSet.nodeFacts[i][yi];
                siteSet.harm[i] = temp;
            }
        }
        siteSet.currentYear = year;
    }

    void testYear(SiteSet siteSet, long t) {
        Date tt = new Date(t * 1000); // "t" is in seconds, dummy
        int yi = useCalendar(tt, Calendar.YEAR);
        checkYear(siteSet, yi);
        if (siteSet.currentYear != yi) {
            yearCorrect(siteSet, yi);
        }
    }

    double performSignedRoot(double v) {
        return (v < 0) ? -Math.sqrt(-v) : Math.sqrt(v);
    }

    double timeToTide(SiteSet siteSet, long t, boolean setYear) {
        double result = siteSet.baseHeight;
        if (setYear) {
            testYear(siteSet, t);
        }
        t -= siteSet.epochTime;
        double th = t * TideConstants.SECONDS_TO_HOURS;
        for (int i = 0; i < siteSet.constituentMax; i++) {
            result += siteSet.harm[i].amplitude * Math.cos(siteSet.constSpeeds[i] * th - siteSet.harm[i].epoch);
        }
        if (siteSet.needRoot) {
            result = performSignedRoot(result);
        }
        return result;
    }

    // for scaling the graph
    void
    findHiLoWater(SiteSet siteSet, Date t, int step) {
        long start = t.getTime() / 1000;
        long end = start + 31536000L; // year
        double tide, lo = 1, hi = -1;
        for (; start < end; start += 7200) { // 2-hour intervals
            tide = timeToTide(siteSet, start, true);
            hi = (hi < tide) ? tide : hi;
            lo = (lo > tide) ? tide : lo;
        }
        siteSet.mLoWater = ConvertHeight(siteSet, lo);
        siteSet.mHiWater = ConvertHeight(siteSet, hi);
        siteSet.gLoWater = ((int) siteSet.mLoWater) - 1;
        siteSet.gHiWater = ((int) siteSet.mHiWater) + 1;
        siteSet.currentDisplayUnits = app.configValues.displayUnits;
    }

    double timeToTideDeriv(SiteSet siteSet, long t, int deriv, boolean setYear) {
        double result = 0;
        double term;
        double offset = TideConstants.M_PID2 * deriv;
        if (setYear) {
            testYear(siteSet, t);
        }
        t -= siteSet.epochTime;
        double th = t * TideConstants.SECONDS_TO_HOURS;
        for (int i = 0; i < siteSet.constituentMax; i++) {
            term = siteSet.harm[i].amplitude //term = ((Harmonic)siteSet.harm.get(i)).amplitude
                    * Math.cos(offset + siteSet.constSpeeds[i] * th //* Math.cos(offset + ((Double)siteSet.constSpeeds.get(i)).doubleValue() * th
                    - siteSet.harm[i].epoch);
            //- ((Harmonic)siteSet.harm.get(i)).epoch);
            for (int j = 0; j < deriv; j++) {
                term *= siteSet.constSpeeds[i];
                //term *= ((Double)siteSet.constSpeeds.get(i)).doubleValue();
            }
            result += term;
        }
        if (siteSet.needRoot) {
            result = performSignedRoot(result);
        }
        return result;
    }

    // find time where |f(t)'| <= delta
    // time aa = old time, a = new time. Root lies between them.
    // the previously computed value is passed as oldv
    long findRoot(SiteSet siteSet, long oa, long a, double oldv) {
        long step = (oa - a);
        int i = 0;
        double v;
        boolean oldSign = oldv > 0, sign;
        while ((i++ < 50) && Math.abs(step) > 1) { // accepted delta time = 1 second
            a += step;
            v = timeToTideDeriv(siteSet, a, 1, false);
            sign = v > 0;
            v = Math.abs(v);
            // if result sign changes or |new result| > |old|, bisect
            if ((oldv < v) || (oldSign != sign)) {
                step = -step / 2;
            }
            oldv = v;
            oldSign = sign;
        }
        //CString q;
        //q.Format("FindRoot required %d\n",i);
        //TRACE(q);
        return a;
    }

    long findSlackRoot(SiteSet siteSet, long oa, long a, double oldv) {
        long step = (oa - a);
        int i = 0;
        double v;
        boolean oldSign = oldv > 0, sign;
        while ((i++ < 50) && Math.abs(step) > 1) {
            a += step;
            v = timeToTide(siteSet, a, false);
            sign = v > 0;
            v = Math.abs(v);
            // if result sign changes or |new result| > |old|, bisect
            if ((oldv < v) || (oldSign != sign)) {
                step = -step / 2;
            }
            oldv = v;
            oldSign = sign;
        }
        //CString q;
        //q.Format("slack required %d\n",i);
        //TRACE(q);
        return a;
    }

    long getNextEventTime(SiteSet siteSet, long t, boolean increm) {
        boolean sign, oldsign = timeToTideDeriv(siteSet, t, 1, false) > 0;
        int count = 0;
        do {
            t += (increm) ? 960 : -960; // 16 minutes
            sign = timeToTideDeriv(siteSet, t, 1, false) > 0;
        } while ((count++ < 90) && (sign == oldsign)); // 24h max
        return t;
    }

    void predictSlackEvents(SiteSet siteSet, long start, long end, long bt, long et, ArrayList
            results, ThreadStopper threadStopper) {
        long oldTime = start;

        double height;
        boolean sign, oldSign = timeToTide(siteSet, start, true) > 0;
        for (long t = start; t <= end && (threadStopper == null || !threadStopper.stop); t += 960) {
            height = timeToTide(siteSet, t, true);
            if ((sign = height > 0) != oldSign) {
                long rt = findSlackRoot(siteSet, oldTime, t, height);
                // an empirical adjustment! No justification
                // except to make my listing agree with the "official" published
                // slack water readings
                TideEvent te = new TideEvent();
                te.t = rt + 30;
                te.height = 0;
                te.slack = true;
                if (te.t >= bt && te.t <= et) {
                    results.add(te);
                }
            }
            oldSign = sign;
            oldTime = t;
        }
    }

    // "start" and "end" are a pair of tide events that straddle the period
    // bt and et are fixed time delmiters on the period
    ArrayList predictTideEvents(SiteSet siteSet, long start, long end, long bt,
                                long et, ThreadStopper threadStopper) {
        //System.out.println("predict times: " + start + "," + end);
        //startProgressBar(pBar, start, end, 0);
        ArrayList results = new ArrayList();
        long oldTime = start;
        double height;
        boolean sign, oldSign = timeToTideDeriv(siteSet, start, 1, true) > 0;
        // 16-minute intervals
        for (long t = start; t <= end && (threadStopper == null || !threadStopper.stop); t += 960) {
            TideEvent te = new TideEvent();
            height = timeToTideDeriv(siteSet, t, 1, true);
            if ((sign = height > 0) != oldSign) {
                long rt = findRoot(siteSet, oldTime, t, height);
                height = timeToTide(siteSet, rt, true);
                te.t = rt;
                te.height = height;
                // is this a high? Get 2d deriv
                te.high = timeToTideDeriv(siteSet, rt, 2, false) < 0;
                if (te.t >= bt && te.t <= et) {
                    results.add(te);
                }
            }
            oldSign = sign;
            oldTime = t;
        }
        if (siteSet.current) {
            predictSlackEvents(siteSet, start, end, bt, et, results, threadStopper);
        }
        Collections.sort(results, new CompareTC());
        return results;
    }

    // comparator to sort the tide event list
    final class CompareTC implements Comparator {

        public int compare(java.lang.Object a, java.lang.Object b) {
            return (int) (((TideEvent) a).t - ((TideEvent) b).t);
        }
    }

    Date IncDecCTime(SiteSet siteSet, Date tt, int yr, int mon, int day) {

        GregorianCalendar qt = new GregorianCalendar(
                useCalendar(tt, Calendar.YEAR), useCalendar(tt, Calendar.MONTH), useCalendar(tt, Calendar.DAY_OF_MONTH), 0, 0, 0); // don't allow daylight time shift


        if (day > 0) {
            qt.add(Calendar.DAY_OF_MONTH, 1);
        } else if (day < 0) {
            qt.add(Calendar.DAY_OF_MONTH, -1);
        }

        GregorianCalendar check = new GregorianCalendar(siteSet.startYear, 0, 1, 0, 0, 0);

        if (qt.getTime().getTime() < check.getTime().getTime()) {
            qt = check;
        }

        int y, m, d;

        y = qt.get(Calendar.YEAR);
        m = qt.get(Calendar.MONTH);
        d = qt.get(Calendar.DAY_OF_MONTH);


        m += mon;

        if (m < 1) {
            m = 12;
            y--;
        }
        if (m > 12) {
            m = 1;
            y++;
        }
        y += yr;

        y = (y < siteSet.startYear) ? siteSet.startYear : y;
        y = (y > siteSet.startYear + siteSet.equMax - 1) ? siteSet.startYear + siteSet.equMax - 1 : y;
        checkYear(siteSet, y);
        GregorianCalendar r = new GregorianCalendar(y, m - 1, d, 0, 0, 0);
        return r.getTime();
    }


    double stringToDouble(String s) {
        double v = 0;
        try {
            v = new Double(s).doubleValue();
        } catch (Exception e) {
            //System.out.println("stringToDouble error: [" + s + "]");
            //e.printStackTrace();
        }
        return v;
    }

    int stringToInt(String s) {
        int v = 0;
        try {
            v = new Integer(s).intValue();
        } catch (Exception e) {
            //System.out.println("stringToInt error: [" + s + "]");
            //e.printStackTrace();
        }
        return v;
    }

    private String readNonBlankLine(ListIterator<String> lIter) {
        String r = null;

        do {
            r = lIter.next();
            if (r != null) {
                r = r.trim();
            }
        } while (r != null && r.length() == 0);
        return r;
    }

    protected void processSiteData(int index) {
        if (app.indexArray != null && index >= 0 && index < app.indexArray.size()) {
            //Log.e("processSite: ", si);
            //int index = Integer.parseInt(si);
            app.configValues.lastDisplayedSite = index;
            //RandomAccessFile raf;
            String indexEntry = app.indexArray.get(index);
            GregorianCalendar now = new GregorianCalendar();
            //System.out.println(index);
            readHarmonicData(siteSet);
            readSite(siteSet, indexEntry, now.get(Calendar.YEAR));
            //resetTitle();
            app.currentActivity.showingChart = true;
        }
    }

    void readSite(SiteSet siteSet, String indexEntry, int year) {
        //Log.e("readSite: ", indexEntry);
        siteSet.currentYear = -1;
        siteSet.currentDisplayUnits = -1;
        siteSet.needRoot = false;
        siteSet.current = false;
        siteSet.valid = false;
        String line;
        //ArrayList record;
        long pbc = 0;
        int neededFile;
        //line = getFullEntry(query);
        if (indexEntry.length() > 0) {
            //THEMAIN->siteString = THEMAIN->tideTable.shortForm(line.c_str(),4);
            ArrayList<String> record = parseDelimLine(indexEntry, "\t");
            if (record.size() >= 9) {
                siteSet.current = (record.get(0)).equals("C");
                siteSet.name = record.get(3);
                siteSet.shortName = siteSet.name;
                int p = siteSet.name.indexOf(",");
                if (p != -1) {
                    siteSet.shortName = siteSet.name.substring(0, p);
                }
                //System.out.println("siteSet.name: " + siteSet.name);
                siteSet.lng = stringToDouble(record.get(4));
                //sscanf(record[4].c_str(),"%lf",&siteSet.lng);
                siteSet.lat = stringToDouble(record.get(5));
                //sscanf(record[5].c_str(),"%lf",&siteSet.lat);

                int h, m;
                ArrayList<String> temp = parseDelimLine(record.get(6), ":");
                h = stringToInt(temp.get(0));
                m = stringToInt(temp.get(1));

                siteSet.tz = h + (m / 60.0);

                neededFile = stringToInt((String) record.get(7));
                int index = stringToInt(record.get(8));

                try {
                    ListIterator<String> lIter = app.harmonicArray.listIterator(index);
                    line = readNonBlankLine(lIter); // timezone line
                    //Log.e("First line: ", "(timezone): " + line);
                    record = parseDelimLine(line, ":");
                    double hh, mm;
                    hh = stringToDouble(record.get(0));
                    //sscanf(record[0].c_str(),"%lf",&hh);
                    mm = stringToDouble(record.get(1));
                    //sscanf(record[1].c_str(),"%lf",&mm);
                    hh += (hh < 0) ? -mm / 60.0 : mm / 60.0;
                    if (h != 0) {
                        siteSet.tz = hh;
                    } else if (record.size() > 2) {
                        String tzs = record.get(2);
                        System.out.println("Time zone name: " + tzs);
                        TimeZone tz = TimeZone.getTimeZone(tzs);
                        siteSet.tz = tz.getRawOffset() / 3600000;
                    } else {
                        //System.out.println("error in determining time zone for " + siteSet.name);
                    }

                    line = readNonBlankLine(lIter); // base and units line
                    //Log.e("Second line: ", "(base and units): " + line);
                    record = parseLine(line);
                    //System.out.println("base and units: " + line);
                    //System.out.println("gives: " + record.toString());
                    siteSet.baseHeight = stringToDouble(record.get(0));
                    // sscanf(record[0].c_str(),"%lf",&siteSet.baseHeight);
                    // meters = 0, feet = 1, knots = 2
                    String s = record.get(1);
                    siteSet.units = (s.indexOf("meters") != -1) ? 0 : 1;
                    if (s.indexOf("knots") != -1) {
                        siteSet.units = 2;
                    }
                    if (s.indexOf("^2") != -1) {
                        siteSet.needRoot = true;
                    }

                    // now read the constituent amplitudes and epochs
                    siteSet.harmBase = new Harmonic[siteSet.constituentMax];
                    int i = 0;
                    while ((i < siteSet.constituentMax) && ((line = readNonBlankLine(lIter)) != null)) {
                        //Log.e("Harmonic lines: ", "(numeric): " + line);
                        record = parseLine(line);
                        //System.out.print("from: " + record + " -> ");
                        if (record.size() > 2) {
                            Harmonic pair = new Harmonic();
                            //System.out.println("harmonic pair: " + record.toString());
                            pair.amplitude = stringToDouble(record.get(1));
                            pair.epoch = stringToDouble(record.get(2));
                            //sscanf(record[1].c_str(),"%lf",&pair.amplitude);
                            //sscanf(record[2].c_str(),"%lf",&pair.epoch);
                            pair.epoch *= TideConstants.RADIANS;
                            siteSet.harmBase[i] = pair;
                            //System.out.println(pair);
                        } else {
                            // TRACE("parse error in readSite\n");
                            //raf.close();
                            errorMessage("parse error in readSite", "wrong record size");
                        }
                        i++;
                    }
                    //raf.close();
                    //Log.e("readSite", "total read: " + i + ", " + siteSet.constituentMax);
                } catch (Exception e) {
                    //e.printStackTrace();
                    Log.e("readSite err", e.toString());
                }
            }
            siteSet.valid = true;
            // for now:
            siteSet.fullName = indexEntry;
            // this must be fixed
            yearCorrect(siteSet, year);
            findHiLoWater(siteSet, new GregorianCalendar(year, 0, 1, 0, 0, 0).getTime(), 172800); // 48-hour intervals
        }
        //Log.e("readSite: ", "exit: " + siteSet);
    }

    String getComDatLine(Iterator<String> it, ScanData sd) {
        String line;
        sd.pos = 0;
        try {
            line = null;
            while (it.hasNext() && (line = it.next()) != null) {
                sd.pos += line.length() + 1;
                if ((line.charAt(0) == '#') && (line.indexOf('!') != -1)) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("getComDatLine err: ", e.toString());
            return null;
        }
        if (line != null) {
            line = line.trim();
        }
        return line;
    }

    void getNonCommentLine(ListIterator<String> lIter, ScanData sd) {
        sd.line = "";
        sd.pos = 0;
        try {
            while ((sd.line = lIter.next()) != null) {
                //sd.pos += sd.line.length() + 1;
                //System.out.println("looking at: ["+d.line+"]");
                if (sd.line.length() > 0) {
                    if (!(sd.line.charAt(0) == '#')) {
                        break;
                    }
                }
            }
            if (sd.line != null) {
                //System.out.println("nonCommentLinePreTrim: ["+d.line+"]");
                sd.line = sd.line.trim();
                //System.out.println("nonCommentLinePostTrim: ["+d.line+"]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void getNonCommentLinePos(Iterator<String> it, ScanData sd) {
        sd.line = null;
        sd.pos = 0;
        try {
            do {
                if (!it.hasNext()) {
                    sd.line = null;
                    break;
                }
                sd.line = it.next();
                if (sd.line != null) {
                    sd.pos += sd.line.length() + 1;
                    sd.line = sd.line.trim();
                }

            } while ((sd.line != null) && (sd.line.charAt(0) == '#'));
        } catch (Exception e) {
            Log.e("getNonCommentLP err: ", e.toString());
        }
    }

    void buildNewIndex() {

        if(app.harmonicArray == null || app.configValues.decodedHarmonics == null) {
            String rawData = app.currentActivity.copyAssetToString(tideDataFile, "ISO-8859-1");
            app.harmonicArray = new ArrayList<>(Arrays.asList(rawData.split("\n")));
            // are the harmonics not read yet?
            if (app.configValues.decodedHarmonics == null) {
                //Log.e("ERROR", "Generating harmonics");
                app.configValues.decodedHarmonics = harmonicsToArrayList(app.harmonicArray);
            }
            app.indexArray = new LinkedHashMap();
            app.reverseArray = new LinkedHashMap();
            app.titleArray = new LinkedHashMap();

            int i = 0;
            for (String s : app.configValues.decodedHarmonics) {
                String[] fields = s.split("\t");
                app.indexArray.put(i, s);
                app.reverseArray.put(fields[3], i);
                app.titleArray.put(i, fields[3]);
                i += 1;
            }
        }

}

    ArrayList<String> harmonicsToArrayList(
            ArrayList<String> lines) {
        ListIterator<String> lineIter = lines.listIterator();
        ArrayList<String> data = new ArrayList();
        double lat, lng;
        int count;
        long filePosition;
        int p = 0;
        String line = "", placeName, tzs, zoneName, state, category;
        //long pos = 0;
        ScanData sd = new ScanData();
        while (line != null) {
            zoneName = "";
            tzs = "";
            lat = 0;
            lng = 0;
            category = "T"; // tide reference station
            count = 0; // count of valid fields
            if ((line = getComDatLine(lineIter, sd)) != null) {
                boolean specialComment;
                do {
                    //System.out.println("1[" + line + "]");
                    FindData d = new FindData(line, "!longitude:", p);
                    if (findStr(d)) {
                        //System.out.println("success!");
                        count++;

                        //System.out.println("success! " + line);
                        line = line.substring(d.index);
                        lng = stringToDouble(line);
                        //System.out.println("success!" + line);
                        //System.out.println("2[" + line + "] : " + d.index);
                        //sscanf(line,"%lf",&lng);
                    } else {
                        d.srch = "!latitude:";
                        if (findStr(d)) {
                            count++;

                            line = line.substring(d.index);
                            lat = stringToDouble(line);
                            //System.out.println("3[" + line + "] : "+ d.index);
                            //sscanf(line.c_str(),"%lf",&lat);
                        }
                    }
                    line = null;
                    if (lineIter.hasNext()) {
                        line = lineIter.next();
                    }
                    if (line != null) {
                        //pos += line.length() + 1;
                        specialComment = ((line.charAt(0) == '#') && (line.indexOf('!') != -1));
                    } else {
                        specialComment = false;
                    }
                } while (specialComment);
                if (line != null && line.charAt(0) != '#') {
                    count++;
                    line = line.trim();
                    placeName = line;
                    state = "";
                    if ((p = placeName.lastIndexOf(',')) != -1) {
                        state = placeName.substring(p + 1);
                        if ((p = state.indexOf('(')) != -1) {
                            state = state.substring(0, p);
                        }
                    }
                    if (state.indexOf(')') != -1) {
                        state = "";
                    }
                    if ((p = state.indexOf(" Current")) != -1) {
                        state = state.substring(0, p);
                        category = "C"; // current station
                    }
                    state = state.trim();
                    if (state.length() == 0) {
                        state = "Other";
                    }

                    filePosition = lineIter.nextIndex();
                    getNonCommentLinePos(lineIter, sd); // tz and zone name
                    //pos += sd.pos;
                    ArrayList<String> vs = parseLine(sd.line);
                    if (vs.size() > 1) {
                        tzs = vs.get(0);
                        zoneName = (vs.get(1)).substring(1);
                        if ((p = zoneName.indexOf('/')) != -1) {
                            zoneName = zoneName.substring(0, p);
                        }
                        count += 2;
                    }
                    if (count == 5) {
                        String out = category + "\t" + zoneName + "\t" + state + "\t" + placeName + "\t" + lng + "\t" + lat + "\t" + tzs + "\t" + 0 + "\t" + filePosition;

                        data.add(out);
                    }
                }
            }
        }
        Collections.sort(data);
        return data;
    }

    void errorMessage(String msg, String fn) {
        //String indexPath = main.basePath + "/" + main.indexName;
        String message = "Data Format Error: " + msg + ", file " + fn + "\n\n" + "This error may result from an index file that is not\n" + "synchronized with the installed data files. The remedy\n" + "is to delete the index file. The file is located at\n" + "???" + " on this system.\n" + "This file will be automatically regenerated\n" + "teh next time you run JTides.\n\n" + "Press OK to delete the file, Cancel to preserve it.";

    }

    // duplicating character-by-character
    // effect of RandomAccessFile

    private String readNonWSChars(CharacterIterator ci) {
        StringBuilder sb = new StringBuilder();
        try {
            char c;
            do {
                c = ci.next();
            } while (Character.isWhitespace(c) && c != CharacterIterator.DONE);
            while (!Character.isWhitespace(c) && c != CharacterIterator.DONE) {
                sb.append(c);
                c = ci.next();
            }
        } catch (Exception e) {
        }
        return sb.toString();
    }

    boolean readHarmonicData(SiteSet siteSet) {

        if (siteSet.constituentMax > 0) {
            // we already have this data list
            return true;
        }
        siteSet.equMax = -1;
        siteSet.nodeMax = -1;
        siteSet.constituentMax = -1;

        ListIterator<String> lIter = app.harmonicArray.listIterator();
        String line;
        // read constituent count
        ScanData sd = new ScanData();
        getNonCommentLine(lIter, sd);
        if (sd.line != null) {
            siteSet.constituentMax = stringToInt(sd.line);
            //Log.e("constituentMax", "" + siteSet.constituentMax + "," + sd.line);
        } else {
            //raf.close();
            Log.e("readData", "Cannot read argument count in " + siteSet.dataFileName);
            return false;
        }
        //System.out.println(siteSet.constituentMax);
        siteSet.harm = new Harmonic[siteSet.constituentMax];
        siteSet.constSpeeds = new double[siteSet.constituentMax];
        //System.out.println("read: " + siteSet.constituentMax);
        for (int i = 0; i < siteSet.constituentMax && sd.line != null; i++) { // read constituent speeds
            getNonCommentLine(lIter, sd);
            if (sd.line != null) {
                ArrayList<String> record = parseLine(sd.line);
                double v = stringToDouble(record.get(1));
                //sscanf(record[1].c_str(),"%lf",&v);
                v *= TideConstants.RADIANS;
                siteSet.constSpeeds[i] = v;
            } else {
                //raf.close();
                Log.e("readDataFile", "Premature end of constituent data in " + siteSet.dataFileName);
                return false;
            }
        }
        getNonCommentLine(lIter, sd);
        if (sd.line != null) { // read base year for data
            siteSet.startYear = stringToInt(sd.line);
            //System.out.println("startYear: " + siteSet.startYear);
        } else {
            //raf.close();
            Log.e("readDataFile", "Cannot read base year in " + siteSet.dataFileName);
            return false;
        }

        // read equ count
        getNonCommentLine(lIter, sd);
        if (sd.line != null) {
            siteSet.equMax = stringToInt(sd.line);
            siteSet.endYear = siteSet.startYear + siteSet.equMax;
            //System.out.println("equMax: " + siteSet.equMax);
        } else {
            //raf.close();
            Log.e("readDataFile", "Cannot read equ count in " + siteSet.dataFileName);
            return false;
        }


        // equilibrium table is equMax * comstituentMax * sizeof(double)
        siteSet.equArgs = new double[siteSet.constituentMax][siteSet.equMax];
        // make a list of constituents, a subset of the harmonic data array
        StringBuilder sb = new StringBuilder();
        int n = 2;
        while (lIter.hasNext()) {
            line = lIter.next();
            if (line.charAt(0) != '#') {
                sb.append(line + '\n');
                if (line.equals("*END*")) {
                    n -= 1;
                    if (n <= 0) {
                        break;
                    }
                }
            }
        }
        String values = sb.toString();
        //Log.e("Constituent List: ", values);
        CharacterIterator ci = new StringCharacterIterator(values);
        for (int i = 0; i < siteSet.constituentMax; i++) {
            //THEMAIN->updateProgressBar(0,i,siteSet.constituentMax);
            line = readNonWSChars(ci); // skip group name
            //Log.e("Group Name: ",line);
            //ifs >> line; // skip group name
            //double[] tempdata = new double[siteSet.equMax];
            double v;
            for (int j = 0; j < siteSet.equMax; j++) { // read equilibrium arguments
                line = readNonWSChars(ci);
                //Log.e("equ arg: ", line);
                v = stringToDouble(line);
                // ifs >> v;
                v *= TideConstants.RADIANS; // invert values
                // tempdata.add(new Double(v));
                //tempdata[j] = v;
                siteSet.equArgs[i][j] = v;
            }
            //siteSet.equArgs.add(tempdata);
            if (line == null) {
                //raf.close();
                Log.e("readDataFile", "Premature end of equ data in " + siteSet.dataFileName);
                return false;
            }
        }
        line = readNonWSChars(ci);

        if (!line.equals("*END*")) {
            //raf.close();
            Log.e("readDataFile", "Missing equ end mark in " + siteSet.dataFileName);
            return false;
        }
        // read node count
        line = readNonWSChars(ci);
        //getNonCommentLine(lIter, sd);
        if (line != null) {
            siteSet.nodeMax = stringToInt(line);
            //Log.e("nodeMax: ", "" + siteSet.nodeMax);
        } else {
            //raf.close();
            errorMessage("Cannot read node count", siteSet.dataFileName);
            return false;
        }

        siteSet.nodeFacts = new double[siteSet.constituentMax][siteSet.nodeMax];
        for (int i = 0; i < siteSet.constituentMax; i++) {

            line = readNonWSChars(ci);
            //Log.e("Intermediate line: ", "[" + line + "]");
            // ifs >> line; // skip group name
            //double[] tempdata = new double[siteSet.nodeMax];
            double v;
            for (int j = 0; j < siteSet.nodeMax; j++) { // read node factors
                line = readNonWSChars(ci);
                v = stringToDouble(line);
                //Log.e("Node value:", "" + line + "," + v);
                //ifs >> v;
                //tempdata[j] = v;
                siteSet.nodeFacts[i][j] = v;
            }
            //siteSet.nodeFacts.add(tempdata);
            if (line == null) {
                //raf.close();
                errorMessage("Premature end of node data", siteSet.dataFileName);
                return false;
            }
        }
        //System.out.println("nodefacts #s: " + siteSet.nodeMax + "," + siteSet.constituentMax);

        line = readNonWSChars(ci);

        if (!line.equals("*END*")) {
            //raf.close();
            //System.out.println("end mark? [" + line + "]");
            errorMessage("Missing node end mark", siteSet.dataFileName);
            return false;
        }

        return true;
    }

}
