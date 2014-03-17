package umeox.xmpp.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import umeox.xmpp.aidl.IXMPPChatService;
import umeox.xmpp.aidl.IXMPPRosterCallback;
import umeox.xmpp.aidl.IXMPPRosterService;
import umeox.xmpp.aidl.XMPPServiceCallback;
import umeox.xmpp.base.BaseApp;
import umeox.xmpp.base.BaseConfig;
import umeox.xmpp.base.UmeoxException;
import umeox.xmpp.util.ConnectionState;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.lidroid.xutils.util.LogUtils;

public abstract class XMPPService extends Service {
	public static final int Conn_Connecting = 1;
	public static final int Conn_Online = 2;
	public static final int Conn_Offline = 3;
	public static final int Conn_Empty_Roster = 4;
	public static final int Conn_Reconnect = 5;
	public static final int Conn_Disconnected = 6;
	public static final int Conn_Networkchg = 7;
	public static final int Conn_No_Network = 8;
	
	protected static final int RECONNECT_AFTER = 5;
	protected static final int RECONNECT_MAXIMUM = 10 * 60;
	protected static final String RECONNECT_ALARM = "xmpp.android.service.RECONNECT_ALARM";
	
	protected static final int MAX_TICKER_MSG_LEN = 50;
	protected static final int SERVICE_NOTIFICATION = 1;

	protected AtomicBoolean mIsConnected = new AtomicBoolean(false);
	protected AtomicBoolean mConnectionDemanded = new AtomicBoolean(false); //是否尝试重新连接
	protected Map<String, Integer> notificationCount = new HashMap<String, Integer>(2);
	protected Map<String, Integer> notificationId = new HashMap<String, Integer>(2);
	
	protected String mLastConnectionError = null;
	protected String mReconnectInfo = "";
	protected Notification mNotification;
	protected BaseConfig mConfig;
	private Smackable mSmackable;
	private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
	private PendingIntent mPAlarmIntent;
	private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
	private int mReconnectTimeout = RECONNECT_AFTER;

	private NotificationManager mNotificationMGR;
	private WakeLock mWakeLock;
	
	private Vibrator mVibrator;	
	private int lastNotificationId = 2;
	private Thread mConnectingThread;
	private boolean create_account = false;
	private IXMPPRosterService.Stub mServiceRosterConnection;
	private IXMPPChatService.Stub mServiceChatConnection;
	private RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private HashSet<String> mIsBoundTo = new HashSet<String>();
	private Handler mMainHandler = new Handler();

	@Override
	public void onCreate() {
		LogUtils.i("Create");
		super.onCreate();
		mConfig = ((BaseApp) getApplication()).getConfig();
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getSimpleName());
		addNotificationMGR();
	
		createServiceRosterStub();
		createServiceChatStub();
	
		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));	
		
		mConnectionDemanded.set(mConfig.autoConnect);
		XmppReceiver.initNetworkStatus(getApplicationContext());
	
		if (mConfig.autoConnect) {
			/*调用Start服务以保证unbind后服务仍在后台运行*/
			Intent xmppServiceIntent = new Intent(this, XMPPService.class);
			startService(xmppServiceIntent);
		}
	
		mNotificationMGR = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if(mConfig.foregroundService){
			showNotification(this, SERVICE_NOTIFICATION, getForeNotification());
		}		
	}

	@Override
	public void onDestroy() {
		LogUtils.i("Destroy");
		super.onDestroy();
		((AlarmManager) getSystemService(Context.ALARM_SERVICE))
				.cancel(mPAlarmIntent);
		mRosterCallbacks.kill();
		performDisconnect();
		unregisterReceiver(mAlarmReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogUtils.i("onStartCommand(), mConnectionDemanded="
				+ mConnectionDemanded.get());
		if (intent != null) {
			create_account = intent.getBooleanExtra("create_account", false);
			if ("disconnect".equals(intent.getAction())) {
				if (mConnectingThread != null || mIsConnected.get())
					connectionFailed(getConnectStr(Conn_Networkchg,0));
				return START_STICKY;
			} else if ("reconnect".equals(intent.getAction())) {
				// reset reconnection timeout
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
				return START_STICKY;
			} else if ("ping".equals(intent.getAction())) {
				if (mSmackable != null && mSmackable.isAuthenticated())
					mSmackable.sendServerPing();
				return START_STICKY;
			}
		}
	
		mConnectionDemanded.set(mConfig.autoConnect);
		doConnect();
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			resetNotificationCounter(chatPartner);
			mIsBoundTo.add(chatPartner);
			return mServiceChatConnection;
		}
		return mServiceRosterConnection;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.add(chatPartner);
			resetNotificationCounter(chatPartner);
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.remove(chatPartner);
		}
		return true;
	}

	private void shortToastNotify(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	private void notifyClient(String fromJid, String fromUserName,
			String message, boolean showNotification,
			boolean silent_notification, boolean is_error) {
		if (!showNotification) {
			if (is_error)
				shortToastNotify(getConnectStr(Conn_Disconnected,0) + " "
						+ message);
			// only play sound and return
			try {
				if (!silent_notification)
					RingtoneManager.getRingtone(getApplicationContext(),
							mConfig.notifySound).play();
			} catch (NullPointerException e) {
				// ignore NPE when ringtone was not found
			}
			return;
		}
		mWakeLock.acquire();
	
		// Override silence when notification is created initially
		// if there is no open notification for that JID, and we get a "silent"
		// one (i.e. caused by an incoming carbon message), we still
		// ring/vibrate,
		// but only once. As long as the user ignores the notifications, no more
		// sounds are made. When the user opens the chat window, the counter is
		// reset and a new sound can be made.
		if (silent_notification && !notificationCount.containsKey(fromJid)) {
			silent_notification = false;
		}
	
		setNotification(fromJid, fromUserName, message, is_error);
		setLEDNotification();
		if (!silent_notification)
			mNotification.sound = mConfig.notifySound;
	
		int notifyId = 0;
		if (notificationId.containsKey(fromJid)) {
			notifyId = notificationId.get(fromJid);
		} else {
			lastNotificationId++;
			notifyId = lastNotificationId;
			notificationId.put(fromJid, Integer.valueOf(notifyId));
		}
	
		// If vibration is set to "system default", add the vibration flag to
		// the
		// notification and let the system decide.
		if (!silent_notification && "SYSTEM".equals(mConfig.vibraNotify)) {
			mNotification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		mNotificationMGR.notify(notifyId, mNotification);
	
		// If vibration is forced, vibrate now.
		if (!silent_notification && "ALWAYS".equals(mConfig.vibraNotify)) {
			mVibrator.vibrate(400);
		}
		mWakeLock.release();
	}
	
	public void showNotification(Service context, int id, Notification n) {
		Log.d("EclairAndBeyond", "showNotification " + id + " " + n);
		context.startForeground(id, n);
	}
	public void hideNotification(Service context, int id) {
		Log.d("EclairAndBeyond", "hideNotification");
		context.stopForeground(true);
	}

	private void resetNotificationCounter(String userJid) {
		notificationCount.remove(userJid);
	}

	private void setLEDNotification() {
		if (mConfig.isLEDNotify) {
			mNotification.ledARGB = Color.MAGENTA;
			mNotification.ledOnMS = 300;
			mNotification.ledOffMS = 1000;
			mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
		}
	}

	private void clearNotification(String Jid) {
		int notifyId = 0;
		if (notificationId.containsKey(Jid)) {
			notifyId = notificationId.get(Jid);
			mNotificationMGR.cancel(notifyId);
		}
	}

	private void createServiceChatStub() {
		mServiceChatConnection = new IXMPPChatService.Stub() {
			public void sendMessage(String user, String message)
					throws RemoteException {
				if (mSmackable != null)
					mSmackable.sendMessage(user, message);
				else
					SmackableImp.sendOfflineMessage(getContentResolver(), user,
							message);
			}

			public boolean isAuthenticated() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.isAuthenticated();
				}
				return false;
			}

			public void clearNotifications(String Jid) throws RemoteException {
				clearNotification(Jid);
			}
		};
	}

	private void createServiceRosterStub() {
		mServiceRosterConnection = new IXMPPRosterService.Stub() {
			public void registerRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.register(callback);
			}

			public void unregisterRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.unregister(callback);
			}

			public int getConnectionState() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.getConnectionState().ordinal();
				} else {
					return ConnectionState.OFFLINE.ordinal();
				}
			}

			public String getConnectionStateString() throws RemoteException {
				return XMPPService.this.getConnectionStateString();
			}

			public void setStatusFromConfig() throws RemoteException {
				if (mSmackable != null) { // this should always be true, but
											// stil...
					mSmackable.setStatusFromConfig();
					updateForeNotification();
				}
			}

			public void addRosterItem(String user, String alias, String group,
					String msg) throws RemoteException {
				try {
					mSmackable.addRosterItem(user, alias, group, msg);
				} catch (UmeoxException e) {
					shortToastNotify(e.getMessage());
					LogUtils.e("exception in addRosterItem()", e);
				}
			}

			public void addRosterGroup(String group) throws RemoteException {
				mSmackable.addRosterGroup(group);
			}

			public void removeRosterItem(String user) throws RemoteException {
				try {
					mSmackable.removeRosterItem(user);
				} catch (UmeoxException e) {
					shortToastNotify(e.getMessage());
					LogUtils.e("exception in removeRosterItem()",e);
				}
			}

			public void moveRosterItemToGroup(String user, String group)
					throws RemoteException {
				try {
					mSmackable.moveRosterItemToGroup(user, group);
				} catch (UmeoxException e) {
					shortToastNotify(e.getMessage());
					LogUtils.e("exception in moveRosterItemToGroup()",e);
				}
			}

			public void renameRosterItem(String user, String newName)
					throws RemoteException {
				try {
					mSmackable.renameRosterItem(user, newName);
				} catch (UmeoxException e) {
					shortToastNotify(e.getMessage());
					LogUtils.e("exception in renameRosterItem()",e);
				}
			}

			public void renameRosterGroup(String group, String newGroup)
					throws RemoteException {
				mSmackable.renameRosterGroup(group, newGroup);
			}

			public void disconnect() throws RemoteException {
				manualDisconnect();
			}

			public void connect() throws RemoteException {
				mConnectionDemanded.set(true);
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
			}

			public void sendPresenceRequest(String jid, String type)
					throws RemoteException {
				mSmackable.sendPresenceRequest(jid, type);
			}

		};
	}

	private void createAdapter() {
		System.setProperty("smack.debugEnabled", "" + mConfig.smackdebug);
		try {
			mSmackable = new SmackableImp(mConfig, getContentResolver(), this);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		mSmackable.registerCallback(new XmppServiceCallbackImp());
	}

	private void doConnect() {
		mReconnectInfo = getConnectStr(Conn_Connecting,0);
		updateForeNotification();
		if (mSmackable == null) {
			createAdapter();
		}
		mSmackable.requestConnectionState(ConnectionState.ONLINE,
				create_account);
	}

	private NetworkInfo getNetworkInfo() {
		Context ctx = getApplicationContext();
		ConnectivityManager connMgr = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connMgr.getActiveNetworkInfo();
	}

	private boolean networkConnected() {
		NetworkInfo info = getNetworkInfo();
		return info != null && info.isConnected();
	}

	private void manualDisconnect() {
		mConnectionDemanded.set(false);
		performDisconnect();
	}

	private void performDisconnect() {
		if (mConnectingThread != null) {
			synchronized (mConnectingThread) {
				try {
					mConnectingThread.interrupt();
					mConnectingThread.join(50);
				} catch (InterruptedException e) {
					LogUtils.i("doDisconnect: failed catching connecting thread");
				} finally {
					mConnectingThread = null;
				}
			}
		}
		if (mSmackable != null) {
			mSmackable.unRegisterCallback();
			mSmackable = null;
		}
		connectionFailed(getConnectStr(Conn_Offline,0));
        hideNotification(this, SERVICE_NOTIFICATION);
	}

	private void connectionFailed(String reason) {
		LogUtils.i("connectionFailed: " + reason);
		// mLastConnectionError = reason;
		if (!networkConnected() && mSmackable != null) {
			mReconnectInfo = getConnectStr(Conn_No_Network,0);
			mSmackable
					.requestConnectionState(ConnectionState.RECONNECT_NETWORK);
		} else if (mConnectionDemanded.get() && mSmackable != null) {
			mReconnectInfo = getConnectStr(Conn_Reconnect,mReconnectTimeout);
			mSmackable
					.requestConnectionState(ConnectionState.RECONNECT_DELAYED);
			LogUtils.i("connectionFailed(): registering reconnect in "
					+ mReconnectTimeout + "s");
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(
					AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
							+ mReconnectTimeout * 1000, mPAlarmIntent);
			mReconnectTimeout = mReconnectTimeout * 2;
			if (mReconnectTimeout > RECONNECT_MAXIMUM)
				mReconnectTimeout = RECONNECT_MAXIMUM;
		} else {
			connectionClosed();
		}
		broadcastConnectionState();
	}

	private void connectionClosed() {
		LogUtils.i("connectionClosed.");
		mReconnectInfo = "";
		hideNotification(this, SERVICE_NOTIFICATION);
	}

	private void broadcastConnectionState() {
		ConnectionState cs = ConnectionState.OFFLINE;
		if (mSmackable != null) {
			cs = mSmackable.getConnectionState();
		}
		int broadCastItems = mRosterCallbacks.beginBroadcast();
		for (int i = 0; i < broadCastItems; i++) {
			try {
				mRosterCallbacks.getBroadcastItem(i).connectionStateChanged(
						cs.ordinal());
			} catch (RemoteException e) {
				LogUtils.e("caught RemoteException", e);
			}
		}
		mRosterCallbacks.finishBroadcast();
	}

	private String getConnectionStateString() {
		StringBuilder sb = new StringBuilder();
		sb.append(mReconnectInfo);
		if (mSmackable != null && mSmackable.getLastError() != null) {
			sb.append("\n");
			sb.append(mSmackable.getLastError());
		}
		return sb.toString();
	}

	protected abstract void setNotification(String fromJid, String fromUserId,
	String message, boolean is_error);

	protected abstract void updateForeNotification();

	protected abstract void addNotificationMGR();

	protected abstract String getConnectStr(int state,int param);
	
	protected abstract Notification getForeNotification();

	private class ReconnectAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			LogUtils.i("Alarm received.");
			if (!mConnectionDemanded.get()) {
				return;
			}
			if (mIsConnected.get()) {
				LogUtils.e("Reconnect attempt aborted: we are connected again!");
				return;
			}
			doConnect();
		}
	}
	
	class XmppServiceCallbackImp implements XMPPServiceCallback{
		public void newMessage(String from, String message,
				boolean silent_notification) {
			LogUtils.i("notification: " + from);
			notifyClient(from, mSmackable.getNameForJID(from), message,
					!mIsBoundTo.contains(from), silent_notification, false);
		}

		public void messageError(final String from, final String error,
				final boolean silent_notification) {
			LogUtils.i("error notification: " + from);
			mMainHandler.post(new Runnable() {
				public void run() {
					// work around Toast fallback for errors
					notifyClient(from, mSmackable.getNameForJID(from),
							error, !mIsBoundTo.contains(from),
							silent_notification, true);
				}
			});
		}

		public void connectionStateChanged() {
			// TODO: OFFLINE is sometimes caused by XMPPConnection calling
			// connectionClosed() callback on an error, need to catch that?
			switch (mSmackable.getConnectionState()) {
			// case OFFLINE:
			case DISCONNECTED:
				connectionFailed(getConnectStr(Conn_Disconnected,0));
				break;
			case ONLINE:
				mReconnectTimeout = RECONNECT_AFTER;
			default:
				broadcastConnectionState();
				updateForeNotification();
			}
		}
	}
 
}
