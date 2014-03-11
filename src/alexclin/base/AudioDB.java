package alexclin.base;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class AudioDB {
	private static final String DB_NAME = "audio.db";
	private static final String TABLE_NAME = "voice";
	private static final String ID = "_id";
	private static final String URL = "url";
	private static final String PATH = "path";
	
	private SQLiteOpenHelper mDbOpenHelper;
	
	public AudioDB(Context context) {
		super();
		this.mDbOpenHelper = new AudioDBHelper(context);
	}
	
	public void close(){
		mDbOpenHelper.close();
	}

	public void insert(String url,String path){
		SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(URL, url);
		cv.put(PATH, path);
		db.insert(TABLE_NAME, null, cv);
	}
	
	public void delete(String url){
		SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
		String path = getPathByUrl(url, db);		
		db.delete(TABLE_NAME, URL + "= ?", new String[]{url});
		deleteFile(path);
	}
	
	public String getPath(String url){
		SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
		String path = getPathByUrl(url, db);
		return path;
	}

	private String getPathByUrl(String url, SQLiteDatabase db) {
		Cursor c = db.query(TABLE_NAME, new String[]{URL,PATH},URL + "= ?",new String[]{url}, null, null, null);
		String path = null;
		if(c.moveToNext()){
			path = c.getString(c.getColumnIndex(PATH));
		}
		c.close();
		return path;
	}

	private void deleteFile(String path) {
		File file = new File(path);
		if(file.exists()){
			file.delete();
		}
	}

	class AudioDBHelper extends SQLiteOpenHelper{
		
		public AudioDBHelper(Context context) {
			super(context, DB_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ URL + " TEXT,"
					+ PATH + " TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}		
	}
}
