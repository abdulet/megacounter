package com.abdulet.megacounter;

import android.app.*;
import android.os.*;
//import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
//import android.widget.RadioGroup.*;
//import android.support.v4.util.*;
import android.util.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.*;
//import android.nfc.*;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
	private SQLiteDatabase db;
	private long counterId;
	private View target;
    @Override
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
		LinearLayout counters = (LinearLayout) findViewById(R.id.counters);
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		ContentValues values = new ContentValues();
		values.put("name", counter.getText().toString());
		this.counterId = this.db.insert("counters","",values);
        LinearLayout.LayoutParams lpTxt = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTxt.weight = 1;
		txt.setText(counter.getText());
		txt.setTextSize(35);
        txt.setLayoutParams(lpTxt);
		hints.setText("0");
		hints.setTag(this.counterId);
		hints.setTextSize(35);
		row.setClickable(true);
		row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout row = (LinearLayout) v;
                TextView tv = (TextView) row.getChildAt(1);
                long id = (Long) tv.getTag();
                Log.d("id", Long.toString(id));
                SQLiteDatabase db = openOrCreateDatabase("megaCounter", MODE_PRIVATE, null);
                int hints = Integer.parseInt(tv.getText().toString());
				hints+=1;
                Log.d("hints", Integer.toString(hints));
                tv.setText(Integer.toString(hints++));
                db.execSQL("insert into hints (id) values (" + Long.toString(id) + ")");
                Log.d("query", "insert into hints (id) values (" + Long.toString(id) + ")");
                db.close();
            }
        });
		registerForContextMenu(row);
		row.addView(txt);
		row.addView(hints);
		counters.addView(row);
		counter.setText("");
	}
	
	public void loadCounters(){
		LinearLayout counters = (LinearLayout) findViewById(R.id.counters);
		Cursor c = db.rawQuery("SELECT * from counters", null);
		c.moveToFirst();
        LinearLayout.LayoutParams lpTxt = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTxt.weight = 1;
		while (!c.isAfterLast()){
			this.counterId = c.getLong(0);
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.LEFT|Gravity.TOP);
			TextView txt = new TextView(this);
			TextView hints = new TextView(this);
			txt.setText(c.getString(1));
            txt.setLayoutParams(lpTxt);
			txt.setTextSize(35);
			Cursor hnt = this.db.rawQuery("select count(date) from hints where id="+c.getLong(c.getColumnIndex("id"))+";",null);
			hnt.moveToFirst();
			hints.setText(hnt.getString(0));
			hints.setTextSize(35);
			hints.setTag(this.counterId);
			hnt.close();
			row.addView(txt);
			row.addView(hints);
			row.setClickable(true);
			row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout row = (LinearLayout) v;
                    TextView tv = (TextView) row.getChildAt(1);
                    long id = (Long) tv.getTag();
                    //Log.d("id", Long.toString(id));
                    SQLiteDatabase db = openOrCreateDatabase("megaCounter", MODE_PRIVATE, null);
                    int hints = Integer.parseInt(tv.getText().toString());
                    //Log.d("hints", Integer.toString(hints++));
					hints+=1;
                    tv.setText(Integer.toString(hints));
                    db.execSQL("insert into hints (id) values (" + Long.toString(id) + ")");
                    //Log.d("query", "insert into hints (id) values (" + Long.toString(id) + ")");
                    db.close();
                }
            });
			registerForContextMenu(row);
			counters.addView(row);
			//Log.d("aaaaaa", c.getString(0));
			//Log.d("bbbbbb", c.getString(1));
			c.moveToNext();
		}
		c.close();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		this.target = v;
		LinearLayout row = (LinearLayout) v;
		TextView tv = (TextView) row.getChildAt(0);
		inflater.inflate(R.menu.contextual_menu, menu);
		String menuTitle = getString(R.string.menu_del)+" "+tv.getText().toString();
		menu.add(0,99,0,menuTitle);
	}

    public boolean onContextItemSelected(MenuItem item) {
        //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		Log.d("aaaa", this.target.toString());
		LinearLayout row = (LinearLayout) this.target;
        switch (item.getItemId()) {
            case R.id.menu_sub:
				Log.d("iii",row.getChildAt(1).toString());
                this.subtract( (TextView) row.getChildAt(1));
                return true;
            case 99:
                this.delete(row);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void subtract(TextView tv){
        int hints = Integer.parseInt(tv.getText().toString());
		hints-=1;
        tv.setText(Integer.toString(hints));
        this.db.execSQL("delete from hints where(id="+ tv.getTag().toString() +" and date=(select max(date) from hints where id="+ tv.getTag().toString() +"))");
    }

    private void delete( LinearLayout row ){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() { 
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					LinearLayout row = (LinearLayout) MainActivity.this.target;
					TextView tv = (TextView) row.getChildAt(1);
					Integer id = Integer.parseInt( tv.getTag().toString());
					MainActivity.this.db.execSQL("delete from hints where (id="+id.toString()+")");
					MainActivity.this.db.execSQL("delete from counters where (id="+id.toString()+")");
					row.removeAllViews();
				break;
			}
		}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
    }
}
