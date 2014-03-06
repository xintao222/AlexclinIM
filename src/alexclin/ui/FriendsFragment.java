package alexclin.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.ChatProvider.ChatConstants;
import umeox.xmpp.data.RosterProvider;
import umeox.xmpp.data.RosterProvider.RosterConstants;
import umeox.xmpp.util.PrefConsts;
import alexclin.dialogs.AddRosterItemDialog;
import alexclin.dialogs.GroupNameView;
import alexclin.ui.base.StatusMode;
import alexclin.xmpp.jabberim.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 好友列表界面
 * 
 * @author alex
 * 
 */
public class FriendsFragment extends Fragment implements OnChildClickListener {
	private static final String TAG = "FriendsFragment";
	private MainTabActivity mMainActivity;
	private ExpandableListView mListView;
	RosterExpListAdapter rosterListAdapter;
	private ContentObserver mRosterObserver;
	private ContentObserver mChatObserver;	

	private HashMap<String, Boolean> mGroupsExpanded = new HashMap<String, Boolean>();
	private static final String OFFLINE_EXCLUSION = RosterConstants.STATUS_MODE
			+ " != " + StatusMode.offline.ordinal();
	private static final String countAvailableMembers = "SELECT COUNT() FROM "
			+ RosterProvider.TABLE_ROSTER + " inner_query"
			+ " WHERE inner_query." + RosterConstants.GROUP + " = "
			+ RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP
			+ " AND inner_query." + OFFLINE_EXCLUSION;
	private static final String countMembers = "SELECT COUNT() FROM "
			+ RosterProvider.TABLE_ROSTER + " inner_query"
			+ " WHERE inner_query." + RosterConstants.GROUP + " = "
			+ RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP;
	private static final String[] GROUPS_QUERY_COUNTED = new String[] {
			RosterConstants._ID,
			RosterConstants.GROUP,
			"(" + countAvailableMembers + ") || '/' || (" + countMembers
					+ ") AS members" };
	private static final String[] GROUPS_FROM = new String[] {
			RosterConstants.GROUP, "members" };
	private static final int[] GROUPS_TO = new int[] { R.id.groupname,
			R.id.members };
	private static final String[] GROUPS_QUERY = new String[] {
			RosterConstants._ID, RosterConstants.GROUP, };

	private static final String[] ROSTER_QUERY = new String[] {
			RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS,
			RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mMainActivity = (MainTabActivity) getActivity();
		mRosterObserver = new RosterObserver();
		mChatObserver = new ChatObserver();
		mMainActivity.getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mRosterObserver);
		mMainActivity.getContentResolver().registerContentObserver(
				ChatProvider.CONTENT_URI, true, mChatObserver);		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.frg_friendlist, container, false);
		initViews(view);
		return view;
	}

	private void initViews(View v) {
		mListView = (ExpandableListView) v
				.findViewById(R.id.FriendList_FriendFrag);
		mListView.requestFocus();

		mListView
				.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
					public boolean onGroupClick(ExpandableListView parent,
							View v, int groupPosition, long id) {
						groupClicked = true;
						return false;
					}
				});
		mListView
				.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
					public void onGroupCollapse(int groupPosition) {
						handleGroupChange(groupPosition, false);
					}
				});
		mListView
				.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
					public void onGroupExpand(int groupPosition) {
						handleGroupChange(groupPosition, true);
					}
				});
		mListView.setOnChildClickListener(this);
		registerForContextMenu(mListView);
		registerListAdapter();
	}

	@Override
	public void onDestroy() {
		mMainActivity.getContentResolver().unregisterContentObserver(
				mRosterObserver);
		mMainActivity.getContentResolver().unregisterContentObserver(
				mChatObserver);
		super.onDestroy();
	}

	// need this to workaround unwanted OnGroupCollapse/Expand events
	boolean groupClicked = false;

	void handleGroupChange(int groupPosition, boolean isExpanded) {
		String groupName = getGroupName(groupPosition);
		if (groupClicked) {
			Log.d(TAG, "group status change: " + groupName + " -> "
					+ isExpanded);
			mGroupsExpanded.put(groupName, isExpanded);
			groupClicked = false;
			// } else {
			// if (!mGroupsExpanded.containsKey(name))
			// restoreGroupsExpanded();
		}
	}

	// get the name of a roster group from the cursor
	public String getGroupName(int groupId) {
		return getPackedItemRow(
				ExpandableListView.getPackedPositionForGroup(groupId),
				RosterConstants.GROUP);
	}

	private String getPackedItemRow(long packedPosition, String rowName) {
		int flatPosition = mListView.getFlatListPosition(packedPosition);
		Cursor c = (Cursor) mListView.getItemAtPosition(flatPosition);
		return c.getString(c.getColumnIndex(rowName));
	}

	// store mGroupsExpanded into prefs (this is a hack, but SQLite /
	// content providers suck wrt. virtual groups)
	public void storeExpandedState() {
		SharedPreferences.Editor prefedit = PreferenceManager
				.getDefaultSharedPreferences(mMainActivity).edit();
		for (HashMap.Entry<String, Boolean> item : mGroupsExpanded.entrySet()) {
			prefedit.putBoolean("expanded_" + item.getKey(), item.getValue());
		}
		prefedit.commit();
	}

	public void restoreGroupsExpanded() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mMainActivity);
		for (int count = 0; count < mListView.getExpandableListAdapter()
				.getGroupCount(); count++) {
			String name = getGroupName(count);
			if (!mGroupsExpanded.containsKey(name))
				mGroupsExpanded.put(name,
						prefs.getBoolean("expanded_" + name, true));
			Log.d(TAG, "restoreGroupsExpanded: " + name + ": "
					+ mGroupsExpanded.get(name));
			if (mGroupsExpanded.get(name))
				mListView.expandGroup(count);
			else
				mListView.collapseGroup(count);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		long packedPosition = ExpandableListView.getPackedPositionForChild(
				groupPosition, childPosition);
		Cursor c = (Cursor)mListView.getItemAtPosition(mListView.getFlatListPosition(packedPosition));
		String userJid = getPackedItemRow(packedPosition, RosterConstants.JID);
		String userName = getPackedItemRow(packedPosition,
				RosterConstants.ALIAS);
		// TODO
		Intent i = mMainActivity.getIntent();
		if (i.getAction() != null && i.getAction().equals(Intent.ACTION_SEND)) {
			// delegate ACTION_SEND to child window and close self
			mMainActivity.startChatActivity(userJid, userName,
					i.getStringExtra(Intent.EXTRA_TEXT));
			mMainActivity.finish();
		} else{
			int s = c.getInt(c.getColumnIndexOrThrow(RosterConstants.STATUS_MODE));
			if (s == PrefConsts.subscribe)
				rosterAddRequestedDialog(userJid,
					c.getString(c.getColumnIndexOrThrow(RosterConstants.STATUS_MESSAGE)));
			else
				mMainActivity.startChatActivity(userJid, userName, null);
		}			
		return true;
	}
	
	private void rosterAddRequestedDialog(final String jid, String message) {
		new AlertDialog.Builder(mMainActivity)
			.setTitle(R.string.subscriptionRequest_title)
			.setMessage(getString(R.string.subscriptionRequest_text, jid, message))
			.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mMainActivity.serviceAdapter.sendPresenceRequest(jid, "subscribed");
//							addToRosterDialog(jid);
							mMainActivity.serviceAdapter.sendPresenceRequest(jid,"subscribe");
						}
					})
			.setNegativeButton(android.R.string.no, 
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							mMainActivity.serviceAdapter.sendPresenceRequest(jid, "unsubscribed");
						}
					})
			.create().show();
	}

	boolean addToRosterDialog(String jid) {
		if (mMainActivity.serviceAdapter != null && mMainActivity.serviceAdapter.isAuthenticated()) {
			new AddRosterItemDialog(this, mMainActivity.serviceAdapter, jid).show();
			return true;
		} else {
			mMainActivity.showToastNotification(R.string.Global_authenticate_first);
			return false;
		}
	}
	
	public void updateRoster() {
		rosterListAdapter.requery();
		restoreGroupsExpanded();
	}

	@Override
	public void onPause() {
		restoreGroupsExpanded();
		super.onPause();
	}
	
	public Handler getHandler(){
		return ((MainTabActivity)getActivity()).getHadnler();
	}

	@Override
	public void onResume() {
		updateRoster();
		// handle imto:// intent after restoring service connection
		getHandler().post(new Runnable() {
			public void run() {
				handleJabberIntent();
			}
		});
		super.onResume();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		ExpandableListView.ExpandableListContextMenuInfo info;

		try {
			info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuinfo: ", e);
			return;
		}

		long packedPosition = info.packedPosition;
		boolean isChild = isChild(packedPosition);

		mMainActivity.getMenuInflater()
				.inflate(R.menu.roster_contextmenu, menu);

		// get the entry name for the item
		String menuName;
		if (isChild) {
			menuName = String.format("%s (%s)",
					getPackedItemRow(packedPosition, RosterConstants.ALIAS),
					getPackedItemRow(packedPosition, RosterConstants.JID));
		} else {
			menuName = getPackedItemRow(packedPosition, RosterConstants.GROUP);
			if (menuName.equals(""))
				menuName = getString(R.string.default_group);
		}

		// display contact menu for contacts
		menu.setGroupVisible(R.id.roster_contextmenu_contact_menu, isChild);
		// display group menu for non-standard group
		menu.setGroupVisible(R.id.roster_contextmenu_group_menu, !isChild
				&& (menuName.length() > 0));

		menu.setHeaderTitle(getString(R.string.roster_contextmenu_title,
				menuName));
	}

	private boolean isChild(long packedPosition) {
		int type = ExpandableListView.getPackedPositionType(packedPosition);
		return (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD);
	}

	private boolean applyMenuContextChoice(MenuItem item) {

		ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		long packedPosition = contextMenuInfo.packedPosition;

		if (isChild(packedPosition)) {

			String userJid = getPackedItemRow(packedPosition,
					RosterConstants.JID);
			String userName = getPackedItemRow(packedPosition,
					RosterConstants.ALIAS);
			Log.d(TAG, "action for contact " + userName + "/" + userJid);

			int itemID = item.getItemId();

			switch (itemID) {
			case R.id.roster_contextmenu_contact_delmsg:
				mMainActivity.removeChatHistoryDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_delete:
				if (!mMainActivity.isConnected()) {
					mMainActivity
							.showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				mMainActivity.removeRosterItemDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_rename:
				if (!mMainActivity.isConnected()) {
					mMainActivity
							.showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				mMainActivity.renameRosterItemDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_request_auth:
				if (!mMainActivity.isConnected()) {
					mMainActivity
							.showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				mMainActivity.serviceAdapter
						.sendPresenceRequest(userJid,"subscribe");
				return true;

			case R.id.roster_contextmenu_contact_change_group:
				if (!mMainActivity.isConnected()) {
					mMainActivity
							.showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				moveRosterItemToGroupDialog(userJid);
				return true;
			}
		} else {

			int itemID = item.getItemId();
			String seletedGroup = getPackedItemRow(packedPosition,
					RosterConstants.GROUP);
			Log.d(TAG, "action for group " + seletedGroup);

			switch (itemID) {
			case R.id.roster_contextmenu_group_rename:
				if (!mMainActivity.isConnected()) {
					mMainActivity
							.showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				mMainActivity.renameRosterGroupDialog(seletedGroup);
				return true;

			}
		}
		return false;
	}

	public boolean onContextItemSelected(MenuItem item) {
		return applyMenuContextChoice(item);
	}

	private void registerListAdapter() {
		rosterListAdapter = new RosterExpListAdapter(mMainActivity);
		mListView.setAdapter(rosterListAdapter);
	}

	public class RosterExpListAdapter extends SimpleCursorTreeAdapter {

		public RosterExpListAdapter(Context context) {
			super(context, /* cursor = */null, R.layout.maingroup_row,
					GROUPS_FROM, GROUPS_TO, R.layout.mainchild_row,
					new String[] { RosterConstants.ALIAS,
							RosterConstants.STATUS_MESSAGE,
							RosterConstants.STATUS_MODE }, new int[] {
							R.id.roster_screenname, R.id.roster_statusmsg,
							R.id.roster_icon });
		}

		public void requery() {
			String selectWhere = null;
			if (!mMainActivity.showOffline)
				selectWhere = OFFLINE_EXCLUSION;
			Cursor cursor = mMainActivity.getContentResolver().query(
					RosterProvider.GROUPS_URI, GROUPS_QUERY_COUNTED,
					selectWhere, null, RosterConstants.GROUP);
			Cursor oldCursor = getCursor();
			changeCursor(cursor);
			mMainActivity.stopManagingCursor(oldCursor);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			// Given the group, we return a cursor for all the children within
			// that group
			int idx = groupCursor.getColumnIndex(RosterConstants.GROUP);
			String groupname = groupCursor.getString(idx);

			String selectWhere = RosterConstants.GROUP + " = ?";
			if (!mMainActivity.showOffline)
				selectWhere += " AND " + OFFLINE_EXCLUSION;
			return mMainActivity.getContentResolver().query(
					RosterProvider.CONTENT_URI, ROSTER_QUERY, selectWhere,
					new String[] { groupname }, null);
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			super.bindGroupView(view, context, cursor, isExpanded);
			if (cursor.getString(
					cursor.getColumnIndexOrThrow(RosterConstants.GROUP))
					.length() == 0) {
				TextView groupname = (TextView) view
						.findViewById(R.id.groupname);
				groupname.setText(R.string.default_group);
			}
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor,
				boolean isLastChild) {
			super.bindChildView(view, context, cursor, isLastChild);
			TextView statusmsg = (TextView) view
					.findViewById(R.id.roster_statusmsg);
			boolean hasStatus = statusmsg.getText() != null
					&& statusmsg.getText().length() > 0;
			statusmsg.setVisibility(hasStatus ? View.VISIBLE : View.GONE);

			int JIDIdx = cursor.getColumnIndex(RosterConstants.JID);
			String selection = ChatConstants.JID + " = '"
					+ cursor.getString(JIDIdx) + "' AND "
					+ ChatConstants.DIRECTION + " = " + ChatConstants.INCOMING
					+ " AND " + ChatConstants.DELIVERY_STATUS + " = "
					+ ChatConstants.DS_NEW;
			Cursor msgcursor = mMainActivity.getContentResolver().query(
					ChatProvider.CONTENT_URI,
					new String[] { "count(" + ChatConstants.PACKET_ID + ")" },
					selection, null, null);
			msgcursor.moveToFirst();
			TextView unreadmsg = (TextView) view
					.findViewById(R.id.roster_unreadmsg_cnt);
			unreadmsg.setText(msgcursor.getString(0));
			unreadmsg.setVisibility(msgcursor.getInt(0) > 0 ? View.VISIBLE
					: View.GONE);
			unreadmsg.bringToFront();
			msgcursor.close();
		}

		protected void setViewImage(ImageView v, String value) {
			int presenceMode = Integer.parseInt(value);
			v.setImageResource(getIconForPresenceMode(presenceMode));
		}

		private int getIconForPresenceMode(int presenceMode) {
			if(presenceMode<StatusMode.values().length){
				return StatusMode.values()[presenceMode].getDrawableId();
			}else{
				return R.drawable.ic_launcher;
			}			
		}
	}

	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(getHandler());
		}

		public void onChange(boolean selfChange) {
			Log.d(TAG, "RosterObserver.onChange: " + selfChange);
			if (rosterListAdapter != null)
				getHandler().postDelayed(new Runnable() {
					public void run() {
						restoreGroupsExpanded();
					}
				}, 100);
		}
	}

	private class ChatObserver extends ContentObserver {
		public ChatObserver() {
			super(getHandler());
		}

		public void onChange(boolean selfChange) {
			updateRoster();
		}
	}

	public List<String> getRosterGroups() {
		// we want all, online and offline
		List<String> list = new ArrayList<String>();
		Cursor cursor = mMainActivity.getContentResolver().query(
				RosterProvider.GROUPS_URI, GROUPS_QUERY, null, null,
				RosterConstants.GROUP);
		int idx = cursor.getColumnIndex(RosterConstants.GROUP);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(idx));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public List<String[]> getRosterContacts() {
		// we want all, online and offline
		List<String[]> list = new ArrayList<String[]>();
		Cursor cursor = mMainActivity.getContentResolver().query(
				RosterProvider.CONTENT_URI, ROSTER_QUERY, null, null,
				RosterConstants.ALIAS);
		int JIDIdx = cursor.getColumnIndex(RosterConstants.JID);
		int aliasIdx = cursor.getColumnIndex(RosterConstants.ALIAS);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String jid = cursor.getString(JIDIdx);
			String alias = cursor.getString(aliasIdx);
			if ((alias == null) || (alias.length() == 0))
				alias = jid;
			list.add(new String[] { jid, alias });
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public void handleJabberIntent() {
		Intent intent = mMainActivity.getIntent();
		String action = intent.getAction();
		Uri data = intent.getData();
		if ((action != null) && (action.equals(Intent.ACTION_SENDTO))
				&& data != null && data.getHost().equals("jabber")) {
			String jid = data.getPathSegments().get(0);
			Log.d(TAG, "handleJabberIntent: " + jid);

			List<String[]> contacts = getRosterContacts();
			for (String[] c : contacts) {
				if (jid.equalsIgnoreCase(c[0])) {
					// found it
					mMainActivity.startChatActivity(c[0], c[1], null);
					mMainActivity.finish();
					return;
				}
			}
			// did not find in roster, try to add
			if (mMainActivity.serviceAdapter != null
					&& mMainActivity.serviceAdapter.isAuthenticated()) {
				new AddRosterItemDialog(this, mMainActivity.serviceAdapter, jid)
						.show();
			} else {
				mMainActivity
						.showToastNotification(R.string.Global_authenticate_first);
				mMainActivity.finish();
			}
		}
	}

	void moveRosterItemToGroupDialog(final String jabberID) {
		LayoutInflater inflater = (LayoutInflater) mMainActivity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View group = inflater.inflate(R.layout.moverosterentrytogroupview,
				null, false);
		final GroupNameView gv = (GroupNameView) group
				.findViewById(R.id.moverosterentrytogroupview_gv);
		gv.setGroupList(getRosterGroups());
		new AlertDialog.Builder(mMainActivity)
				.setTitle(R.string.MoveRosterEntryToGroupDialog_title)
				.setView(group)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								Log.d(TAG, "new group: " + gv.getGroupName());
								mMainActivity.serviceAdapter
										.moveRosterItemToGroup(jabberID,
												gv.getGroupName());
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}
}
