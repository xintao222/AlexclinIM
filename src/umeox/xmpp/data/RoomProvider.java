package umeox.xmpp.data;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

public class RoomProvider extends ContentProvider{
	public static final String AUTHORITY = "xmpp.android.provider.Rooms";
	public static final String TABLE_NAME = "rooms";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + TABLE_NAME);

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);

	private static final int ROOMS = 1;
	private static final int ROOM_ID = 2;

	static {
		URI_MATCHER.addURI(AUTHORITY, "rooms", ROOMS);
		URI_MATCHER.addURI(AUTHORITY, "rooms/#", ROOM_ID);
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
			String arg4) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues cv, String where, String[] arg3) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static class RoomDatabaseHelper extends SQLiteOpenHelper{
		private static final String DATABASE_NAME = "room.db";
		private static final int DATABASE_VERSION = 1;

		public RoomDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
					+ RoomConstants._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ RoomConstants.JID + " TEXT, "
					+ RoomConstants.NAME + " TEXT);");			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);			
		}
		
	}
	
	public static final class RoomConstants implements BaseColumns{
		
		private RoomConstants() {
		}
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xmpp.room";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xmpp.room";
		public static final String DEFAULT_SORT_ORDER = "_id ASC"; // sort by auto-id
		
		public static final String JID = "jid";
		public static final String NAME = "name";
		
		public static ArrayList<String> getRequiredColumns(){
			ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(JID);
			tmpList.add(NAME);
			return tmpList;
		}
	}

}
