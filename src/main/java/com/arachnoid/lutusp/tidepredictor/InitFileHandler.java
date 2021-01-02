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

import java.lang.reflect.Field;

/**
 * Created by lutusp on 9/15/17.
 */

final public class InitFileHandler {
    String path;

    /**
     * Creates new InitFileHandler
     */
    public InitFileHandler(String p) {
        path = p;
    }

    static public String decodeField(Field f, Object obj) {
        String name = f.getName();
        String type = f.getType().toString();
        String value = "";
        try {
            if (type.equals("int")) {
                value = "" + f.getInt(obj);
            } else if (type.equals("long")) {
                value = "" + f.getLong(obj);
            } else if (type.equals("double")) {
                value = "" + f.getDouble(obj);
            } else if (type.equals("boolean")) {
                value = "" + f.getBoolean(obj);
            } else if (type.indexOf("String") != -1) {
                value = f.get(obj).toString();
            }
            if(value.length() == 0) {
                value = null;
            }
        } catch (Exception e) {
            Log.e("Error", "InitFileHandler:decodeField: " + e.toString());
        }
        return String.format("%s = %s\n",name,value);
    }

    static public void encodeField(Object v,String s) {
        Class cv = v.getClass();
        String[] fields = s.split(" = ");
        if(fields.length == 2) {
            String name = fields[0];
            String value = fields[1];
            try {
                Field a = cv.getField(name);
                String type = a.getType().toString();
                if (type.equals("int")) {
                    a.setInt(v, Integer.parseInt(value));
                } else if (type.equals("long")) {
                    a.setLong(v, Long.parseLong(value));
                } else if (type.equals("double")) {
                    a.setDouble(v, Double.parseDouble(value));
                } else if (type.equals("boolean")) {
                    a.setBoolean(v, value.equals("true"));
                } else if (type.indexOf("String") != -1) {
                    a.set(v, value);
                }
            }
            catch(Exception e) {
                Log.e("Error","InitFileHandler.encodeField: " + e.toString());
            }
        }
    }


}
