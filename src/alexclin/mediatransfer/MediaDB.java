package alexclin.mediatransfer;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 用来存储音频和图片与网址的对应关系
 * @author Administrator
 *
 */
public class MediaDB {
	public static final int TYPE_VOICE = 1;
	public static final int TYPE_IMAGE = 2;
	public static final int STATUS_FAILED = 1;
	public static final int STATUS_SUCCESS = 2;
	private static final String DB_NAME = "media.db";
	private static final String TABLE_NAME = "media";
	private static final String C_ID = "_id";
	private static final String C_URL = "url";
	private static final String C_PATH = "path";
	private static final String C_TYPE = "type";
	private static final String C_STATUS = "status";
	private SQLiteOpenHelper mDbOpenHelper;

	public MediaDB(Context context) {
		super();
		this.mDbOpenHelper = new AudioDBHelper(context);
	}

	public void close() {
		mDbOpenHelper.close();
	}

	public void insert(String url, String path) {
		SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(C_URL, url);
		cv.put(C_PATH, path);
		db.insert(TABLE_NAME, null, cv);
	}

	public void delete(String url) {
		SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
		String path = getPathByUrl(url, db);
		db.delete(TABLE_NAME, C_URL + "= ?", new String[] { url });
		deleteFile(path);
	}

	public String getPath(String url) {
		SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
		String path = getPathByUrl(url, db);
		return path;
	}

	private String getPathByUrl(String url, SQLiteDatabase db) {
		Cursor c = db.query(TABLE_NAME, new String[] { C_URL, C_PATH }, C_URL
				+ "= ?", new String[] { url }, null, null, null);
		String path = null;
		if (c.moveToNext()) {
			path = c.getString(c.getColumnIndex(C_PATH));
		}
		c.close();
		return path;
	}

	private void deleteFile(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
	}

	class AudioDBHelper extends SQLiteOpenHelper {

		public AudioDBHelper(Context context) {
			super(context, DB_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + C_ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT," 
					+ C_URL + " TEXT,"
					+ C_PATH + " TEXT,"
					+ C_TYPE + " INTEGER,"
					+ C_STATUS + " INTEGER);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
	}
}
