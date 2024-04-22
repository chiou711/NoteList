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

package com.cw.ListNote.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.util.Date;


/**
 *  Data Base Class for Page
 *
 */
public class DB_page
{

    private final Context mContext;
    private DatabaseHelper mDbHelper ;
    private SQLiteDatabase mSqlDb;

	// Table name format: Page1_2
	private static final String DB_PAGE_TABLE_PREFIX = "Page";
    private static String DB_PAGE_TABLE_NAME; // Note: name = prefix + id

	// Note rows
    public static final String KEY_NOTE_ID = "_id"; //do not rename _id for using CursorAdapter (BaseColumns._ID)
    public static final String KEY_NOTE_TITLE = "note_title";
    public static final String KEY_NOTE_BODY = "note_body";
    public static final String KEY_NOTE_MARKING = "note_marking";
    public static final String KEY_NOTE_CREATED = "note_created";

	// Cursor
	public Cursor mCursor_note;

	// Table Id
    private static int mTableId_page;

    /** Constructor */
	public DB_page(Context context, int pageTableId)
	{
		mContext = context;
		setFocusPage_tableId(pageTableId);
	}

    /**
     * DB functions
     * 
     */
	public DB_page open() throws SQLException
	{
		mDbHelper = new DatabaseHelper(mContext);

		// Will call DatabaseHelper.onCreate()first time when WritableDatabase is not created yet
		mSqlDb = mDbHelper.getWritableDatabase();

		//try to get note cursor
		try
		{
//			System.out.println("DB_page / _open / open page table Try / table name = " + DB_PAGE_TABLE_NAME);
			mCursor_note = this.getNoteCursor_byPageTableId(getFocusPage_tableId());
		}
		catch(Exception e)
		{
			System.out.println("DB_page / _open / open page table NG! / table name = " + DB_PAGE_TABLE_NAME);
		}//catch

		return DB_page.this;
	}

	public void close()
	{
		if((mCursor_note != null)&& (!mCursor_note.isClosed()))
			mCursor_note.close();

		mDbHelper.close();
	}

    /**
     *  Page table columns for note row
     * 
     */
    private final String[] strNoteColumns = new String[] {
          KEY_NOTE_ID,
          KEY_NOTE_TITLE,
          KEY_NOTE_BODY,
          KEY_NOTE_MARKING,
          KEY_NOTE_CREATED
      };

    // select all notes
    private Cursor getNoteCursor_byPageTableId(int pageTableId) {

        // table number initialization: name = prefix + id
        DB_PAGE_TABLE_NAME = DB_PAGE_TABLE_PREFIX.concat(DB_folder.getFocusFolder_tableId()+
                                                    "_"+
                                                    pageTableId);

        return mSqlDb.query(DB_PAGE_TABLE_NAME,
             strNoteColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }   
    
    //set page table id
    public static void setFocusPage_tableId(int id)
    {
    	mTableId_page = id;
    }
    
    //get page table id
    public static int getFocusPage_tableId()
    {
    	return mTableId_page;
    }
    
    // Insert note
    // createTime: 0 will update time
    public long insertNote(String title,String body, int marking, Long createTime)
    {
    	this.open();

        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_NOTE_TITLE, title);   
        args.put(KEY_NOTE_BODY, body);
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, now.getTime());
        else
        	args.put(KEY_NOTE_CREATED, createTime);
        	
        args.put(KEY_NOTE_MARKING,marking);
        long rowId = mSqlDb.insert(DB_PAGE_TABLE_NAME, null, args);

        this.close();

        return rowId;  
    }  
    
    public void deleteNote(long rowId, boolean enDbOpenClose)
    {
    	if(enDbOpenClose)
    		this.open();

    	mSqlDb.delete(DB_PAGE_TABLE_NAME, KEY_NOTE_ID + "=" + rowId, null);

        if(enDbOpenClose)
        	this.close();

    }
    
    //query note
    public Cursor queryNote(long rowId) throws SQLException 
    {  
        Cursor mCursor = mSqlDb.query(true,
									DB_PAGE_TABLE_NAME,
					                new String[] {KEY_NOTE_ID,
				  								  KEY_NOTE_TITLE,
        										  KEY_NOTE_BODY,
        										  KEY_NOTE_MARKING,
        										  KEY_NOTE_CREATED},
					                KEY_NOTE_ID + "=" + rowId,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }

        return mCursor;
    }

    // update note
    // 		createTime:  0 for Don't update time
    @SuppressLint("Range")
    public boolean updateNote(long rowId, String title, String body, long marking, long createTime, boolean enDbOpenClose)
    {
    	if(enDbOpenClose)
    		this.open();

        ContentValues args = new ContentValues();
        args.put(KEY_NOTE_TITLE, title);
        args.put(KEY_NOTE_BODY, body);
        args.put(KEY_NOTE_MARKING, marking);
        
        Cursor cursor = queryNote(rowId);
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, cursor.getLong(cursor.getColumnIndex(KEY_NOTE_CREATED)));
        else
        	args.put(KEY_NOTE_CREATED, createTime);

        int cUpdateItems = mSqlDb.update(DB_PAGE_TABLE_NAME, args, KEY_NOTE_ID + "=" + rowId, null);

		if(enDbOpenClose)
        	this.close();

		return cUpdateItems > 0;
    }    
    
    
	public int getNotesCount(boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		int count = 0;
		if(mCursor_note != null)
			count = mCursor_note.getCount();

		if(enDbOpenClose)
			this.close();

		return count;
	}	
	
	public int getCheckedNotesCount()
	{
		this.open();

		int countCheck =0;
		int notesCount = getNotesCount(false);
		for(int i=0;i< notesCount ;i++)
		{
			if(getNoteMarking(i,false) == 1)
				countCheck++;
		}

		this.close();

		return countCheck;
	}		
	
	public String getNoteTitle_byId(Long mRowId)
	{
		this.open();

		String title = queryNote(mRowId).getString(queryNote(mRowId)
										.getColumnIndexOrThrow(DB_page.KEY_NOTE_TITLE));

		this.close();

		return title;
	}
	
	public String getNoteBody_byId(Long mRowId)
	{
		this.open();

		String id = queryNote(mRowId).getString(queryNote(mRowId)
												.getColumnIndexOrThrow(DB_page.KEY_NOTE_BODY));
		this.close();

		return id;
	}

	public Long getNoteCreatedTime_byId(Long mRowId)
	{
		this.open();

		Long time = queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB_page.KEY_NOTE_CREATED));

		this.close();

		return time;
	}

	// get note by position
	public Long getNoteId(int position,boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);
	    @SuppressLint("Range") Long id = mCursor_note.getLong(mCursor_note.getColumnIndex(KEY_NOTE_ID));

		if(enDbOpenClose)
	    	this.close();

		return id;
	}	
	
	@SuppressLint("Range")
	public String getNoteTitle(int position, boolean enDbOpenClose)
	{
		String title = null;

		if(enDbOpenClose)
			this.open();

		if(mCursor_note.moveToPosition(position))
			title = mCursor_note.getString(mCursor_note.getColumnIndex(KEY_NOTE_TITLE));

		if(enDbOpenClose)
        	this.close();

		return title;
	}	
	
	public String getNoteBody(int position,boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);

		@SuppressLint("Range") String body = mCursor_note.getString(mCursor_note.getColumnIndex(KEY_NOTE_BODY));

		if(enDbOpenClose)
        	this.close();

		return body;
	}

	public int getNoteMarking(int position,boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);

		@SuppressLint("Range") int marking = mCursor_note.getInt(mCursor_note.getColumnIndex(KEY_NOTE_MARKING));

		if(enDbOpenClose)
			this.close();

		return marking;
	}
	
	public Long getNoteCreatedTime(int position,boolean enDbOpenClose)
	{
		if(enDbOpenClose)
			this.open();

		mCursor_note.moveToPosition(position);
		@SuppressLint("Range") Long time = mCursor_note.getLong(mCursor_note.getColumnIndex(KEY_NOTE_CREATED));

		if(enDbOpenClose)
			this.close();

		return time;
	}
}
