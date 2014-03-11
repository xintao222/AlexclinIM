package alexclin.ui;

import java.util.ArrayList;
import java.util.List;

import umeox.xmpp.aidl.IXMPPRosterCallback;
import umeox.xmpp.aidl.IXMPPRosterService;
import umeox.xmpp.aidl.XMPPRosterServiceAdapter;
import umeox.xmpp.base.BaseApp;
import umeox.xmpp.base.BaseConfig;
import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.util.ConnectionState;
import umeox.xmpp.util.LogUtil;
import umeox.xmpp.util.PrefConsts;
import alexclin.base.JimService;
import alexclin.base.StatusMode;
import alexclin.dialogs.AddRosterItemDialog;
import alexclin.dialogs.ChangeStatusDialog;
import alexclin.ui.login.LoginActivity;
import alexclin.ui.preferences.MainPrefs;
import alexclin.xmpp.jabberim.R;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;

public class MainTabActivity extends SherlockFragmentActivity implements
		OnClickListener, OnPageChangeListener {

	private static final String TAG = "alexclin.MainWindow";
	private Handler mainHandler = new Handler();
	private BaseConfig mConfig;

	private Intent xmppServiceIntent;
	private ServiceConnection xmppServiceConnection;
	XMPPRosterServiceAdapter serviceAdapter;
	private IXMPPRosterCallback.Stub rosterCallback;

	private TextView mConnectingText;
	boolean showOffline;

	private String mStatusMessage;
	private StatusMode mStatusMode;

	private ActionBar actionBar;
	private String mTheme;

	private ViewPager mViewPager;
	private FragPagerAdapter adapter;
	/** 对话Tab */
	private TextView mChatTabTv;
	/** 好友Tab */
	private TextView mFriendTabTv;
	/** 群组Tab */
	private TextView mGroupTabTv;
	/** 个人Tab */
	private TextView mPersonalTabTv;
	/** Tab项底部标记Image */
	private ImageView mActiveLineIv;
	/** 当前页码 */
	private int currIndex = 0;
	private int mPos1 = 0;
	private int mPos2;
	private int mPos3;
	private int mPos4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mConfig = ((BaseApp) getApplication()).getConfig();
		mTheme = mConfig.theme;
		setTheme(mConfig.getTheme());
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE,
				ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setHomeButtonEnabled(true);
		initXMPPServiceIntent();
		createUICallback();
		initViews();
		actionBar.setSubtitle(mStatusMessage);
	}

	@Override
	public void onDestroy() {
		LogUtil.e(this, "onDestroy");
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (serviceAdapter != null)
			serviceAdapter.unregisterUICallback(rosterCallback);
		unbindXMPPService();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferences(PreferenceManager.getDefaultSharedPreferences(this));
		String theme = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(PrefConsts.THEME, "dark");
		if (theme.equals(mTheme) == false) {
			// restart
			Intent restartIntent = new Intent(this, MainTabActivity.class);
			restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(restartIntent);
			finish();
		}
		bindXMPPService();
		// handle SEND action
		handleSendIntent();
	}

	void initViews() {
		setContentView(R.layout.act_maintab);
		mConnectingText = (TextView) findViewById(R.id.error_view);
		mViewPager = (ViewPager) findViewById(R.id.ViewPager_MainTab);
		List<Fragment> list = new ArrayList<Fragment>();
		list.add(new ChatsFragment());
		list.add(new FriendsFragment());
		list.add(new GroupsFragment());
		list.add(new PersonalFragment());
		adapter = new FragPagerAdapter(getSupportFragmentManager(), list);
		mViewPager.setAdapter(adapter);
		mViewPager.setOnPageChangeListener(this);

		mChatTabTv = (TextView) findViewById(R.id.ChatsTab_MainTab);
		mFriendTabTv = (TextView) findViewById(R.id.FriendsTab_MainTab);
		mGroupTabTv = (TextView) findViewById(R.id.GroupsTab_MainTab);
		mPersonalTabTv = (TextView) findViewById(R.id.PersonalTab_MainTab);
		mActiveLineIv = (ImageView) findViewById(R.id.ActiveLineIv_MainTab);
		initWidth();
		mChatTabTv.setOnClickListener(this);
		mFriendTabTv.setOnClickListener(this);
		mGroupTabTv.setOnClickListener(this);
		mPersonalTabTv.setOnClickListener(this);
	}

	private void initWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;
		int width = screenW / 3;
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				mActiveLineIv.getLayoutParams());
		lp.width = width;
		mActiveLineIv.setLayoutParams(lp);
		mPos2 = (int) (screenW / 3.0);
		mPos3 = (int) (screenW * 2 / 3.0);
		mPos4 = (int) (screenW * 3 / 4.0);
	}

	public int getStatusActionIcon() {
		boolean showOffline = !isConnected() || isConnecting()
				|| getStatusMode() == null;
		if (showOffline) {
			return StatusMode.offline.getDrawableId();
		}
		return getStatusMode().getDrawableId();
	}

	public void handleSendIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();
		if ((action != null) && (action.equals(Intent.ACTION_SEND))) {
			showToastNotification(R.string.chooseContact);
			setTitle(R.string.chooseContact);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	boolean isConnected() {
		return serviceAdapter != null && serviceAdapter.isAuthenticated();
	}

	private boolean isConnecting() {
		return serviceAdapter != null
				&& serviceAdapter.getConnectionState() == ConnectionState.CONNECTING.ordinal();
	}

	void removeChatHistory(final String JID) {
		getContentResolver().delete(ChatProvider.CONTENT_URI,
				ChatProvider.ChatConstants.JID + " = ?", new String[] { JID });
	}

	void removeChatHistoryDialog(final String JID, final String userName) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.deleteChatHistory_title)
				.setMessage(
						getString(R.string.deleteChatHistory_text, userName,
								JID))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								removeChatHistory(JID);
							}
						}).setNegativeButton(android.R.string.no, null)
				.create().show();
	}

	void removeRosterItemDialog(final String JID, final String userName) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.deleteRosterItem_title)
				.setMessage(
						getString(R.string.deleteRosterItem_text, userName, JID))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								serviceAdapter.removeRosterItem(JID);
							}
						}).setNegativeButton(android.R.string.no, null)
				.create().show();
	}

	abstract class EditOk {
		abstract public void ok(String result);
	}

	void editTextDialog(int titleId, CharSequence message, String text,
			final EditOk ok) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_edittext,
				(ViewGroup) findViewById(R.id.layout_root));

		TextView messageView = (TextView) layout.findViewById(R.id.text);
		messageView.setText(message);
		final EditText input = (EditText) layout.findViewById(R.id.editText);
		input.setTransformationMethod(android.text.method.SingleLineTransformationMethod
				.getInstance());
		input.setText(text);
		new AlertDialog.Builder(this)
				.setTitle(titleId)
				.setView(layout)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								String newName = input.getText().toString();
								if (newName.length() != 0)
									ok.ok(newName);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}

	void renameRosterItemDialog(final String JID, final String userName) {
		editTextDialog(R.string.RenameEntry_title,
				getString(R.string.RenameEntry_summ, userName, JID), userName,
				new EditOk() {
					public void ok(String result) {
						serviceAdapter.renameRosterItem(JID, result);
					}
				});
	}

	void renameRosterGroupDialog(final String groupName) {
		editTextDialog(R.string.RenameGroup_title,
				getString(R.string.RenameGroup_summ, groupName), groupName,
				new EditOk() {
					public void ok(String result) {
						serviceAdapter.renameRosterGroup(groupName, result);
					}
				});
	}

	void startChatActivity(String user, String userName, String message) {
		Intent chatIntent = new Intent(this,
				alexclin.ui.chat.ChatActivity.class);
		Uri userNameUri = Uri.parse(user);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(
				alexclin.ui.chat.ChatActivity.INTENT_EXTRA_USERNAME, userName);
		if (message != null) {
			chatIntent
					.putExtra(
							alexclin.ui.chat.ChatActivity.INTENT_EXTRA_MESSAGE,
							message);
		}
		startActivity(chatIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.roster_options, menu);
		actionBar.setIcon(getStatusActionIcon());
		return true;
	}

	void setMenuItem(Menu menu, int itemId, int iconId, CharSequence title) {
		com.actionbarsherlock.view.MenuItem item = menu.findItem(itemId);
		if (item == null)
			return;
		item.setIcon(iconId);
		item.setTitle(title);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		setMenuItem(menu, R.id.menu_connect, getConnectDisconnectIcon(),
				getConnectDisconnectText());
		setMenuItem(menu, R.id.menu_show_hide, getShowHideMenuIcon(),
				getShowHideMenuText());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		return applyMainMenuChoice(item);
	}

	private int getShowHideMenuIcon() {
		TypedValue tv = new TypedValue();
		if (showOffline) {
			getTheme().resolveAttribute(R.attr.OnlineFriends, tv, true);
			return tv.resourceId;
		}
		getTheme().resolveAttribute(R.attr.AllFriends, tv, true);
		return tv.resourceId;
	}

	private String getShowHideMenuText() {
		return showOffline ? getString(R.string.Menu_HideOff)
				: getString(R.string.Menu_ShowOff);
	}

	public Handler getHadnler() {
		return mainHandler;
	}

	public StatusMode getStatusMode() {
		return mStatusMode;
	}

	public String getStatusMessage() {
		return mStatusMessage;
	}

	public int getAccountPriority() {
		return mConfig.priority;
	}

	public static String getStatusTitle(Context context, String status,
			String statusMessage) {
		status = context.getString(StatusMode.fromString(status).getTextId());

		if (statusMessage.length() > 0) {
			status = status + " (" + statusMessage + ")";
		}

		return status;
	}

	public void setAndSaveStatus(StatusMode statusMode, String message,
			int priority) {
		setStatus(statusMode, message);

		SharedPreferences.Editor prefedit = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		// do not save "offline" to prefs, or else!
		if (statusMode != StatusMode.offline)
			prefedit.putString(PrefConsts.STATUS_MODE, statusMode.name());
		prefedit.putString(PrefConsts.STATUS_MESSAGE, message);
		prefedit.putString(PrefConsts.PRIORITY, String.valueOf(priority));
		prefedit.commit();

		// check if we are connected and want to go offline
		boolean needToDisconnect = (statusMode == StatusMode.offline)
				&& isConnected();
		// check if we want to reconnect
		boolean needToConnect = (statusMode != StatusMode.offline)
				&& serviceAdapter.getConnectionState() == ConnectionState.OFFLINE.ordinal();

		if (needToConnect || needToDisconnect)
			toggleConnection();
		else if (isConnected())
			serviceAdapter.setStatusFromConfig();
	}

	private void setStatus(StatusMode statusMode, String message) {
		mStatusMode = statusMode;
		mStatusMessage = message;

		// This and many other things like it should be done with observer
		actionBar.setIcon(getStatusActionIcon());

		if (mStatusMessage.equals("")) {
			actionBar.setSubtitle(null);
		} else {
			actionBar.setSubtitle(mStatusMessage);
		}
	}

	private void aboutDialog() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View about = inflater.inflate(R.layout.aboutview, null, false);
		String versionTitle = getString(R.string.AboutDialog_title);
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			versionTitle += " v" + pi.versionName;
		} catch (NameNotFoundException e) {
		}

		new AlertDialog.Builder(this)
				.setTitle(versionTitle)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView(about)
				.setPositiveButton(android.R.string.ok, null)
				.setNeutralButton(R.string.AboutDialog_Vote,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								Intent market = new Intent(Intent.ACTION_VIEW,
										Uri.parse("market://details?id="
												+ getPackageName()));
								market.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
										| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								try {
									startActivity(market);
								} catch (Exception e) {
									Log.e(TAG, "could not go to market: " + e);
								}
							}
						}).create().show();
	}

	private boolean applyMainMenuChoice(com.actionbarsherlock.view.MenuItem item) {

		int itemID = item.getItemId();

		switch (itemID) {
		case R.id.menu_connect:
			toggleConnection();
			return true;

		case R.id.menu_add_friend:
			if (serviceAdapter.isAuthenticated()) {
				new AddRosterItemDialog((FriendsFragment) adapter.getItem(1),
						serviceAdapter).show();
			} else {
				showToastNotification(R.string.Global_authenticate_first);
			}
			return true;

		case R.id.menu_show_hide:
			setOfflinceContactsVisibility(!showOffline);
			((FriendsFragment) adapter.getItem(1)).updateRoster();
			return true;

		case android.R.id.home:
			new ChangeStatusDialog(this).show();
			return true;
		case R.id.menu_logout:// 注销
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putString(PrefConsts.PASSWORD, "").commit();
			startActivity(new Intent(this.getApplicationContext(), LoginActivity.class));			
		case R.id.menu_exit:// 退出
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putBoolean(PrefConsts.CONN_STARTUP, false).commit();
			stopService(xmppServiceIntent);
			finish();						
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, MainPrefs.class));
			return true;
		case R.id.menu_about:
			aboutDialog();
			return true;
		}

		return false;

	}

	/** Sets if all contacts are shown in the roster or online contacts only. */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	// required for Sherlock's invalidateOptionsMenu */
	private void setOfflinceContactsVisibility(boolean showOffline) {
		this.showOffline = showOffline;
		invalidateOptionsMenu();

		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean(PrefConsts.SHOW_OFFLINE, showOffline).commit();
	}

	private void setConnectingStatus(boolean isConnecting) {

		String lastStatus;

		if (serviceAdapter != null
				&& !serviceAdapter.isAuthenticated()
				&& (lastStatus = serviceAdapter.getConnectionStateString()) != null) {
			mConnectingText.setVisibility(View.VISIBLE);
			mConnectingText.setText(lastStatus);
		} else if (serviceAdapter == null
				|| serviceAdapter.isAuthenticated() == false) {
			mConnectingText.setVisibility(View.VISIBLE);
			mConnectingText.setText(R.string.conn_offline);
		} else
			mConnectingText.setVisibility(View.GONE);
	}

	public void startConnection(boolean create_account) {
		setConnectingStatus(true);
		xmppServiceIntent.putExtra("create_account", create_account);
		startService(xmppServiceIntent);
	}

	// this function changes the prefs to keep the connection
	// according to the requested state
	private void toggleConnection() {
		boolean oldState = isConnected() || isConnecting();
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean(PrefConsts.CONN_STARTUP, !oldState).commit();
		setSupportProgressBarIndeterminateVisibility(true);
		if (oldState) {
			setConnectingStatus(false);
			(new Thread() {
				public void run() {
					serviceAdapter.disconnect();
					stopService(xmppServiceIntent);
				}
			}).start();

		} else
			startConnection(false);
	}

	private int getConnectDisconnectIcon() {
		if (isConnected() || isConnecting()) {
			return R.drawable.ic_menu_unplug;
		}
		return R.drawable.ic_menu_plug;
	}

	private String getConnectDisconnectText() {
		if (isConnected() || isConnecting()) {
			return getString(R.string.Menu_disconnect);
		}
		return getString(R.string.Menu_connect);
	}

	private void initXMPPServiceIntent() {
		Log.i(TAG, "init XMPPService intent");
		xmppServiceIntent = new Intent(this, JimService.class);
		xmppServiceIntent.setAction("org.yaxim.androidclient.XMPPSERVICE");
		xmppServiceConnection = new ServiceConnection() {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			// required for Sherlock's invalidateOptionsMenu */
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				serviceAdapter = new XMPPRosterServiceAdapter(
						IXMPPRosterService.Stub.asInterface(service));
				serviceAdapter.registerUICallback(rosterCallback);
				Log.i(TAG,
						"getConnectionState(): "
								+ serviceAdapter.getConnectionState());
				invalidateOptionsMenu();
				actionBar.setIcon(getStatusActionIcon());
				// TODO 顶部显示用户名
				// actionBar.setTitle(serviceAdapter.);
				actionBar.setTitle(PreferenceManager
						.getDefaultSharedPreferences(MainTabActivity.this)
						.getString(PrefConsts.JID, ""));
				setConnectingStatus(serviceAdapter.getConnectionState() == ConnectionState.CONNECTING.ordinal());
				setSupportProgressBarIndeterminateVisibility(serviceAdapter
						.getConnectionState() == ConnectionState.CONNECTING.ordinal());
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}
		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(xmppServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(xmppServiceIntent, xmppServiceConnection, BIND_AUTO_CREATE);
	}

	private void createUICallback() {
		rosterCallback = new IXMPPRosterCallback.Stub() {
			@Override
			public void connectionStateChanged(final int connectionstate) throws RemoteException {
				mainHandler.post(new Runnable() {
					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					// required for Sherlock's invalidateOptionsMenu */
					public void run() {
						boolean isConnected = connectionstate==ConnectionState.ONLINE.ordinal();
						setConnectingStatus(!isConnected);
						setSupportProgressBarIndeterminateVisibility(false);
						invalidateOptionsMenu();
					}
				});
			}
		};
	}

	private void getPreferences(SharedPreferences prefs) {
		showOffline = prefs.getBoolean(PrefConsts.SHOW_OFFLINE, true);

		setStatus(StatusMode.fromString(prefs.getString(PrefConsts.STATUS_MODE,
				StatusMode.available.name())), prefs.getString(
				PrefConsts.STATUS_MESSAGE, ""));
	}

	public static Intent createIntent(Context context) {
		Intent i = new Intent(context, MainTabActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}

	protected void showToastNotification(int message) {
		Toast tmptoast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		tmptoast.show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ChatsTab_MainTab:
			mViewPager.setCurrentItem(0);
			break;
		case R.id.FriendsTab_MainTab:
			mViewPager.setCurrentItem(1);
			break;
		case R.id.GroupsTab_MainTab:
			mViewPager.setCurrentItem(2);
			break;
		case R.id.PersonalTab_MainTab:
			mViewPager.setCurrentItem(3);
			break;
		}

	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int arg0) {
		Resources mResources = getResources();
		getCurTabTv(currIndex).setTextColor(
				mResources.getColor(android.R.color.black));
		getCurTabTv(arg0).setTextColor(mResources.getColor(R.color.tab_green));
		Animation animation = new TranslateAnimation(getCurPos(currIndex),
				getCurPos(arg0), 0, 0);
		currIndex = arg0;
		animation.setFillAfter(true);
		animation.setDuration(300);
		mActiveLineIv.startAnimation(animation);
	}

	private int getCurPos(int index) {
		switch (index) {
		case 1:
			return mPos2;
		case 2:
			return mPos3;
		case 3:
			return mPos4;
		default:
			return mPos1;
		}
	}

	private TextView getCurTabTv(int index) {
		switch (index) {
		case 1:
			return mFriendTabTv;
		case 2:
			return mGroupTabTv;
		case 3:
			return mPersonalTabTv;
		default:
			return mChatTabTv;
		}
	}

}
