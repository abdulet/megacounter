package com.abdulet.megacounter;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.*;
import java.util.Calendar;

public class MainActivity extends Activity implements DatePicker.OnDateChangedListener
{
    /** Called when the activity is first created. */
	private SQLiteDatabase db;
	private View target;
	private String counterName;
	private PopupWindow pw;
	private Long dateFrom, dateTo;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		this.db=openOrCreateDatabase("megaCounter",MODE_PRIVATE,null);
		this.db.execSQL("CREATE TABLE IF NOT EXISTS counters " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR" +
                ", date REAL DEFAULT (datetime('now', 'localtime')));");
		this.db.execSQL("CREATE TABLE IF NOT EXISTS hints " +
                "(id int,date REAL DEFAULT (datetime('now', 'localtime')));");
		this.loadCounters();
    }

	public void createCounter (View view){
		EditText counter = (EditText) findViewById(R.id.newCounter);
        if(counter.getText() != null && !counter.getText().toString().equals("")) {
            ContentValues values = new ContentValues();
            values.put("name", counter.getText().toString());
            this.db.insert("counters", "", values);
            this.clearLayouts();
            this.loadCounters();
            counter.setText("");
        }
	}
	
	public void loadCounters(){
		this.loadCounters("SELECT * FROM counters", "ORDER BY name");
	}
	
	public void loadCounters(String query, String order){
		LinearLayout counters = (LinearLayout) findViewById(R.id.counters);
		
		Cursor c = db.rawQuery(query+" "+order, null);
		c.moveToFirst();
        LinearLayout.LayoutParams lpTxt = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT
                , LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTxt.weight = 1;
		while (!c.isAfterLast()){
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.LEFT|Gravity.TOP);
			TextView txt = new TextView(this);
			TextView hints = new TextView(this);
			txt.setText(c.getString(1));
            txt.setLayoutParams(lpTxt);
			txt.setTextSize(35);
			Cursor hnt = this.db.rawQuery("select count(date) from hints where id="
                    +c.getLong(c.getColumnIndex("id"))+";",null);
			hnt.moveToFirst();
			hints.setText(hnt.getString(0));
			hints.setTextSize(35);
			hints.setTag(c.getLong(0));
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
                    SQLiteDatabase db = openOrCreateDatabase("megaCounter", MODE_PRIVATE, null);
                    int hints = Integer.parseInt(tv.getText().toString());
					hints+=1;
                    tv.setText(Integer.toString(hints));
                    db.execSQL("insert into hints (id) values (" + Long.toString(id) + ")");
                    db.close();
                }
            });
			registerForContextMenu(row);
			counters.addView(row);
			c.moveToNext();
		}
		c.close();
	}
	
	public void clearLayouts(){
		LinearLayout counters = (LinearLayout)  findViewById(R.id.counters);
		counters.removeAllViewsInLayout();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v
            , ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		this.target = v;
		LinearLayout row = (LinearLayout) v;
		TextView tv = (TextView) row.getChildAt(0);
		this.counterName = tv.getText().toString();
		inflater.inflate(R.menu.contextual_menu, menu);
		String menuTitle = getString(R.string.menu_del);
		menu.add(0,R.id.menu_del,0,menuTitle);
    }

    public boolean onContextItemSelected(MenuItem item) {
		LinearLayout row = (LinearLayout) this.target;
        switch (item.getItemId()) {
            case R.id.menu_sub:
                this.subtract( (TextView) row.getChildAt(1));
                return true;
            case R.id.menu_del:
                this.delete(  );
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void subtract(TextView tv){
        int hints = Integer.parseInt(tv.getText().toString());
		hints-=1;
        tv.setText(Integer.toString(hints));
        this.db.execSQL("delete from hints where(id="
                + tv.getTag().toString() +" and date=(select max(date) from hints where id="
                + tv.getTag().toString() +"))");
    }

    private void delete(){
		DialogInterface.OnClickListener dialogClickListener
                = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        LinearLayout row = (LinearLayout) MainActivity.this.target;
                        TextView tv = (TextView) row.getChildAt(1);
                        Integer id = Integer.parseInt(tv.getTag().toString());
                        MainActivity.this.db.execSQL
                                ("delete from hints where (id="+id.toString()+")");
                        MainActivity.this.db.execSQL
                                ("delete from counters where (id="+id.toString()+")");
                        row.removeAllViews();
                    break;
                }
            }
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.confirm_deletion)
                +" "+MainActivity.this.counterName)
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener).show();
		MainActivity.this.counterName=null;
    }
	
	public void showSearchWindow(View v){
		//android.util.Log.d("aaa","jjjjjjjjj");
		LayoutInflater inflater = (LayoutInflater) MainActivity.this
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.popup_search, 
			(ViewGroup) findViewById(R.id.popupParent));
		Calendar c = Calendar.getInstance();
		DatePicker sFrom = (DatePicker) layout.findViewById(R.id.searchFrom);
        DatePicker sTo = (DatePicker) layout.findViewById(R.id.searchTo);
		sFrom.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), this);
		sTo.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), this);
		
		pw = new PopupWindow(layout, 700, 1100, true);
		// display the popup in the center
		pw.showAtLocation(layout, Gravity.CENTER, 0, 0);
	}

    public void onDateChanged(DatePicker v, int year, int monthOfYear, int dayOfMonth) {
		String strNewDate = Integer.toString(year)
                +Integer.toString(monthOfYear)+Integer.toString(dayOfMonth);
        Integer intNewDate = Integer.parseInt(strNewDate);
		LinearLayout ly = (LinearLayout) v.getParent();
        if ( v.getId() == R.id.searchFrom ){
			DatePicker tmpTo = (DatePicker) ly.findViewById(R.id.searchTo);
			String strDateTo = Integer.toString(tmpTo.getYear())
                    +Integer.toString(tmpTo.getMonth())+Integer.toString(tmpTo.getDayOfMonth());
            Integer intDateTo = Integer.parseInt(strDateTo);
			if(intNewDate > intDateTo){
				tmpTo.updateDate(year,monthOfYear,dayOfMonth);
			}
        }else{
			DatePicker tmpFrom = (DatePicker) ly.findViewById(R.id.searchFrom);
			String strDateFrom = Integer.toString(tmpFrom.getYear())
                    +Integer.toString(tmpFrom.getMonth())+Integer.toString(tmpFrom.getDayOfMonth());
            Integer intDateFrom = Integer.parseInt(strDateFrom);
			if(intNewDate < intDateFrom){
				tmpFrom.updateDate(year,monthOfYear,dayOfMonth);
			}
        }
    }

	public void search(View v){
		LinearLayout ly = (LinearLayout) v.getParent();
		EditText tv = (EditText) ly.findViewById(R.id.searchName);
        String where = "";
		
		if (tv.getText() != null && !tv.getText().toString().contentEquals("")){
			where = "name like '%"+tv.getText().toString()+"%'";
		}
		
		if (dateFrom != null && dateFrom > 0) {
            if (!where.equals(""))
                where += " and ";
            where += "date > "+Long.toString(dateFrom);
			dateFrom = null;
			
			if (dateTo != null && dateTo > 0) {
				where += " and date < "+Long.toString(dateTo);
				dateTo = null;
			}
        }
		android.util.Log.d("query", "SELECT name FROM counters WHERE ("+where+")");
		/*
		clearLayouts();
		loadCounters("SELECT name FROM counters WHERE ("+where+")","ORDER BY name");
		*/
		pw.dismiss();
	}
}
