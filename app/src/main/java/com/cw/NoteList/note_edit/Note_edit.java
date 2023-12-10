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

package com.cw.NoteList.note_edit;

import com.cw.NoteList.R;
import com.cw.NoteList.db.DB_page;
import com.cw.NoteList.tabs.TabsHost;
import com.cw.NoteList.util.ColorSet;
import com.cw.NoteList.util.Util;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_edit extends Activity 
{

    private Long noteId, createdTime;
    private String title, picUriStr, drawingUri, audioUri, linkUri, cameraPictureUri, body;
    Note_edit_ui note_edit_ui;
    private boolean enSaveDb = true;
    boolean bUseCameraImage;
    DB_page dB;
    int position;
    final int EDIT_LINK = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        // check note count first
	    dB = new DB_page(this, TabsHost.getCurrentPageTableId());

        if(dB.getNotesCount(true) ==  0)
        {
        	finish(); // add for last note being deleted
        	return;
        }
        
        setContentView(R.layout.note_edit);
        setTitle(R.string.edit_note_title);// set title
    	
        System.out.println("Note_edit / onCreate");
        

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setBackgroundDrawable(new ColorDrawable(ColorSet.getBarColor(this)));

    	Bundle extras = getIntent().getExtras();
    	position = extras.getInt("list_view_position");
    	noteId = extras.getLong(DB_page.KEY_NOTE_ID);
		picUriStr = extras.getString(DB_page.KEY_NOTE_PICTURE_URI);
		drawingUri = extras.getString(DB_page.KEY_NOTE_DRAWING_URI);
    	audioUri = extras.getString(DB_page.KEY_NOTE_AUDIO_URI);
    	linkUri = extras.getString(DB_page.KEY_NOTE_LINK_URI);
    	title = extras.getString(DB_page.KEY_NOTE_TITLE);
    	body = extras.getString(DB_page.KEY_NOTE_BODY);
    	createdTime = extras.getLong(DB_page.KEY_NOTE_CREATED);
        

        //initialization
        note_edit_ui = new Note_edit_ui(this, dB, noteId, title, picUriStr, audioUri, drawingUri, linkUri, body, createdTime);
        note_edit_ui.UI_init();
        cameraPictureUri = "";
        bUseCameraImage = false;

        if(savedInstanceState != null)
        {
	        System.out.println("Note_edit / onCreate / noteId =  " + noteId);
	        if(noteId != null)
	        {
	        	picUriStr = dB.getNotePictureUri_byId(noteId);
				note_edit_ui.currPictureUri = picUriStr;
	        	audioUri = dB.getNoteAudioUri_byId(noteId);
				note_edit_ui.currAudioUri = audioUri;
				drawingUri = dB.getNoteDrawingUri_byId(noteId);
	        }
        }
        
    	// show view
		note_edit_ui.populateFields_all(noteId);
		
		// OK button: edit OK, save
        Button okButton = (Button) findViewById(R.id.note_edit_ok);
//        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
				if(note_edit_ui.bRemovePictureUri)
				{
					picUriStr = "";
				}
				if(note_edit_ui.bRemoveAudioUri)
				{
					audioUri = "";
				}	
				System.out.println("Note_edit / onClick (okButton) / noteId = " + noteId);
                enSaveDb = true;
                finish();
            }

        });
        
        // delete button: delete note
        Button delButton = (Button) findViewById(R.id.note_edit_delete);
//        delButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        // delete
        delButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view)
			{
				Util util = new Util(Note_edit.this);
				util.vibrate();

				Builder builder1 = new Builder(Note_edit.this );
				builder1.setTitle(R.string.confirm_dialog_title)
					.setMessage(R.string.confirm_dialog_message)
					.setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener()
						{   @Override
							public void onClick(DialogInterface dialog1, int which1)
							{/*nothing to do*/}
						})
					.setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener()
						{   @Override
							public void onClick(DialogInterface dialog1, int which1)
							{
								note_edit_ui.deleteNote(noteId);
								finish();
							}
						})
					.show();//warning:end
            }
        });
        
        // cancel button: leave, do not save current modification
        Button cancelButton = (Button) findViewById(R.id.note_edit_cancel);
//        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                
                // check if note content is modified
               	if(note_edit_ui.isNoteModified())
            	{
               		// show confirmation dialog
            		confirmToUpdateDlg();
            	}
            	else
            	{
            		enSaveDb = false;
                    finish();
            	}
            }
        });
    }
    
    // confirm to update change or not
    void confirmToUpdateDlg()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(Note_edit.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.edit_note_confirm_update)
	           // Yes, to update
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						if(note_edit_ui.bRemovePictureUri)
						{
							picUriStr = "";
						}
						if(note_edit_ui.bRemoveAudioUri)
						{
							audioUri = "";
						}						
					    enSaveDb = true;
					    finish();
					}})
			   // cancel
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})
			   // no, roll back to original status		
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Bundle extras = getIntent().getExtras();

	                    finish();
					}})
			   .show();
    }
    

    // for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
        
        System.out.println("Note_edit / onPause / enSaveDb = " + enSaveDb);
        noteId = note_edit_ui.saveStateInDB(noteId, enSaveDb, picUriStr, audioUri, drawingUri);
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        System.out.println("Note_edit / onSaveInstanceState / enSaveDb = " + enSaveDb);
        System.out.println("Note_edit / onSaveInstanceState / bUseCameraImage = " + bUseCameraImage);
        System.out.println("Note_edit / onSaveInstanceState / cameraPictureUri = " + cameraPictureUri);
        
        noteId = note_edit_ui.saveStateInDB(noteId, enSaveDb, picUriStr, audioUri, drawingUri);
        outState.putSerializable(DB_page.KEY_NOTE_ID, noteId);
        
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onBackPressed() {
	    {
	    	if(note_edit_ui.isNoteModified())
	    	{
	    		confirmToUpdateDlg();
	    	}
	    	else
	    	{
	            enSaveDb = false;
	            finish();
	    	}
	    }
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// inflate menu
		getMenuInflater().inflate(R.menu.edit_note_menu, menu);

//	    menu.add(0, CHANGE_LINK, 0, R.string.edit_note_link )
//	    .setIcon(android.R.drawable.ic_menu_share)
//	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//
//	    menu.add(0, CHANGE_AUDIO, 1, R.string.audioUi_note )
//	    .setIcon(R.drawable.ic_audio_unselected)
//	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//
//	    menu.add(0, CAPTURE_IMAGE, 2, R.string.note_camera_image )
//	    .setIcon(android.R.drawable.ic_menu_camera)
//	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//
//	    menu.add(0, CAPTURE_VIDEO, 3, R.string.note_camera_video )
//	    .setIcon(android.R.drawable.presence_video_online)
//	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
        {
		    case android.R.id.home:
		    	if(note_edit_ui.isNoteModified())
		    	{
		    		confirmToUpdateDlg();
		    	}
		    	else
		    	{
		            enSaveDb = false;
		            finish();
		    	}
		        return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    void setAudioSource() 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_note_set_audio_dlg_title);
		// Cancel
		builder.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
		   	   {
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{// cancel
				}});
		// Set
		builder.setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
		    enSaveDb = true;
	        startActivityForResult(Util.chooseMediaIntentByType(Note_edit.this,"audio/*"),
	        					   Util.CHOOSER_SET_AUDIO);
		}});

		Dialog dialog = builder.create();
		dialog.show();
    }
    
    void setLinkUri() 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_note_dlg_set_link);
		
		// select Web link
		builder.setNegativeButton(R.string.note_web_link, new DialogInterface.OnClickListener()
   	   {
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
	    		Intent intent_web_link = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.google.com"));
	    		startActivityForResult(intent_web_link,EDIT_LINK);	
	    		enSaveDb = false;
			}
		});
		
		// select YouTube link
		builder.setNeutralButton(R.string.note_youtube_link, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
	        	Intent intent_youtube_link = new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com"));
	        	startActivityForResult(intent_youtube_link,EDIT_LINK);
	        	enSaveDb = false;
			}
		});
		// None
		if(!Util.isEmptyString(linkUri))
		{
			builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					note_edit_ui.populateFields_all(noteId);
				}
			});		
		}
		
		Dialog dialog = builder.create();
		dialog.show();
    }

}
