package com.abdulet.megacounter;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.*;
import java.util.Calendar;
import java.util.TimeZone;
import org.achartengine.*;
import org.achartengine.model.*;
import org.achartengine.renderer.*;
import org.achartengine.chart.*;
import android.graphics.*;

public class MainActivity extends Activity implements DatePicker.OnDateChangedListener
{
    /** Called when the activity is first created. */
	private SQLiteDatabase db;
	private View target;
	private String counterName;
	private Long dateFrom, dateTo;
	private static final int DAY=0, WEEK=1, MONTH=2, YEAR=3;
	private static final int DAYINMILS=1440000, WEEKINMILS=10080000, MONTHINMILS=43200000, YEARINMILS=525600000;
	
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
		loadCounters("SELECT * FROM counters", "ORDER BY name");
	}
	
	public void loadCounters(String query, String order){
		LinearLayout counters = (LinearLayout) findViewById(R.id.counters);

        android.util.Log.d("query in Load", query+" "+order);
		Cursor c = db.rawQuery(query+" "+order, null);

        LinearLayout.LayoutParams lpTxt = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT
                , LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTxt.weight = 1;
        c.moveToFirst();
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
	public void onCreateContextMenu(
	ContextMenu menu, View v
            , ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		this.target = v;
		LinearLayout row = (LinearLayout) v;
		TextView tv = (TextView) row.getChildAt(0);
		this.counterName = tv.getText().toString();
		inflater.inflate(R.menu.contextual_menu, menu);
		menu.add(0,R.id.menu_del,0, getString(R.string.menu_del));
		menu.add(0,R.id.menu_dayChart,0, getString(R.string.menu_dayChart));
		menu.add(0,R.id.menu_weekChart,0, getString(R.string.menu_weekChart));
		menu.add(0,R.id.menu_monthChart,0, getString(R.string.menu_monthChart));
		menu.add(0,R.id.menu_yearChart,0, getString(R.string.menu_yearChart));
    }

    public boolean onContextItemSelected(MenuItem item) {
		LinearLayout row = (LinearLayout) this.target;
        switch (item.getItemId()) {
            case R.id.menu_sub:
                this.subtract( (TextView) row.getChildAt(1));
                return true;
            case R.id.menu_del:
                this.delete();
                return true;
            case R.id.menu_dayChart:
                this.getStats(this.DAY);
                return true;
			case R.id.menu_weekChart:
                this.getStats(this.WEEK);
                return true;
			case R.id.menu_monthChart:
                this.getStats(this.MONTH);
                return true;
			case R.id.menu_yearChart:
                this.getStats(this.YEAR);
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
        setContentView(R.layout.search);
		Calendar c = Calendar.getInstance();
		DatePicker sFrom = (DatePicker) findViewById(R.id.searchFrom);
        DatePicker sTo = (DatePicker) findViewById(R.id.searchTo);
		sFrom.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), this);
		sTo.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), this);
	}

    public void onDateChanged(DatePicker v, int year, int monthOfYear, int dayOfMonth) {
		LinearLayout ly = (LinearLayout) v.getParent();

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthOfYear);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        Long lngNewDate = cal.getTimeInMillis();

        if ( v.getId() == R.id.searchFrom ){
			DatePicker dateTo = (DatePicker) ly.findViewById(R.id.searchTo);
            cal.set(Calendar.YEAR, dateTo.getYear());
            cal.set(Calendar.MONTH, dateTo.getMonth());
            cal.set(Calendar.DAY_OF_MONTH, dateTo.getDayOfMonth());
            Long lngDateTo = cal.getTimeInMillis();

            if(lngNewDate > lngDateTo){
				dateTo.updateDate(year, monthOfYear, dayOfMonth);
			}
            dateFrom = lngNewDate;
        }else if ( v.getId() == R.id.searchTo) {
			DatePicker dateFrom = (DatePicker) ly.findViewById(R.id.searchFrom);
            cal.set(Calendar.YEAR, dateFrom.getYear());
            cal.set(Calendar.MONTH, dateFrom.getMonth());
            cal.set(Calendar.DAY_OF_MONTH, dateFrom.getDayOfMonth());

            Long lngDateFrom = cal.getTimeInMillis();
			if(lngNewDate < lngDateFrom){
				dateFrom.updateDate(year, monthOfYear, dayOfMonth);
			}
            dateTo = lngNewDate;
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
            android.util.Log.d("Time From", Long.toString(dateFrom));
            if (!where.equals(""))
                where += " and ";
            where += "date > "+Long.toString(dateFrom);
			dateFrom = null;
			
			if (dateTo != null && dateTo > 0) {
                android.util.Log.d("Time To", Long.toString(dateTo));
				where += " and date < " + Long.toString(dateTo);
				dateTo = null;
			}
        }
        if (!where.equalsIgnoreCase("")){
            where = "WHERE ("+where+")";
        }
		android.util.Log.d("query", "SELECT * FROM counters "+where);
        setContentView(R.layout.main);
		clearLayouts();
		loadCounters("SELECT * FROM counters "+where,"ORDER BY name");
        View bt = findViewById(R.id.clearSearch);
        bt.setVisibility(View.VISIBLE);
	}

    public void clearSearch(View v){
        clearLayouts();
        loadCounters();
        v.setVisibility(View.GONE);
    }

	private XYSeries getSeries(int period, int id, String name){
		XYSeries series = new XYSeries(name);
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		//Calendar.YEAR
		//Calendar.MONTH
		//Calendar.DAY_OF_MONTH
		String query = "SELECT * FROM counters where (id="+Integer.toString(id);
		long Fromdate = 0;
		switch(period){
			case DAY:
				Fromdate = cal.getTimeInMillis() - DAYINMILS;
				break;
			case WEEK:
				Fromdate = cal.getTimeInMillis() - WEEKINMILS;
				break;
			case MONTH:
				Fromdate = cal.getTimeInMillis() - MONTHINMILS;
				break;
			case YEAR:
				Fromdate = cal.getTimeInMillis() - YEARINMILS;
				break;
		}
		query.concat(" AND date > "+Long.toString(Fromdate)+")");
		Cursor c = db.rawQuery(query, null);
		return series;
	}
	
    public void getStats(int period){
        LinearLayout row = (LinearLayout) target;
		TextView tv = (TextView ) row.getChildAt(1);
		Integer cId = Integer.parseInt(tv.getText().toString());
		tv = (TextView ) row.getChildAt(1);
		
		// example from: http://www.survivingwithandroid.com/2014/06/android-chart-tutorial-achartengine.html?m=1
		XYSeries series = this.getSeries(period, cId, tv.getText().toString());
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

		/*
		for(i=0; i<counters.length; i++){
			series.add(unity,counters[i]);
		}
		*/
		
		dataset.addSeries(series);
		
		// Now we create the renderer
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setLineWidth(2); 
		renderer.setColor(Color.RED); 
		
		// Include low and max value 
		renderer.setDisplayBoundingPoints(true);
		
		// we add point markers 
		renderer.setPointStyle(PointStyle.CIRCLE); 
		renderer.setPointStrokeWidth(3);
		
		XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
		mRenderer.addSeriesRenderer(renderer);

		// We want to avoid black border
		mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins 
		// Disable Pan on two axis 
		mRenderer.setPanEnabled(false, false); 
		mRenderer.setYAxisMax(35); 
		mRenderer.setYAxisMin(0); 
		mRenderer.setShowGrid(true); // we show the grid
		
		GraphicalView chartView = ChartFactory.getLineChartView(MainActivity.this, dataset, mRenderer);
		
		//add to view
		//chartLyt.addView(chartView,0);
    }
	
	public void getStatsDay (){
		this.getStats(this.DAY);
	}
	
	public void getStatsWek (){
		this.getStats(this.WEEK);
	}
	
	public void getStatsMonth (){
		this.getStats(this.MONTH);
	}
	
	public void getStatsYear (){
		this.getStats(this.YEAR);
	}
}
