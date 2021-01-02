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
 * Created by lutusp on 9/26/17.
 */

/*
Example of use:

We want to show a dialog and, on acceptance, run a passed function.

Target function: void myFunction(void)

Dialog function: actionDialog(String title,String message, FunctionPointer f)

public void resetToDefaults(View v) {
        String title = "Reset to Defaults";
        String message = "Do you really want to reset all values to their defaults?";
        FunctionPointer f = new FunctionInterface() {
            public void function() {
                myFunction();
            }
        };
        actionDialog(v,title,message,f);
}

public void actionDialog(String title, String message,final FunctionInterface f) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.app_icon)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                f.yes_function();
                            }
                        })
                .setNegativeButton(android.R.string.no, null).show();
    }

*/

public interface FunctionInterface {
    void yes_function();
    void no_function();
}
