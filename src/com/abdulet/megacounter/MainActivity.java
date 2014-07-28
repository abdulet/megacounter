package com.abdulet.megacounter;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.RadioGroup.*;
import android.support.v4.util.*;
import android.util.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.*;
import android.nfc.*;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
	private SQLiteDatabase db;
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		this.db=openOrCreateDatabase("megaCounter",MODE_PRIVATE,null);
		this.db.execSQL("CREATE TABLE IF NOT EXISTS counters (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, date REAL DEFAULT (datetime('now', 'localtime')));");
		this.db.execSQL("CREATE TABLE IF NOT EXISTS hints (id int,date REAL DEFAULT (datetime('now', 'localtime')));");
		this.loadCounters();
    }
	
	public void createCounter (View view){
		EditText counter = (EditText) findViewById(R.id.newCounter);
		TextView txt = new TextView(view.getContext());
		TextView hints = new TextView(view.getContext());
		GridLayout counters = (GridLayout) findViewById(R.id.counters);
		this.db.execSQL("INSERT INTO counters (name) VALUES('"+counter.getText().toString()+"');");
		ContentValues values = new ContentValues();
		values.put("name", counter.getText().toString());
		long counterId = this.db.insert("counters","",values);
		txt.setText(counter.getText());
		txt.setTextSize(15);
		hints.setText("0");
		/*
		hints.setTag(0, counterId);
		hints.setTag(1, hints);
		hints.setTag(2, this.db);
		*/
		hints.setTextSize(15);
		hints.setClickable(true);
		/*
		txt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				long id = v.getTag(0);
				TextView tv = (TextView) v.getTag(1);
				SQLiteDatabase db = (SQLiteDatabase) v.getTag(2);
				int hints = Integer.parseInt(tv.getText().toString());
				tv.setText(Integer.toString(hints++));
				db.execSQL("insert into hints (id) values ("+id+");");
			}
		});
		*/
		counters.addView(txt);
		counters.addView(hints);
		counter.setText("");
	}
	
	public void loadCounters(){
		GridLayout counters = (GridLayout) findViewById(R.id.counters);
		Cursor c = db.rawQuery("SELECT * from counters", null);
		c.moveToFirst();
		while (c.isAfterLast() == false){
			TextView txt = new TextView(counters.getContext());
			TextView hints = new TextView(counters.getContext());
			txt.setText(c.getString(1));
			Cursor hnt = this.db.rawQuery("select count(date) from hints where id="+c.getLong(c.getColumnIndex("id"))+";",null);
			hnt.moveToFirst();
			hints.setText(hnt.getString(0));
			hnt.close();
			counters.addView(txt);
			counters.addView(hints);
			c.moveToNext();
		}
		c.close();
	}
}
