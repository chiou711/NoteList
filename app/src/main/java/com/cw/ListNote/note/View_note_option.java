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

package com.cw.ListNote.note;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.util.Util;
import com.cw.ListNote.util.preferences.Pref;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by cw on 2017/10/7.
 */
public class View_note_option {
    int option_id;
    int option_drawable_id;
    int option_string_id;

    View_note_option(int id, int draw_id, int string_id)
    {
        this.option_id = id;
        this.option_drawable_id = draw_id;
        this.option_string_id = string_id;
    }

    /**
     *
     * 	Add new note
     *
     */
    static List<View_note_option> option_list;

    private final static int ID_OPTION_NOTE_TITLE_FONT_SIZE_DOWN = 9;
    private final static int ID_OPTION_NOTE_TITLE_FONT_SIZE_UP = 10;
    private final static int ID_OPTION_NOTE_BODY_FONT_SIZE_DOWN = 11;
    private final static int ID_OPTION_NOTE_BODY_FONT_SIZE_UP = 12;
    static long noteId;
    static GridIconAdapter mGridIconAdapter;

    public static void note_option(final AppCompatActivity act, long _noteId,DB_page dbPage)
    {
        AbsListView gridView;
        noteId = _noteId;
        // get layout inflater
        View rootView = act.getLayoutInflater().inflate(R.layout.option_grid, null);

        option_list = new ArrayList<>();

        String strTitle = dbPage.getNoteTitle_byId(noteId);

        // title
        if(!Util.isEmptyString(strTitle)) {
            // down size
            option_list.add(new View_note_option(ID_OPTION_NOTE_TITLE_FONT_SIZE_DOWN,
                    R.drawable.rate_star_small_on_holo_dark,
                    R.string.btn_size_down));

            // up size
            option_list.add(new View_note_option(ID_OPTION_NOTE_TITLE_FONT_SIZE_UP,
                    R.drawable.rate_star_big_on_holo_dark,
                    R.string.btn_size_up));
        }

        // body
        String strBody = dbPage.getNoteBody_byId(noteId);
        if(!Util.isEmptyString(strBody)) {
            // down size
            option_list.add(new View_note_option(ID_OPTION_NOTE_BODY_FONT_SIZE_DOWN,
                    R.drawable.ic_media_previous,
                    R.string.btn_size_down));

            // up size
            option_list.add(new View_note_option(ID_OPTION_NOTE_BODY_FONT_SIZE_UP,
                    R.drawable.ic_media_next,
                    R.string.btn_size_up));
        }

        gridView = (GridView) rootView.findViewById(R.id.option_grid_view);

        // check if directory is created AND not empty
        if( (option_list != null  ) && (option_list.size() > 0))
        {
            mGridIconAdapter = new GridIconAdapter(act);
            gridView.setAdapter(mGridIconAdapter);
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("View_note_option / _note_option / _OnItemClickListener / position = " + position +" id = " + id);
                startAddNoteActivity(act, option_list.get(position).option_id);
            }
        });

        // set view to dialog
        AlertDialog.Builder builder1 = new AlertDialog.Builder(act);
        builder1.setView(rootView);
        dlgAddNew = builder1.create();
        dlgAddNew.show();
    }
    private static AlertDialog dlgAddNew;

    private static void startAddNoteActivity(AppCompatActivity act,int optionId)
    {
        System.out.println("View_note_option / _startAddNoteActivity / optionId = " + optionId);

        switch (optionId) {
            case ID_OPTION_NOTE_TITLE_FONT_SIZE_DOWN:
            {
//                dlgAddNew.dismiss();
                int titleFontSize = Pref.getPref_note_title_font_size(act);
                titleFontSize -= 4;
                Pref.setPref_note_title_font_size(act,titleFontSize);
                Note.mPagerAdapter.notifyDataSetChanged();
            }
            break;

            case ID_OPTION_NOTE_TITLE_FONT_SIZE_UP:
            {
                int titleFontSize = Pref.getPref_note_title_font_size(act);
                titleFontSize += 4;
                Pref.setPref_note_title_font_size(act,titleFontSize);
                Note.mPagerAdapter.notifyDataSetChanged();
            }
            break;

            case ID_OPTION_NOTE_BODY_FONT_SIZE_DOWN:
            {
                int bodyFontSize = Pref.getPref_note_body_font_size(act);
                bodyFontSize -= 4;
                Pref.setPref_note_body_font_size(act,bodyFontSize);
                Note.mPagerAdapter.notifyDataSetChanged();
            }
            break;

            case ID_OPTION_NOTE_BODY_FONT_SIZE_UP:
            {
                int bodyFontSize = Pref.getPref_note_body_font_size(act);
                bodyFontSize += 4;
                Pref.setPref_note_body_font_size(act,bodyFontSize);
                Note.mPagerAdapter.notifyDataSetChanged();
            }
            break;

            // default
            default:
                break;
        }

    }


    /**
     * Created by cw on 2017/10/7.
     */
    static class GridIconAdapter extends BaseAdapter {
        private AppCompatActivity act;
        GridIconAdapter(AppCompatActivity fragAct){act = fragAct;}

        @Override
        public int getCount() {
            return option_list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = act.getLayoutInflater().inflate(R.layout.add_note_grid_item, parent, false);
                holder = new ViewHolder();
                assert view != null;
                holder.imageView = (ImageView) view.findViewById(R.id.grid_item_image);
                holder.text = (TextView) view.findViewById(R.id.grid_item_text);
                holder.text.setTextColor(Color.WHITE);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Drawable drawable = act.getResources().getDrawable(option_list.get(position).option_drawable_id);
            holder.imageView.setImageDrawable(drawable);
            holder.text.setText(option_list.get(position).option_string_id);
            return view;
        }

        private class ViewHolder {
            ImageView imageView;
            TextView text;
        }
    }
}
