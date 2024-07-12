/*
 * Copyright (C) 2024 CW Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.ListNote.define;

import android.content.Context;

import com.cw.ListNote.R;

public class Define {

    //
    public final static int DEBUG_DEFAULT_BY_INITIAL = 0;
    public final static int RELEASE_DEFAULT_BY_INITIAL = 1;
    public final static String  DB_FILE_NAME = "listnote.db";

    public static void setAppBuildMode(){
        /** 1 debug, initial */
        int mode = DEBUG_DEFAULT_BY_INITIAL;

        /** 2 release, initial */
//        int mode  =  Define.RELEASE_DEFAULT_BY_INITIAL;

        setAppBuildMode(mode);
    }

    private static void setAppBuildMode(int appBuildMode) {
        app_build_mode = appBuildMode;

        switch (appBuildMode)
        {
            case DEBUG_DEFAULT_BY_INITIAL:
                CODE_MODE = DEBUG_MODE;
                DEFAULT_CONTENT = BY_INITIAL_TABLES;
                INITIAL_FOLDERS_COUNT = 2;  // Folder1, Folder2
                INITIAL_PAGES_COUNT = 1;// Page1_1
                break;

            case RELEASE_DEFAULT_BY_INITIAL:
                CODE_MODE = RELEASE_MODE;
                DEFAULT_CONTENT = BY_INITIAL_TABLES;
                INITIAL_FOLDERS_COUNT = 2;  // Folder1, Folder2
                INITIAL_PAGES_COUNT = 1;// Page1_1
                break;

            default:
                break;
        }
    }

    public static int app_build_mode = 0;

    /***************************************************************************
     * Set release/debug mode
     * - RELEASE_MODE
     * - DEBUG_MODE
     ***************************************************************************/
    public static int CODE_MODE;// = DEBUG_MODE; //DEBUG_MODE; //RELEASE_MODE;
    public static int DEBUG_MODE = 0;
    public static int RELEASE_MODE = 1;


    /***
     *  With default content by XML file
     */
    public static int DEFAULT_CONTENT;
    // by none
    public static int BY_INITIAL_TABLES = 0;
    // by assets XML file

    /**
     * With initial tables: table count
     * - folder count: 2
     * - page count: 1
     */
    // initial table count
    public static int INITIAL_FOLDERS_COUNT;
    public static int INITIAL_PAGES_COUNT;

    // default style
    public static int STYLE_DEFAULT = 1;

    public static String getTabTitle(Context context, Integer Id)
    {
        String title;

        if(Define.DEFAULT_CONTENT != Define.BY_INITIAL_TABLES) {
            title = context.getResources().getString(R.string.prefer_page_name).concat(String.valueOf(Id));
        }
        else {
            title = context.getResources().getString(R.string.default_page_name).concat(String.valueOf(Id));
        }
        return title;
    }

    // note view setting
//    public static boolean NOTE_WEB_VIEW = true;
    public static boolean NOTE_WEB_VIEW = false;

    // page view setting
//    public static boolean PAGE_VIEW_SHOW_TIME = true;
    public static boolean PAGE_VIEW_SHOW_TIME = false;
}