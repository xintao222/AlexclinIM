package alexclin.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AddOtherDB {
	private static final String DB_NAME = "addother.db";
	private static final String TABLE_NAME = "addother";
	private static final String USER_ID = "userid";
	private static final String OTHER_JID = "jid";

	public static boolean isInAddHistory(Context ctx, String jid, String userId) {
		AddOtherHelper helper = new AddOtherHelper(ctx);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = db.query(TABLE_NAME, null, USER_ID + "= ? and " + OTHER_JID + "= ?",
				new String[] { userId, jid }, null, null, null);
		boolean result = c.moveToNext();
		c.close();
		db.close();
		helper.close();
		return result;
	}

	public static void record(Context ctx, String jid, String userId) {
		AddOtherHelper helper = new AddOtherHelper(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(USER_ID, userId);
		cv.put(OTHER_JID, jid);
		db.insert(TABLE_NAME, null, cv);
		db.close();
		helper.close();
	}

	public static void remove(Context ctx, String jid, String userId) {
		AddOtherHelper helper = new AddOtherHelper(ctx);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete(TABLE_NAME, USER_ID + "= ? and " + OTHER_JID + "= ?", new String[] { userId, jid });
		db.close();
		helper.close();
	}

	static class AddOtherHelper extends SQLiteOpenHelper {
		public AddOtherHelper(Context context) {
			super(context, DB_NAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME
					+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT," + USER_ID
					+ " TEXT," + OTHER_JID + " TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}

	}

}
