/*
 * Copyright (C) 2019 CW Chiu
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

package com.cw.ListNote.util.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by cw on 2017/10/11.
 */

public class Pref
{
    // set folder table id of focus view
    public static void setPref_focusView_folder_tableId(Activity act, int folderTableId )
    {
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_FOCUS_VIEW_FOLDER_TABLE_ID";
        pref.edit().putInt(keyName, folderTableId).apply();
    }

    // get folder table id of focus view
    public static int getPref_focusView_folder_tableId(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_FOCUS_VIEW_FOLDER_TABLE_ID";
        return pref.getInt(keyName, 1); // folder table Id: default is 1
    }

    // remove key of focus view for folder
    public static void removePref_focusView_folder_tableId_key(Activity act)
    {
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_FOCUS_VIEW_FOLDER_TABLE_ID";
        pref.edit().remove(keyName).apply();
    }

    // set page table id of focus view
    public static void setPref_focusView_page_tableId(Activity act, int pageTableId )
    {
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyPrefix = "KEY_FOCUS_VIEW_PAGE_TABLE_ID_";
        int folderTableId = getPref_focusView_folder_tableId(act);
        String keyName = keyPrefix.concat(String.valueOf(folderTableId));
        pref.edit().putInt(keyName, pageTableId).apply();
    }

    // get page table id of focus view
    public static int getPref_focusView_page_tableId(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("focus_view", 0);
        String keyPrefix = "KEY_FOCUS_VIEW_PAGE_TABLE_ID_";
        int folderTableId = getPref_focusView_folder_tableId(context);
        String keyName = keyPrefix.concat(String.valueOf(folderTableId));
        // page table Id: default is 1
        return pref.getInt(keyName, 1);
    }

    // remove key of focus view for page table Id
    public static void removePref_focusView_page_tableId_key(Activity act, int folderTableId)
    {
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyPrefix = "KEY_FOCUS_VIEW_PAGE_TABLE_ID_";
        String keyName = keyPrefix.concat(String.valueOf(folderTableId));
        pref.edit().remove(keyName).commit();
    }

    // Set list view first visible Index of focus view
    public static void setPref_focusView_list_view_first_visible_index(Activity act, int index )
    {
//		System.out.println("Pref / _setPref_focusView_list_view_first_visible_index / index = " + index);
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX";
        String location = getCurrentListViewLocation(act);
        keyName = keyName.concat(location);
        pref.edit().putInt(keyName, index).apply();
    }

    // Get list view first visible Index of focus view
    public static Integer getPref_focusView_list_view_first_visible_index(Activity act)
    {
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX";
        String location = getCurrentListViewLocation(act);
        keyName = keyName.concat(location);
        return pref.getInt(keyName, 0); // default scroll X is 0
    }

    // Set list view first visible index Top of focus view
    public static void setPref_focusView_list_view_first_visible_index_top(Activity act, int top )
    {
//        System.out.println("Pref / _setPref_focusView_list_view_first_visible_index_top / top = " + top);
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX_TOP";
        String location = getCurrentListViewLocation(act);
        keyName = keyName.concat(location);
        pref.edit().putInt(keyName, top).apply();
    }

    // Get list view first visible index Top of focus view
    public static Integer getPref_focusView_list_view_first_visible_index_top(Activity act)
    {
        SharedPreferences pref = act.getSharedPreferences("focus_view", 0);
        String keyName = "KEY_LIST_VIEW_FIRST_VISIBLE_INDEX_TOP";
        String location = getCurrentListViewLocation(act);
        keyName = keyName.concat(location);
        return pref.getInt(keyName, 0);
    }

    // set key: has answered if default content needed dialog
    public static void setPref_has_answered_if_default_content_needed(Activity act, boolean will)
    {
        SharedPreferences pref = act.getSharedPreferences("create_view", 0);
        String keyName = "KEY_HAS_ANSWERED_IF_DEFAULT_CONTENT_NEEDED";
        pref.edit().putBoolean(keyName, will).apply();
    }

    // get key: has answered if default content needed dialog
    public static boolean getPref_has_answered_if_default_content_needed(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("create_view", 0);
        String keyName = "KEY_HAS_ANSWERED_IF_DEFAULT_CONTENT_NEEDED";
        return pref.getBoolean(keyName, false);
    }

    // set key: will create default content
    public static void setPref_will_create_default_content(Activity act, boolean will)
    {
        SharedPreferences pref = act.getSharedPreferences("create_view", 0);
        String keyName = "KEY_WILL_CREATE_DEFAULT_CONTENT";
        pref.edit().putBoolean(keyName, will).apply();

        setPref_has_answered_if_default_content_needed(act,true);
    }

    // get key: will create default content
    public static boolean getPref_will_create_default_content(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("create_view", 0);
        String keyName = "KEY_WILL_CREATE_DEFAULT_CONTENT";
//        return pref.getBoolean(keyName, false);
        return pref.getBoolean(keyName, true);
    }

    // location about drawer table Id and page table Id
    static String getCurrentListViewLocation(Activity act)
    {
        String strLocation = "";
        // folder
        int folderTableId = getPref_focusView_folder_tableId(act);
        String strFolderTableId = String.valueOf(folderTableId);
        // page
        int pageTableId = getPref_focusView_page_tableId(act);
        String strPageTableId = String.valueOf(pageTableId);
        strLocation = "_" + strFolderTableId + "_" + strPageTableId;
        return strLocation;
    }

    // set default title font size
    public static void setPref_title_font_size(Activity act, int fontSize )
    {
        SharedPreferences pref = act.getSharedPreferences("font_size", 0);
        String keyName = "KEY_TITLE_FONT_SIZE";
        pref.edit().putInt(keyName, fontSize).apply();
    }

    // get default title font size
    public static int getPref_title_font_size(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("font_size", 0);
        String keyName = "KEY_TITLE_FONT_SIZE";
        return pref.getInt(keyName, 18); // folder table Id: default is 1
    }

    // set default body font size
    public static void setPref_body_font_size(Activity act, int fontSize )
    {
        SharedPreferences pref = act.getSharedPreferences("font_size", 0);
        String keyName = "KEY_BODY_FONT_SIZE";
        pref.edit().putInt(keyName, fontSize).apply();
    }

    // get default body font size
    public static int getPref_body_font_size(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("font_size", 0);
        String keyName = "KEY_BODY_FONT_SIZE";
        return pref.getInt(keyName, 18); // folder table Id: default is 1
    }

    // set default note title font size
    public static void setPref_note_title_font_size(Activity act, int fontSize )
    {
        SharedPreferences pref = act.getSharedPreferences("note_font_size", 0);
        String keyName = "KEY_NOTE_TITLE_FONT_SIZE";
        pref.edit().putInt(keyName, fontSize).apply();
    }

    // get default note title font size
    public static int getPref_note_title_font_size(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("note_font_size", 0);
        String keyName = "KEY_NOTE_TITLE_FONT_SIZE";
        return pref.getInt(keyName, 18); // folder table Id: default is 1
    }

    // set default note body font size
    public static void setPref_note_body_font_size(Activity act, int fontSize )
    {
        SharedPreferences pref = act.getSharedPreferences("note_font_size", 0);
        String keyName = "KEY_NOTE_BODY_FONT_SIZE";
        pref.edit().putInt(keyName, fontSize).apply();
    }

    // get default note body font size
    public static int getPref_note_body_font_size(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("note_font_size", 0);
        String keyName = "KEY_NOTE_BODY_FONT_SIZE";
        return pref.getInt(keyName, 18); // folder table Id: default is 1
    }
}
