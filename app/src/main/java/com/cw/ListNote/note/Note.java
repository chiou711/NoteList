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

import com.cw.ListNote.note_edit.Note_edit;
import com.cw.ListNote.R;
import com.cw.ListNote.db.DB_folder;
import com.cw.ListNote.db.DB_page;
import com.cw.ListNote.main.MainAct;
import com.cw.ListNote.page.PageAdapter_recycler;
import com.cw.ListNote.tabs.TabsHost;
import com.cw.ListNote.util.CustomWebView;
import com.cw.ListNote.util.DeleteFileAlarmReceiver;
import com.cw.ListNote.util.preferences.Pref;
import com.cw.ListNote.operation.mail.MailNotes;
import com.cw.ListNote.util.Util;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class Note extends AppCompatActivity
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public ViewPager viewPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    static int mStyle;
    
    static SharedPreferences mPref_show_note_attribute;

    Button editButton;
    Button optionButton;
    Button backButton;

	public static String mAudioUriInDB;

    public AppCompatActivity act;
    public static int mPlayVideoPositionOfInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

		// set current selection
		mEntryPosition = getIntent().getExtras().getInt("POSITION");
		NoteUi.setFocus_notePos(mEntryPosition);

		act = this;

        MainAct.mMediaBrowserCompat = null;

	} //onCreate end

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("Note / _onWindowFocusChanged");
	}

	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");

		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		// DB
		DB_folder dbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));
		mStyle = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());

		// Instantiate a ViewPager and a PagerAdapter.
		viewPager = (ViewPager) findViewById(R.id.tabs_pager);
		mPagerAdapter = new Note_adapter(viewPager,this);
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setCurrentItem(NoteUi.getFocus_notePos());

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
		}

		// Note: if viewPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		viewPager.setOnPageChangeListener(onPageChangeListener);//todo deprecated

		// edit note button
		editButton = (Button) findViewById(R.id.view_edit);
		editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		editButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Note.this, Note_edit.class);
				intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
				intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_BODY, mDb_page.getNoteBody_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_CREATED, mDb_page.getNoteCreatedTime_byId(mNoteId));
				startActivityForResult(intent, EDIT_CURRENT_VIEW);
			}
		});

		// send note button
		optionButton = (Button) findViewById(R.id.view_option);
		optionButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_more, 0, 0, 0);
		optionButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				View_note_option.note_option(act,mNoteId);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				if(isTextMode())
				{
					// back to view all mode
					setViewAllMode();
					setOutline(act);
				}
				else //view all mode
				{
					finish();
				}
			}
		});
	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{

			NoteUi.setFocus_notePos(viewPager.getCurrentItem());
			System.out.println("Note / _onPageSelected");
//			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
//			System.out.println("    nextPosition = " + nextPosition);

			mIsViewModeChanged = false;

			// show audio name
			mNoteId = mDb_page.getNoteId(nextPosition,true);
			System.out.println("Note / _onPageSelected / mNoteId = " + mNoteId);
			System.out.println("Note / _onPageSelected / mAudioUriInDB = " + mAudioUriInDB);

            setOutline(act);
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode,resultCode,data);
		System.out.println("Note / _onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) || (requestCode==MAIL_CURRENT_VIEW))
        {
        }
		else if(requestCode == MailNotes.EMAIL)
		{
			Toast.makeText(act,R.string.mail_exit,Toast.LENGTH_SHORT).show();
			// note: result code is always 0 (cancel), so it is not used
			new DeleteFileAlarmReceiver(act,
					                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					                    MailNotes.mAttachmentFileName);
		}

	    // check if there is one note at least in the pager
		if( viewPager.getAdapter().getCount() > 0 )
			setOutline(act);
		else
			finish();
	}

    /** Set outline for selected view mode
    *
    *   Determined by view mode: all, text
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public static void setOutline(AppCompatActivity act)
	{
        // Set full screen or not, and action bar
		if(isViewAllMode() || isTextMode())
		{
			Util.setFullScreen_noImmersive(act);
            if(act.getSupportActionBar() != null)
			    act.getSupportActionBar().show();
		}

        // renew pager
        showSelectedView();

		LinearLayout buttonGroup = (LinearLayout) act.findViewById(R.id.view_button_group);
        // button group
        buttonGroup.setVisibility(View.VISIBLE);

        // renew options menu
        act.invalidateOptionsMenu();
	}


    //Refer to http://stackoverflow.com/questions/4434027/android-videoview-orientation-change-with-buffered-video
	/***************************************************************
	video play spec of Pause and Rotate:
	1. Rotate: keep pause state
	 pause -> rotate -> pause -> play -> continue

	2. Rotate: keep play state
	 play -> rotate -> continue play

	3. Key guard: enable pause
	 play -> key guard on/off -> pause -> play -> continue

	4. Key guard and Rotate: keep pause
	 play -> key guard on/off -> pause -> rotate -> pause
	 ****************************************************************/	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note / _onConfigurationChanged");

		// dismiss popup menu
		if(NoteUi.popup != null)
		{
			NoteUi.popup.dismiss();
			NoteUi.popup = null;
		}

        setLayoutView();

        // Set outline of view mode
        setOutline(act);
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note / _onStart");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note / _onResume");

		setLayoutView();

		isPagerActive = true;

		setOutline(act);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note / _onStop");
	}
	
	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("Note / _finish");
		if(mPagerHandler != null)
			mPagerHandler.removeCallbacks(mOnBackPressedRun);		
	    
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
//	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
	    view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
//		System.out.println("Note / _onCreateOptionsMenu");

		// inflate menu
		getMenuInflater().inflate(R.menu.pager_menu, menu);
		mMenu = menu;

		// menu item: checked status
		// get checked or not
		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
		if( isChecked == 0)
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
		else
			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);

	    // menu item: previous
		MenuItem itemPrev = menu.findItem(R.id.ACTION_PREVIOUS);
		itemPrev.setEnabled(viewPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(viewPager.getCurrentItem() > 0?255:30);

		// menu item: Next or Finish
		MenuItem itemNext = menu.findItem(R.id.ACTION_NEXT);
		itemNext.setTitle((viewPager.getCurrentItem() == mPagerAdapter.getCount() - 1)	?
									R.string.view_note_slide_action_finish :
									R.string.view_note_slide_action_next                  );

        // set Disable and Gray for Last item
		boolean isLastOne = (viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1));
        if(isLastOne)
        	itemNext.setEnabled(false);

        itemNext.getIcon().setAlpha(isLastOne?30:255);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(isTextMode())
            	{
        			// back to view all mode
            		setViewAllMode();
					setOutline(act);
            	}
            	else if(isViewAllMode())
            	{
	            	finish();
            	}
                return true;

			case R.id.VIEW_NOTE_CHECK:
				int markingNow = PageAdapter_recycler.toggleNoteMarking(this,NoteUi.getFocus_notePos());

				// update marking
				if(markingNow == 1)
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
				else
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);

				return true;

            case R.id.ACTION_PREVIOUS:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()-1);
            	viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                return true;

            case R.id.ACTION_NEXT:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
				NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()+1);
            	viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
    	if(isTextMode())
    	{
			// back to view all mode
    		setViewAllMode();
			setOutline(act);
    	}
    	else
    	{
    		System.out.println("Note / _onBackPressed / view all mode");
        	finish();
    	}
    }
    
    static Handler mPagerHandler;
	Runnable mOnBackPressedRun = new Runnable()
	{   @Override
		public void run()
		{
			if(Note_adapter.mIntentView != null)
				Note_adapter.mIntentView = null;
		}
	};

    // Show selected view
    static void showSelectedView()
    {
   		mIsViewModeChanged = false;

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_adapter / _setPrimaryItem
    }
    
    public static boolean mIsViewModeChanged;
    
    static void setViewAllMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","ALL")
		   						  .apply();
    }
    
    public static boolean isViewAllMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("ALL");
    }

    public static boolean isTextMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("TEXT_ONLY");
    }

}
