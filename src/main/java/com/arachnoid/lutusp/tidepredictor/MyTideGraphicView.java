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

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * Created by lutusp on 9/18/17.
 */

final public class MyTideGraphicView extends View implements DatePickerDialog.OnDateSetListener {


    TidePredictorActivity activity; // parent
    TidePredictorApplication app;
    int siteOffset;
    int localOffset;
    double oldTimeZone = -100;
    boolean isLocal;
    int scalex;
    int scaley;
    int gstartx;
    int gstarty;
    boolean mouseDown = false;
    int mouseX, mouseY;
    public int graphWidth = 1;

    Rts sunData, twilightData;

    int divsx, divsy;

    int startx, endx;
    int starty, endy;
    double rangex, rangey;

    float viewWidth, viewHeight;

    float strokeWidth = 1;
    float textSize = 1;

    Paint gp;

    public MyTideGraphicView(Context context) {
        super(context);
    }

    public MyTideGraphicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (TidePredictorActivity) context;
        app = (TidePredictorApplication) activity.getApplication();
        divsx = 12;
        divsy = 8;
        setChartCalendar();
        activity.graphView = this;

        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                activity.toggleFullScreen(null);
            }
        });

        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (app.configValues.dataFileCreation) {
                    saveGraphicImage();
                } else {
                    createDatePickerDialog();
                    //String msg = "Must enable data file creation";
                    //Toast.makeText(main, msg, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }

    // this dialog creator is shared with CalendarView
    public void createDatePickerDialog() {
        int year = app.chartCal.get(Calendar.YEAR);
        int month = app.chartCal.get(Calendar.MONTH);
        int day = app.chartCal.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                activity, activity.graphView, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        getChartCalendar().set(Calendar.YEAR, year);
        getChartCalendar().set(Calendar.MONTH, month);
        getChartCalendar().set(Calendar.DAY_OF_MONTH, dayOfMonth);
        activity.most_recent_interaction = activity.getCurrentTime();
        activity.execDrawChart(-1, -1, false);
    }

    private void saveGraphicImage() {
        activity.createDataDirectories();
        String name = app.tideComp.siteSet.name;
        String date = formatTitleDate(app.chartCal);
        name = (name + "_" + date).replaceAll("[^A-Za-z0-9]", "_");
        String pic_path = String.format("%s/%s.jpg", activity.publicGraphicDir, name);
        //Log.e("Monitor","pic_path: " + pic_path);
        Bitmap bitmap = Bitmap.createBitmap(2000, 1125, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawChart(canvas, false);
        File outputFile = new File(pic_path);
        try {
            OutputStream fout = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            fout.flush();
            fout.close();
            String msg = "This chart has been saved in the Pictures directory";
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            Uri uri = Uri.parse("file://" + pic_path);
            //Log.e("Message","Uri as sent: " + uri);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setType("image/jpeg");
            activity.startActivity(Intent.createChooser(sendIntent, "This is a tide chart from TidePredictor"));

        } catch (Exception e) {
            //Log.e("save graphic ", "error: " + e.toString());
            activity.permissionsDialog();
        }
    }


    @Override
    protected void onDraw(Canvas g) {
        super.onDraw(g);
        // use clipping bounds, not width and height
        //Rect rect = g.g.getClipBounds();
        try {
            drawChart(g, true);
        } catch (Exception e) {
            Log.e("onDraw", e.toString());
        }
    }


    // set today 00:00h
    public GregorianCalendar getCurrentCal() {

        // offset to tidal location
        siteOffset = (int) (app.tideComp.siteSet.tz * 3600000);
        TimeZone tz = TimeZone.getDefault();
        // offset to user's local time
        if (app.configValues.timeZone == -14.0) {
            // use system timezone
            localOffset = tz.getRawOffset();
        } else if (app.configValues.timeZone == -13.0) {
            // use site time zone
            localOffset = siteOffset;
        } else {
            // use user-chosen timezone
            localOffset = (int) (app.configValues.timeZone * 3600000);
        }
        GregorianCalendar now = new GregorianCalendar();
        // changed 03/02/2007
        tz.setRawOffset(siteOffset);
        now.setTimeZone(tz);
        now.setTimeInMillis(new Date().getTime());
        GregorianCalendar gc = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        gc.setTimeZone(tz);
        return gc;
    }

    public GregorianCalendar getChartCalendar() {
        return app.chartCal;
    }

    protected void setChartCalendar() {
        app.chartCal = getCurrentCal();
    }

    private void checkClock() {
        long now = new Date().getTime();
        GregorianCalendar cal = new GregorianCalendar();
        int nowDay = cal.get(GregorianCalendar.DAY_OF_YEAR);
        int nowMonth = cal.get(GregorianCalendar.MONTH);
        int nowYear = cal.get(GregorianCalendar.YEAR);
        int chartDay = app.chartCal.get(GregorianCalendar.DAY_OF_YEAR);
        int chartMonth = app.chartCal.get(GregorianCalendar.MONTH);
        int chartYear = app.chartCal.get(GregorianCalendar.YEAR);
        // if day not now, reset to present
        if (nowDay == chartDay && nowMonth == chartMonth && nowYear == chartYear) {
            setChartCalendar();
            // if more than five minutes since change, reset to present
        } else if (now > activity.most_recent_interaction + activity.returnToCurrentTimeDelayMillis) {
            setChartCalendar();
        }
    }

    public void drawChart(Canvas g, boolean include_overlays) {
        if (app.tideComp.siteSet == null || !app.tideComp.siteSet.valid) {
            return;
        }
        // determine if the clock needs resetting
        // based on no recent user interaction
        checkClock();
        viewWidth = g.getWidth();
        viewHeight = g.getHeight();
        Rectangle r = new Rectangle(0, 0, g.getWidth(), g.getHeight());
        float w = (viewWidth > viewHeight) ? viewWidth : viewHeight;
        // set master stroke width
        strokeWidth = w / 1000.0f;
        gp = new Paint();
        gp.setAntiAlias(true);
        gp.setTypeface(app.configValues.boldText ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        gp.setStrokeWidth(strokeWidth);
        textSize = w / 60;
        gp.setTextSize(textSize);
        paintImage(g, gp, r, include_overlays);
    }

    public void paintImage(Canvas g, Paint gp, Rectangle r, boolean include_overlays) {
        if (oldTimeZone != app.configValues.timeZone) {
            setChartCalendar();
            oldTimeZone = app.configValues.timeZone;
        }
        // distinguish between tidal height and current charts
        if (app.tideComp.siteSet.units < 2) {
            app.configValues.displayUnits = app.configValues.heightUnits;
        } else {
            app.configValues.displayUnits = app.configValues.velocityUnits;
        }

        isLocal = app.configValues.timeZone != -13;
        //if (app.tideComp.siteSet.units < 2) {
        if (app.tideComp.siteSet.currentDisplayUnits != app.configValues.displayUnits) {
            app.tideComp.findHiLoWater(app.tideComp.siteSet, new GregorianCalendar(app.chartCal.get(Calendar.YEAR), 0, 1, 0, 0, 0).getTime(), 172800); // 48-hour intervals
        }
        //}

        gp.setColor(activity.backgroundColor);
        g.drawRect(r.x, r.y, r.x + r.width, r.y + r.height, gp);
        startx = 0;
        endx = graphWidth * 24;
        starty = (int) app.tideComp.siteSet.gLoWater;
        endy = (int) app.tideComp.siteSet.gHiWater;

        // make range even
        if (((endy - starty) % 2) != 0) {
            endy++;
        }

        // desirable scale divisions,
        // in order of preference
        divsy = 8; // default value
        int divset[] = {8, 10, 6, 12, 14, 7, 9, 11, 0};
        int i = 0;
        while ((divset[i] != 0) && (((starty - endy) % divset[i])) != 0) {
            i++;
        }

        if (divset[i] != 0) {
            divsy = divset[i];
        }

        rangex = endx - startx;
        rangey = endy - starty;
        int rawScalex = r.width;
        int rawScaley = r.height;

        gstartx = r.x + (rawScalex / 16);
        gstarty = r.y + (rawScaley / 32);
        scalex = rawScalex - gstartx - (rawScalex / 30);
        scaley = rawScaley - gstarty - (rawScaley / 20);
        //if (!isPrinting) {
        int delta = rawScalex / 64;
        gstartx += delta;
        scalex -= delta * 2;
        delta = rawScaley / 64;
        gstarty += delta;
        scaley -= delta * 2;
        //}
        drawGraph(g, gp, include_overlays);
    }


    final class GraphicsData {
        Canvas g;
        float x;
        float y;
        float ox;
        float oy;
        boolean start;

        GraphicsData(Canvas g) {
            this.g = g;
        }
    }

    void drawTideCurve(Canvas g, Paint gp, boolean include_overlays) {
        float sw = strokeWidth * ((app.configValues.thickLine) ? 3f : 2f);
        gp.setStrokeWidth(sw);

        gp.setColor(activity.lineColor);
        //g.setColor(lineColor);
        long t = app.chartCal.getTime().getTime() / 1000;


        //System.out.println(t);
        GraphicsData gd = new GraphicsData(g);
        gd.start = true;
        // pstep assures that every pixel is drawn separately
        double step = ((double) endx - startx) / scalex;
        //double step = (isPrinting) ? pstep : .25;

        for (gd.x = startx; gd.x <= endx + .001; gd.x += step) // 24 hours
        {
            gd.y = (float) app.tideComp.timeToTide(app.tideComp.siteSet, (int) (t + (gd.x * 3600)), true);
            //double ya = gd.y;
            gd.y = (float) app.tideComp.ConvertHeight(app.tideComp.siteSet, gd.y);
            drawLine(gd, gp);
        }
        gp.setStrokeWidth(strokeWidth);
    }

    void drawLine(GraphicsData gd, Paint gp) {
        float x = (int) (gstartx + ((gd.x - startx) * scalex) / (endx - startx));
        float y = (int) (gstarty + scaley - ((gd.y - starty) * scaley) / (endy - starty));
        if (!gd.start) {
            gd.g.drawLine(gd.ox, gd.oy, x, y, gp);
        }
        gd.ox = x;
        gd.oy = y;
        gd.start = false;
    }

    void makeGrid(Canvas g, Paint gp, boolean matNumsOnly) {
        gp.setColor(activity.textColor);
        // g.SetTextColor(RGB(0,0,0));

        // end marks for vertical lines
        int a = gstarty, b = gstarty + scaley;

        //dc->SetTextAlign(TA_CENTER);

        // just to get the vertical part

        //Paint.FontMetrics fm = gp.getFontMetrics();
        //float h = fm.ascent + fm.descent;
        //float w = fm.
        Rect te = new Rect();
        gp.getTextBounds("00:00", 0, 5, te);

        int offsetx = scalex / 96;
        int offsety = scaley / 24;

        int dx = (graphWidth == 1) ? divsx : graphWidth;

        if (matNumsOnly) {
            dx = 12;
        }

        int v;

        gp.setColor(activity.gridColor);
        //g.setColor(gridColor);

        for (int x = 0; x <= dx; x++) {
            int qx = (int) (gstartx + ((x * scalex) / (double) dx));
            if ((app.configValues.chartGrid) && (!matNumsOnly)) {
                gp.setColor(activity.gridColor);
                //g.setColor(gridColor);
                g.drawLine(qx, a, qx, b, gp);
                //dc->MoveTo(qx,a);
                //dc->LineTo(qx,b);
            }
            if ((app.configValues.gridNums) && ((graphWidth == 1) || (matNumsOnly))) {
                String q;
                if (matNumsOnly) {
                    v = x * 120;
                } else {
                    v = (int) ((((double) (endx - startx) * x) / (double) divsx) + startx) * 60;
                }
                int hour = (v / 60) % 24;
                int min = v % 60;
                TimeBundle t = app.tideComp.hourAmPmFormat(hour, "a", "p");
                //q.Format("%02d:%02d%s",hour,min,ampm);
                q = app.tideComp.hmsFormat(t.hour, min) + t.ampm;
                //dc->TextOut(qx,b+offsety,q);
                gp.setColor(activity.textColor);

                g.drawText(q, qx - te.width() / 2, b + offsety, gp);
            }
        }

        if (!matNumsOnly) {

            //dc->SetTextAlign(TA_RIGHT);


            //CSize te = dc->GetTextExtent("0"); // just to get the vertical part

            // end marks for horizontal lines
            a = gstartx;
            b = gstartx + scalex;

            for (int y = 0; y <= divsy; y++) {
                int qy = (int) (scaley + gstarty - ((y * scaley) / (double) divsy));
                if (app.configValues.chartGrid) {
                    //g.setColor(Color.blue);
                    gp.setColor(activity.gridColor);
                    g.drawLine(a, qy, b, qy, gp);
                    //dc->MoveTo(a,qy);
                    //dc->LineTo(b,qy);
                }
                String q;
                if (app.configValues.gridNums) {
                    double dv = (((double) (endy - starty) * y) / (double) divsy) + starty;
                    q = app.tideComp.formatDouble(dv, 1, false);
                    //q.Format("%.1f",dv);
                    //Rectangle tw = fm.getStringBounds(q,g).getBounds();
                    Rect tw = new Rect();
                    gp.getTextBounds(q, 0, q.length(), tw);
                    gp.setColor(activity.textColor);
                    g.drawText(q, gstartx - tw.right - offsetx, qy - te.top / 3, gp);
                }
            }
            if (app.configValues.chartGrid) {

                int qy = (int) (gstarty + scaley * ((rangey + starty) / rangey));
                if ((qy >= gstarty) && (qy <= gstarty + scaley)) {
                    gp.setStrokeWidth(strokeWidth * 2f);
                    gp.setColor(activity.zeroColor);
                    g.drawLine(a, qy, b, qy, gp);
                    gp.setStrokeWidth(strokeWidth);
                }
            }
        }
        //dc->SelectObject(oldLine);
    }

    String createTimeLabel() {
        String s;
        int v = (int) app.configValues.timeZone;
        switch (v) {
            case -14:
                s = "(local time)";
                break;
            case -13:
                s = "(site time)";
                break;
            default:
                s = "(GMT" + ((v >= 0) ? "+" : "") + v + ")";
                break;
        }
        return s;
    }

    void drawCurrent(Canvas g, Paint gp) {
        GregorianCalendar now = new GregorianCalendar();
        TimeZone tz = TimeZone.getDefault();
        tz.setRawOffset(siteOffset);
        now.setTimeZone(tz);
        now.setTimeInMillis(new Date().getTime() + siteOffset);
        // only show the current data line if present day is on display
        //if (now.get(Calendar.YEAR) != getChartCalendar().get(Calendar.YEAR)
        //        || now.get(Calendar.MONTH) != getChartCalendar().get(Calendar.MONTH)
        //        || now.get(Calendar.DAY_OF_MONTH) != getChartCalendar().get(Calendar.DAY_OF_MONTH)) {
        //    return;
        //}

        //double t = new Date().getTime();
        //t = Math.IEEEremainder((t + siteOffset) / 3600000.0, 24.0);


        Rectangle cr = new Rectangle(0, 0, (int) viewWidth, (int) viewHeight);
        //GetClientRect(&cr);

        //dc->SetTextAlign(TA_LEFT);
        //CPen *oldLine = dc->SelectObject(line);
        double x, y;
        if (mouseDown) {
            //x = mouseX;
            x = ((double) mouseX - gstartx) / (double) scalex;
            x = (x < 0) ? 0 : x;
            x = (x > 1) ? 1 : x;
            y = mouseY - (scaley / 48);
            y = (y < gstarty) ? gstarty : y;
            y = (y > gstarty + scaley) ? gstarty + scaley : y;

        } else {
            //CTime now(time(NULL)); // current time
            x = ((now.getTime().getTime() - app.chartCal.getTime().getTime() - siteOffset) / 3600000.0) / rangex;
            x = Math.IEEEremainder(x, 1.0);
            if (x < 0) x = 1.0 + x;
            y = starty + scaley - (scaley / 8);
            if ((x < 0) || (x > 1.0)) { // off the page
                return;
            }
        }
        // now x is normalized 0 <= x <= 1
        double tx = (x - startx) * rangex;
        // pos is in seconds
        long pos = (long) (app.chartCal.getTime().getTime() / 1000 + x * rangex * 3600);
        double height = app.tideComp.timeToTide(app.tideComp.siteSet, pos, true);
        double normHeight = app.tideComp.ConvertHeight(app.tideComp.siteSet, height);
        int gtx = (int) (gstartx + (((tx - startx) / rangex) * scalex));
        gp.setColor(activity.currentColor);
        g.drawLine(gtx, gstarty, gtx, gstarty + scaley, gp);


        boolean timeOnly = (graphWidth == 1);
        String ut = app.tideComp.getUnitsTag(app.tideComp.siteSet);
        GregorianCalendar mouseNow = new GregorianCalendar();
        tz.setRawOffset(localOffset);
        mouseNow.setTimeZone(tz);
        mouseNow.setTimeInMillis(pos * 1000);
        String a1 = app.tideComp.formatDate(mouseNow, false, timeOnly, false, false, "") + " " + app.tideComp.formatDouble(normHeight, 1) + " " + ut;//.charAt(0);
        String a2 = createTimeLabel();
        //q.Format("%s %+.1f %c",formatDate(pos,false,timeOnly,false),normHeight,ut[0]);
        //FontMetrics fm = g.getFontMetrics();
        Rect te1 = new Rect();
        Rect te2 = new Rect();
        gp.getTextBounds(a1, 0, a1.length(), te1);
        gp.getTextBounds(a2, 0, a2.length(), te2);

        // graphic box height and width
        float w1 = (te1.width()) * 1.2f;
        float w2 = (te2.width()) * 1.2f;
        float w = (w1 > w2) ? w1 : w2;
        float h = ((te1.height() + te2.height()) * 2f); // two lines of text

        float gx, gy;
        gx = gtx - w / 2;
        gy = (float) y + h / 8;


        // bracket x position onscreen

        // test left
        gx = (gx < 0) ? 0 : gx;
        // test right
        gx = (gx + w > cr.width) ? (cr.width) - w : gx;

        float bx = gx - w / 16;
        float by = gy + w / 16;
        gp.setColor(0x80ffffff);
        g.drawRect(bx, by - h, bx + w, by, gp);
        gp.setColor(activity.currentColor);
        activity.draw_box(bx, by - h, bx + w, by, g, gp);
        gp.setColor(activity.textColor);
        //float dw = (w1 - w2) / 2;
        //dw = (dw < 0) ? 0 : dw;
        //g.drawText(a1,gx+4,(int)y-fm.getMaxDescent()-te1.height());
        //g.drawText(a2,gx+4+dw,(int)y-fm.getMaxDescent());
        g.drawText(a1, gx, (int) (y - te1.height() * 1.5), gp);
        g.drawText(a2, gx, (int) y, gp);
    }

    int showText(Canvas g, Paint gp, int xpos, int ypos, String text, boolean needErase) {
        //FontMetrics fm = g.getFontMetrics();
        //Rectangle te = fm.getStringBounds(text,g).getBounds();

        Rect te = new Rect();
        gp.getTextBounds(text, 0, text.length(), te);

        int x, y, w, h;
        x = xpos - 4;
        y = ypos;
        w = te.width() + 8;
        h = te.height() + 4;

        if (needErase && app.configValues.listBackground) {
            //Rect cr = new Rect(x, y, w, h);
            gp.setColor(activity.backgroundColor);
            g.drawRect(x, y, w, h, gp);
        }
        gp.setColor(activity.textColor);
        g.drawText(text, xpos, ypos + te.height() - 2, gp);
        return ypos + (int) (textSize * 1.5);
    }

    String plusSign(int v) {
        String s = (v > 0) ? "+" : "";
        return s + v;
    }

    int DrawSitePosText(Canvas g, Paint gp, int ypos) {
        int xpos = gstartx + scalex / 96;
        //dc->SetTextAlign(TA_LEFT);
        String text;
        if (app.tideComp.isDST(app.chartCal)) {
            text = app.tideComp.siteSet.name + " (UTC" + plusSign((int) app.tideComp.siteSet.tz + 1) + ") (Daylight Time)";
        } else {
            text = app.tideComp.siteSet.name + " (UTC" + plusSign((int) app.tideComp.siteSet.tz) + ")";
        }
        ypos = showText(g, gp, xpos, ypos, text, true);
        text = app.tideComp.FormatLatLng(app.tideComp.siteSet.lat, app.tideComp.siteSet.lng);
        String units = ". Units: " + app.tideComp.getUnitsTag(app.tideComp.siteSet);
        int p;
        if ((p = units.indexOf("^")) > 0) {
            units = units.substring(0, p);
        }
        ypos = showText(g, gp, xpos, ypos, text + units, true);
        return ypos;
    }

    void CompSunData(long t, int type) {

        if (type == 0) {
            sunData = app.sunComp.compRTS(app.tideComp.siteSet, t, type);
        } else {
            twilightData = app.sunComp.compRTS(app.tideComp.siteSet, t, type);
        }
    }


    void DrawTimeBox(Canvas g, Paint gp, int ys, int ye, double dxs, double dxe, int color) {
        int xa, xb, ya, yb;

        xa = (int) (gstartx + (dxs / 24.0) * scalex);
        xb = (int) (gstartx + (dxe / 24.0) * scalex);
        ya = ys;
        yb = ye;
        gp.setColor(color);
        g.drawRect(xa, ya, xb, yb, gp);

    }
/*
bool HiColorMode(CDC *dc)
{
        long test = RGB(225,225,225);
        long test2 = dc->GetNearestColor(test); // 256 mode won't match this color
        return(test == test2);
}*/

    void DrawSunLines(Canvas g, Paint gp) {
        if (graphWidth == 1) {
            int ys = gstarty;
            int ye = ys + scaley;

            int dark;
            int twilight;
            int day;

            if (!app.configValues.sunGraphicDark) {
                activity.gridColor = Color.rgb(128, 192, 128);
                dark = Color.rgb(220, 220, 220);
                twilight = Color.rgb(210, 210, 255);
                day = Color.rgb(255, 255, 204);
            } else {
                // the light set
                activity.gridColor = Color.rgb(180, 200, 180);
                dark = Color.rgb(240, 240, 240);
                twilight = Color.rgb(200, 220, 255);
                day = Color.rgb(255, 255, 240);
            }

            Rts twil = twilightData;
            Rts sun = sunData;

            if (twil.rise < 0) {
                twil.rise = 12;
                twil.set = 12;
            } else if (twil.rise > 24) {
                twil.rise = 0;
                twil.set = 24;
            }

            if (sun.rise < 0) {
                sun.rise = 12;
                sun.set = 12;
            } else if (sun.rise > 24) {
                sun.rise = 0;
                sun.set = 24;
            }
            if (sun.rise > sun.set) {
                sun.rise = 0;
                sun.set = 24;
            }

            if (twil.rise > twil.set) {
                twil.rise = 0;
                twil.set = 24;
            }

            // nighttime zones

            DrawTimeBox(g, gp, ys, ye, 0, twil.rise, dark);
            DrawTimeBox(g, gp, ys, ye, twil.set, 24, dark);

            // twilight zones

            DrawTimeBox(g, gp, ys, ye, twil.rise, sun.rise, twilight);
            DrawTimeBox(g, gp, ys, ye, sun.set, twil.set, twilight);

            // daytime zone

            DrawTimeBox(g, gp, ys, ye, sun.rise, sun.set, day);
        }
    }

    int DrawSunData(Canvas g, Paint gp, int ypos, String aLbl, String bLbl, int type) {
        int xpos = gstartx + scalex / 96;
        //dc->SetTextAlign(TA_LEFT);
        String rise, transit = "", set;
        if (type == 0) {
            rise = app.tideComp.formatSunHMS(sunData.rise);
            transit = " Transit " + app.tideComp.formatSunHMS(sunData.transit) + ",";
            set = app.tideComp.formatSunHMS(sunData.set);
        } else {
            rise = app.tideComp.formatSunHMS(twilightData.rise);
            set = app.tideComp.formatSunHMS(twilightData.set);
        }
        String text = aLbl
                + " " + rise
                + " " + transit
                + " " + bLbl
                + " " + set;
        //text.Format("%s %s,%s %s %s",aLbl,rise,transit,bLbl,set);
        ypos = showText(g, gp, xpos, ypos, text, true);
        return ypos;
    }

    // graph display for sizes 1 - 7 inclusive

    void drawGraph(Canvas g, Paint gp, boolean include_overlays) {

        if ((app.configValues.sunText) || (app.configValues.sunGraphic)) {
            long t = app.tideComp.setDT(app.chartCal.getTime().getTime() / 1000);
            CompSunData(t, 0);
            CompSunData(t, 1);
        }

        if (app.configValues.sunGraphic) {
            DrawSunLines(g, gp);
        }


        makeGrid(g, gp, false);

        drawTideCurve(g, gp, include_overlays);


        int ypos = gstarty + scaley / 64;

        // draw position line

        if (app.configValues.timeLine && include_overlays) {
            drawCurrent(g, gp);
        }

        if (app.configValues.siteLabel) {
            ypos = DrawSitePosText(g, gp, ypos);
        }

        if (app.configValues.dateText) {
            String date = formatTitleDate(app.chartCal);
            int xpos = gstartx + scalex / 96;
            ypos = showText(g, gp, xpos, ypos, date, false);
        }

        if (app.configValues.sunText) {
            ypos = DrawSunData(g, gp, ypos, "Sunrise", "Sunset", 0);
            ypos = DrawSunData(g, gp, ypos, "Twilight Begins", "Ends", 1);
        }


        if (app.configValues.tideList) {
            long t = app.chartCal.getTime().getTime() / 1000;
            long endt = t + (endx - startx) * 3600;
            long prevt = app.tideComp.getNextEventTime(app.tideComp.siteSet, t, false); // previous
            long postt = app.tideComp.getNextEventTime(app.tideComp.siteSet, endt, true); // next
            ArrayList eventList = app.tideComp.predictTideEvents(app.tideComp.siteSet, prevt, postt, t, endt, null);
            drawTideEventList(g, gp, ypos, eventList, include_overlays);
        }


        //isPrinting = false;
    }

    // data = TideEvent
    int drawTideEventList(Canvas g, Paint gp, int ypos, ArrayList data, boolean include_overlays) {
        if (graphWidth <= 4) {
            int xpos = gstartx + scalex / 96;
            //dc->SetTextAlign(TA_LEFT);
            //long a, b;
            //a = app.chartCal.getTime().getTime() / 1000;
            String text;
            //b = a + (endx - startx) * 3600;
            boolean timeOnly = ((graphWidth == 1));
            for (int i = 0; i < data.size(); i++) {
                if (ypos > scaley) {
                    ypos = showText(g, gp, xpos, ypos, " ... ", true);
                    break;
                } else {
                    text = app.tideComp.formatDataString(i, data, false, timeOnly, "", false, false, true, "");
                    if (text.length() > 0) {
                        ypos = showText(g, gp, xpos, ypos, text, true);
                    }
                }
            }
        }
        return ypos;
    }


    protected String formatTitleDate(GregorianCalendar cal) {
        return TideConstants.dowNames[cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY] + " "
                + app.tideComp.padChar(cal.get(Calendar.MONTH) + 1, 2, "0") + "."
                + app.tideComp.padChar(cal.get(Calendar.DAY_OF_MONTH), 2, "0") + "."
                + cal.get(Calendar.YEAR);
        //return s;
    }

    public void navHandler(int value, int index) {
        // is user returning to current time?
        if (value == 0) {
            activity.most_recent_interaction = -1;
        } else {
            activity.most_recent_interaction = activity.getCurrentTime();
        }
        switch (value) {
            case -3:
                getChartCalendar().add(Calendar.YEAR, -1);
                break;
            case -2:
                getChartCalendar().add(Calendar.MONTH, -1);
                break;
            case -1:
                getChartCalendar().add(Calendar.DAY_OF_MONTH, -1);
                break;
            case 0:
                setChartCalendar();
                break;
            case 1:
                getChartCalendar().add(Calendar.DAY_OF_MONTH, 1);
                break;
            case 2:
                getChartCalendar().add(Calendar.MONTH, 1);
                break;
            case 3:
                getChartCalendar().add(Calendar.YEAR, 1);
                break;
        }
        newDisplay(index);
    }

    public void newDisplay(final int index) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
                activity.execDrawChart(-1, index, false);
            }
        }, 100);
    }


}
