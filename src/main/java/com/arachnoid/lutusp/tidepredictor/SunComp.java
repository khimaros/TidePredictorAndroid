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

/**
 * Created by lutusp on 9/15/17.
 */

final public class SunComp {
    long oldTime;
    double oldLat,oldLng;

    public Pos compPos;
    double compSid,compJD;
    TidePredictorApplication app;


    SunComp(TidePredictorApplication m)
    {
        app = m;
        oldTime = -1;
        oldLat = -1;
        oldLng = -1;
    }

    // mostly imported from "data/cpp/sun.cpp"

    // compute sun's astronomical position
    // jd = julian date/time

    // convert a GMT time (hours) from MS-DOS into jd

    double tf_to_jd(double tf)
    {
        return tf + 2440587.5;
    }

    // convert a jd to GMT time (hours) in MS-DOS form

    double jd_to_tf(double tf)
    {
        return tf - 2440587.5;
    }

    Pos calcsun(double jd)
    {
        Pos s = new Pos();
        double t;
        double lo;
        double m;
        double rm;
        double e;
        double c;
        double tl;
        double v;
        double r;
        double nut;
        double al;
        double obliq;
        double sunra;
        double sundecl;
        t = (jd - 2451545) * 2.7378507871321E-05;
        lo = 280.46645 + (36000.76983 * t) + (0.0003032 * t * t);
        m = 357.5291 + (35999.0503 * t) - (0.0001559 * t * t) - (0.00000048 * t * t * t);
        rm = m * TideConstants.RADIANS;
        e = 0.016708617 - (0.000042037 * t) - (0.0000001236 * t * t);
        c = (1.9146 - 0.004817 * t - 0.000014 * t * t) * Math.sin(rm);
        c = c + (0.019993 - 0.000101 * t) * Math.sin(2 * rm);
        c = c + 0.00029 * Math.sin(3 * rm);
        tl = lo + c;
        v = m + c;
        r = (1.000001018 * (1 - e * e)) / (1 + (e * Math.cos(v * TideConstants.RADIANS)));
        nut = 125.04 - 1934.136 * t;
        al = TideConstants.RADIANS * (tl - 0.00569 - 0.00478 * Math.sin(TideConstants.RADIANS * nut));
        obliq = 23.4391666666667 - 1.30041666666666E-02 * t - 0.000000163888888 * t * t + 5.03611111111E-08 * t * t * t;
        obliq = TideConstants.RADIANS * (obliq + 0.00256 * Math.cos(TideConstants.RADIANS * nut));
        sunra = TideConstants.DEGREES * (Math.atan2(Math.cos(obliq) * Math.sin(al),Math.cos(al)));
        if (sunra < 0)
            sunra = 360 + sunra;
        sundecl = TideConstants.DEGREES * (Math.asin(Math.sin(obliq) * Math.sin(al)));
        s.lat = sundecl;
        s.lng = sunra;
        return(s);
    }

    // sidereal time from Julian Date

    double sidtime(double jd, double lng)
    {
        double t,x;
        t = ((Math.floor(jd) + 0.5) - 2451545) *2.73785078713210E-05;

        x = 280.46061837;
        x = x + 360.98564736629 * (jd - 2451545);

        x = x + 0.000387933 * t * t;
        x = x - (t * t * t) * 2.583311805734950E-08;
        x = x - lng;
        x = x * TideConstants.convcircle;
        x = x - Math.floor(x);
        x = x * 360;
        return(x);
    }

    double mod1(double t)
    {
        double ot;
        ot = t;
        t = Math.abs(t);
        t = t - Math.floor(t);
        if(ot < 0)
            t = 1-t;
        return(t);
    }

    Rts rmsTime(double gmtsid, double lat, double lng, double tz, double atcr, double ra, double decl)
    {
        double x;
        Rts result = new Rts();
        tz *= TideConstants.convday; // must be 0 < tz < 1

        result.transit = ((ra - lng - gmtsid) / 360);
        atcr = TideConstants.RADIANS * atcr;
        lat = TideConstants.RADIANS * lat;
        decl = TideConstants.RADIANS * decl;
        x = Math.sin(atcr) - (Math.sin(lat) * Math.sin(decl));
        x = x / (Math.cos(lat) * Math.cos(decl));
        if ((x < 1) && (x > -1))
        {
            x = TideConstants.DEGREES * (Math.acos(x)) * TideConstants.convcircle;
            result.rise = mod1((result.transit - x) + tz) * 24.0;
            result.set = mod1((result.transit + x) + tz) * 24.0;
            result.transit = mod1((result.transit) + tz) * 24.0; // must be last!
        }
        else // mark no event
        {
            x = (x >= 0)?-100:100;
            result.rise = x;
            result.transit = x;
            result.set = x;
        }
        return result;
    }

    // provide a time local to the site in seconds

    Rts compRTS(SiteSet data,long t, int type)
    {
        t += data.tz * 3600.0; // move to local time

        t -= t % 86400; // round off to nearest day in local time

        t -= data.tz * 3600.0; // back to GMT

        double types[] = {
                TideConstants.SUNSET
                ,TideConstants.CIVIL
                ,TideConstants.NAUTICAL
                ,TideConstants.ASTRONOMICAL};
        Rts result;
        if((t != oldTime) || (data.lat != oldLat) || (data.lng != oldLng)) {
            oldLat = data.lat;
            oldLng = data.lng;
            oldTime = t;
            double jd = t / 3600.0; // time local to site, in hours
            jd /= 24.0; // now days
            jd = tf_to_jd(jd); // now Julian date
            compSid = sidtime(Math.floor(jd) + 0.5, 0);
            compJD = jd;
            compPos = calcsun(jd); // use GMT
        }
        double typ = types[type];
        result = rmsTime(compSid,data.lat,data.lng,data.tz,typ,compPos.lng,compPos.lat);
        if(data.daylightInEffect) {
            result.rise += 1.0;
            result.transit += 1.0;
            result.set += 1.0;
        }
        return result;
    }
}
