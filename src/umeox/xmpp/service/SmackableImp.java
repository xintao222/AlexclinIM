package umeox.xmpp.service;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.DNSJavaResolver;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.cache.SimpleDirectoryPersistentCache;
import org.jivesoftware.smackx.entitycaps.provider.CapsExtensionProvider;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.Version;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;

import umeox.xmpp.aidl.XMPPServiceCallback;
import umeox.xmpp.base.BaseConfig;
import umeox.xmpp.base.UmeoxException;
import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.ChatProvider.ChatConstants;
import umeox.xmpp.data.RosterProvider;
import umeox.xmpp.data.RosterProvider.RosterConstants;
import umeox.xmpp.util.ConnectionState;
import umeox.xmpp.util.PrefConsts;
import umeox.xmpp.util.XmppHelper;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.lidroid.xutils.util.LogUtils;

public class SmackableImp implements Smackable {	
	final static private int PACKET_TIMEOUT = 30000;

	final static private String[] SEND_OFFLINE_PROJECTION = new String[] {
			ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE,
			ChatConstants.DATE, ChatConstants.PACKET_ID };
	final static private String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION
			+ " = "
			+ ChatConstants.OUTGOING
			+ " AND "
			+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	private static final String PING_ALARM = "org.yaxim.androidclient.PING_ALARM";

	private static final String PONG_TIMEOUT_ALARM = "org.yaxim.androidclient.PONG_TIMEOUT_ALARM";

	static File capsCacheDir = null; // /< this is used to cache if we already
										// initialized EntityCapsCache

	static {
		registerSmackProviders();
		DNSUtil.setDNSResolver(DNSJavaResolver.getInstance());

		// initialize smack defaults before any connections are created
		SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
		SmackConfiguration.setDefaultPingInterval(0);
	}

	static void registerSmackProviders() {
		ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay", "urn:xmpp:delay",
				new DelayInfoProvider());
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
		// add XEP-0092 Software Version
		pm.addIQProvider("query", Version.NAMESPACE, new Version.Provider());

		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE,
				new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE,
				new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT,
				DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT,
				DeliveryReceipt.NAMESPACE,
				new DeliveryReceiptRequest.Provider());
		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());

		// XEP-0115 Entity Capabilities
		pm.addExtensionProvider("c", "http://jabber.org/protocol/caps",
				new CapsExtensionProvider());

		XmppStreamHandler.addExtensionProviders();
	}

	private final BaseConfig mConfig;
	private ConnectionConfiguration mXMPPConfig;
	private XmppStreamHandler.ExtXMPPConnection mXMPPConnection;
	private XmppStreamHandler mStreamHandler;
	private Thread mConnectingThread;
	private Object mConnectingThreadMutex = new Object();

	private ConnectionState mRequestedState = ConnectionState.OFFLINE;
	private ConnectionState mState = ConnectionState.OFFLINE;
	private String mLastError;

	private XMPPServiceCallback mServiceCallBack;
	private Roster mRoster;
	private RosterListener mRosterListener;
	private PacketListener mPacketListener;
	private PacketListener mPresenceListener;
	private ConnectionListener mConnectionListener;

	private final ContentResolver mContentResolver;

	private PacketListener mPongListener;
	private String mPingID;
	private long mPingTimestamp;

	private PendingIntent mPingAlarmPendIntent;
	private PendingIntent mPongTimeoutAlarmPendIntent;
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private Service mService;

	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

	public SmackableImp(BaseConfig config, ContentResolver contentResolver,
			Service service) {
		this.mConfig = config;
		this.mContentResolver = contentResolver;
		this.mService = service;
	}

	// this code runs a DNS resolver, might be blocking
	private synchronized void initXMPPConnection() {
		// allow custom server / custom port to override SRV record
		if (mConfig.customServer.length() > 0)
			mXMPPConfig = new ConnectionConfiguration(mConfig.customServer,
					mConfig.port, mConfig.customServer);
		else
			mXMPPConfig = new ConnectionConfiguration(mConfig.customServer); // use
																		// SRV
		mXMPPConfig.setReconnectionAllowed(false);
		mXMPPConfig.setSendPresence(false);
		mXMPPConfig.setCompressionEnabled(false); // disable for now
		mXMPPConfig.setDebuggerEnabled(mConfig.smackdebug);
		if (mConfig.require_ssl)
			this.mXMPPConfig
					.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		// register MemorizingTrustManager for HTTPS
		if (Build.VERSION.SDK_INT >= 14) {
			this.mXMPPConfig.setTruststoreType("AndroidCAStore");
			this.mXMPPConfig.setTruststorePassword(null);
			this.mXMPPConfig.setTruststorePath(null);
		} else {
			this.mXMPPConfig.setTruststoreType("BKS");
			this.mXMPPConfig.setTruststorePath(BaseConfig.TRUST_STORE_PATH);
		}

		this.mXMPPConnection = new XmppStreamHandler.ExtXMPPConnection(
				mXMPPConfig);
		this.mStreamHandler = new XmppStreamHandler(mXMPPConnection,
				mConfig.smackdebug);
		mStreamHandler
				.addAckReceivedListener(new XmppStreamHandler.AckReceivedListener() {
					public void ackReceived(long handled, long total) {
						gotServerPong("" + handled);
					}
				});
		mConfig.reconnect_required = false;

		initServiceDiscovery();
	}

	// blocking, run from a thread!
	public boolean doConnect(boolean create_account) throws UmeoxException {
		mRequestedState = ConnectionState.ONLINE;
		updateConnectionState(ConnectionState.CONNECTING);
		if (mXMPPConnection == null || mConfig.reconnect_required)
			initXMPPConnection();
		tryToConnect(create_account);
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated()) {
			registerMessageListener();
			registerPresenceListener();
			registerPongListener();
			sendOfflineMessages();
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			updateConnectionState(ConnectionState.ONLINE);
		} else
			throw new UmeoxException(
					"SMACK connected, but authentication failed");
		return true;
	}

	// BLOCKING, call on a new Thread!
	private void updateConnectingThread(Thread new_thread) {
		synchronized (mConnectingThreadMutex) {
			if (mConnectingThread == null) {
				mConnectingThread = new_thread;
			} else
				try {
					LogUtils.e("updateConnectingThread: old thread is still running, killing it.");
					mConnectingThread.interrupt();
					mConnectingThread.join(50);
				} catch (InterruptedException e) {
					LogUtils.d("updateConnectingThread: failed to join(): " + e);
				} finally {
					mConnectingThread = new_thread;
				}
		}
	}

	private void finishConnectingThread() {
		synchronized (mConnectingThreadMutex) {
			mConnectingThread = null;
		}
	}

	/**
	 * Non-blocking, synchronized function to connect/disconnect XMPP. This code
	 * is called from outside and returns immediately. The actual work is done
	 * on a background thread, and notified via callback.
	 * 
	 * @param new_state
	 *            The state to transition into. Possible values: OFFLINE to
	 *            properly close the connection ONLINE to connect DISCONNECTED
	 *            when network goes down
	 * @param create_account
	 *            When going online, try to register an account.
	 */
	@Override
	public synchronized void requestConnectionState(ConnectionState new_state,
			final boolean create_account) {
		LogUtils.d("requestConnState: " + mState + " -> " + new_state
				+ (create_account ? " create_account!" : ""));
		mRequestedState = new_state;
		if (new_state == mState)
			return;
		switch (new_state) {
		case ONLINE:
			switch (mState) {
			case RECONNECT_DELAYED:
				// TODO: cancel timer
			case RECONNECT_NETWORK:
			case OFFLINE:
				// update state before starting thread to prevent race
				// conditions
				updateConnectionState(ConnectionState.CONNECTING);
				new Thread() {
					@Override
					public void run() {
						updateConnectingThread(this);
						try {
							doConnect(create_account);
						} catch (IllegalArgumentException e) {
							// 当配置中的DNS解析错误时可能会出现
							onDisconnected(e);
						} catch (UmeoxException e) {
							onDisconnected(e);
						} finally {
							finishConnectingThread();
						}
					}
				}.start();
				break;
			case CONNECTING:
			case DISCONNECTING:
				// ignore all other cases
				break;
			}
			break;
		case DISCONNECTED:
			// spawn thread to do disconnect
			if (mState == ConnectionState.ONLINE) {
				// update state before starting thread to prevent race
				// conditions
				updateConnectionState(ConnectionState.DISCONNECTING);
				new Thread() {
					public void run() {
						updateConnectingThread(this);
						mStreamHandler.quickShutdown();
						finishConnectingThread();
						// updateConnectionState(ConnectionState.OFFLINE);
					}
				}.start();
			}
			break;
		case OFFLINE:
			switch (mState) {
			case CONNECTING:
			case ONLINE:
				// update state before starting thread to prevent race
				// conditions
				updateConnectionState(ConnectionState.DISCONNECTING);
				// spawn thread to do disconnect
				new Thread() {
					public void run() {
						updateConnectingThread(this);
						mXMPPConnection.shutdown();
						mStreamHandler.close();
						finishConnectingThread();
						// reconnect if it was requested in the meantime
						if (mRequestedState == ConnectionState.ONLINE)
							requestConnectionState(ConnectionState.ONLINE);
					}
				}.start();
				break;
			case DISCONNECTING:
				break;
			case RECONNECT_DELAYED:
				// TODO: clear timer
			case RECONNECT_NETWORK:
				updateConnectionState(ConnectionState.OFFLINE);
			}
			break;
		case RECONNECT_NETWORK:
		case RECONNECT_DELAYED:
			switch (mState) {
			case DISCONNECTED:
			case RECONNECT_NETWORK:
			case RECONNECT_DELAYED:
				updateConnectionState(new_state);
				break;
			default:
				throw new IllegalArgumentException("Can not go from " + mState
						+ " to " + new_state);
			}
		}
	}

	@Override
	public void requestConnectionState(ConnectionState new_state) {
		requestConnectionState(new_state, false);
	}

	@Override
	public ConnectionState getConnectionState() {
		return mState;
	}

	// called at the end of a state transition
	private synchronized void updateConnectionState(ConnectionState new_state) {
		if (new_state == ConnectionState.ONLINE
				|| new_state == ConnectionState.CONNECTING)
			mLastError = null;
		LogUtils.d("updateConnectionState: " + mState + " -> " + new_state
				+ " (" + mLastError + ")");
		if (new_state == mState)
			return;
		mState = new_state;
		if (mServiceCallBack != null)
			mServiceCallBack.connectionStateChanged();
	}

	private void initServiceDiscovery() {
		// register connection features
		DeliveryReceiptManager dm = DeliveryReceiptManager
				.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.addReceiptReceivedListener(new ReceiptReceivedListener() { // DOES
																		// NOT
																		// WORK
																		// IN
																		// CARBONS
			public void onReceiptReceived(String fromJid, String toJid,
					String receiptId) {
				LogUtils.d("got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
			}
		});

		// init Entity Caps manager with storage in app's cache dir
		try {
			if (capsCacheDir == null) {
				capsCacheDir = new File(mService.getCacheDir(),
						"entity-caps-cache");
				capsCacheDir.mkdirs();
				EntityCapsManager
						.setPersistentCache(new SimpleDirectoryPersistentCache(
								capsCacheDir));
			}
		} catch (java.io.IOException e) {
			LogUtils.e("Could not init Entity Caps cache: "
							+ e.getLocalizedMessage());
		}

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(
				10 * 1000);

		// set Version for replies
		String app_name = mService.getApplication().getApplicationInfo().name;
		String build_version = "*";

		try {
			PackageManager pm = mService.getPackageManager();
			PackageInfo info = pm.getPackageInfo(mService.getPackageName(), 0);
			if (info != null) {
				build_version = info.versionName; // 得到版本信息
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Version.Manager.getInstanceFor(mXMPPConnection).setVersion(
				new Version(app_name, build_version, "Android"));

	}

	public void addRosterItem(String user, String alias, String group,String msg)
			throws UmeoxException {
		tryToAddRosterEntry(XmppHelper.getUserAtHost(user,mXMPPConnection), alias, group,msg);
	}

	public void removeRosterItem(String user) throws UmeoxException {
		debugLog("removeRosterItem(" + user + ")");

		tryToRemoveRosterEntry(user);
		mServiceCallBack.connectionStateChanged();
	}

	public void renameRosterItem(String user, String newName)
			throws UmeoxException {
		RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null)) {
			throw new UmeoxException("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	public void addRosterGroup(String group) {
		mRoster.createGroup(group);
	}

	public void renameRosterGroup(String group, String newGroup) {
		RosterGroup groupToRename = mRoster.getGroup(group);
		groupToRename.setName(newGroup);
	}

	public void moveRosterItemToGroup(String user, String group)
			throws UmeoxException {
		tryToMoveRosterEntryToGroup(user, group);
	}

	public void sendPresenceRequest(String user, String type) {
		// HACK: remove the fake roster entry added by handleIncomingSubscribe()
		if ("unsubscribed".equals(type))
			deleteRosterEntryFromDB(user);
		Presence response = new Presence(Presence.Type.valueOf(type));
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
	}

	private void onDisconnected(String reason) {
		unregisterPongListener();
		mLastError = reason;
		updateConnectionState(ConnectionState.DISCONNECTED);
	}

	private void onDisconnected(Throwable reason) {
		LogUtils.e("onDisconnected: " + reason);
		reason.printStackTrace();
		// iterate through to the deepest exception
		while (reason.getCause() != null)
			reason = reason.getCause();
		onDisconnected(reason.getLocalizedMessage());
	}

	private void tryToConnect(boolean create_account) throws UmeoxException {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mStreamHandler.quickShutdown(); // blocking shutdown prior
													// to re-connection
				} catch (Exception e) {
					debugLog("conn.shutdown() failed: " + e);
				}
			}
			registerRosterListener();
			boolean need_bind = !mStreamHandler.isResumePossible();

			mXMPPConnection.connect(need_bind);
			// the following should not happen as of smack 3.3.1
			if (!mXMPPConnection.isConnected()) {
				throw new UmeoxException(
						"SMACK connect failed without exception!");
			}
			if (mConnectionListener != null)
				mXMPPConnection.removeConnectionListener(mConnectionListener);
			mConnectionListener = new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					onDisconnected(e);
					updateConnectionState(ConnectionState.DISCONNECTED);
				}

				public void connectionClosed() {
					// TODO: fix reconnect when we got kicked by the server or
					// SM failed!
					// onDisconnected(null);
					updateConnectionState(ConnectionState.OFFLINE);
				}

				public void reconnectingIn(int seconds) {
				}

				public void reconnectionFailed(Exception e) {
				}

				public void reconnectionSuccessful() {
				}
			};
			mXMPPConnection.addConnectionListener(mConnectionListener);
			
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				if (create_account) {
					LogUtils.d("creating new server account...");
					AccountManager am = new AccountManager(mXMPPConnection);
					am.createAccount(mConfig.jabberID, mConfig.password);
				}
				mXMPPConnection.login(mConfig.jabberID, mConfig.password,
						mConfig.ressource);
			}
			LogUtils.d("SM: can resume = " + mStreamHandler.isResumePossible()
					+ " needbind=" + need_bind);
			if (need_bind) {
				mStreamHandler.notifyInitialLogin();
				setStatusFromConfig();
			}

		} catch (XMPPException e) {
			throw new UmeoxException(e.getLocalizedMessage(),
					e.getCause());
		} catch (Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			LogUtils.e("tryToConnect(): " + Log.getStackTraceString(e));
			throw new UmeoxException(e.getLocalizedMessage(), e.getCause());
		}
	}

	private void tryToMoveRosterEntryToGroup(String userName, String groupName)
			throws UmeoxException {

		RosterGroup rosterGroup = getRosterGroup(groupName);
		RosterEntry rosterEntry = mRoster.getEntry(userName);

		removeRosterEntryFromGroups(rosterEntry);

		if (groupName.length() == 0)
			return;
		else {
			try {
				rosterGroup.addEntry(rosterEntry);
			} catch (XMPPException e) {
				throw new UmeoxException(e.getLocalizedMessage());
			}
		}
	}

	private RosterGroup getRosterGroup(String groupName) {
		RosterGroup rosterGroup = mRoster.getGroup(groupName);

		// create group if unknown
		if ((groupName.length() > 0) && rosterGroup == null) {
			rosterGroup = mRoster.createGroup(groupName);
		}
		return rosterGroup;

	}

	private void removeRosterEntryFromGroups(RosterEntry rosterEntry)
			throws UmeoxException {
		Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

		for (RosterGroup group : oldGroups) {
			tryToRemoveUserFromGroup(group, rosterEntry);
		}
	}

	private void tryToRemoveUserFromGroup(RosterGroup group,
			RosterEntry rosterEntry) throws UmeoxException {
		try {
			group.removeEntry(rosterEntry);
		} catch (XMPPException e) {
			throw new UmeoxException(e.getLocalizedMessage());
		}
	}

	private void tryToRemoveRosterEntry(String user) throws UmeoxException {
		try {
			RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null) {
				// first, unsubscribe the user
				Presence unsub = new Presence(Presence.Type.unsubscribed);
				unsub.setTo(rosterEntry.getUser());
				mXMPPConnection.sendPacket(unsub);
				// then, remove from roster
				mRoster.removeEntry(rosterEntry);
			}
		} catch (XMPPException e) {
			throw new UmeoxException(e.getLocalizedMessage());
		}
	}

	private void tryToAddRosterEntry(String user, String alias, String group,String msg)
			throws UmeoxException {
		try {
//			mRoster.createEntry(user, alias, new String[] { group });
			createEntry(user, alias, new String[] { group },msg);
		} catch (XMPPException e) {
			throw new UmeoxException(e.getLocalizedMessage());
		}
	}

	private void removeOldRosterEntries() {
		LogUtils.d("removeOldRosterEntries()");
		Collection<RosterEntry> rosterEntries = mRoster.getEntries();
		StringBuilder exclusion = new StringBuilder(RosterConstants.JID
				+ " NOT IN (");
		boolean first = true;
		for (RosterEntry rosterEntry : rosterEntries) {
			updateRosterEntryInDB(rosterEntry);
			if (first)
				first = false;
			else
				exclusion.append(",");
			exclusion.append("'").append(rosterEntry.getUser()).append("'");
		}
		exclusion.append(")");
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				exclusion.toString(), null);
		LogUtils.d("deleted " + count + " old roster entries");
	}

	// HACK: add an incoming subscription request as a fake roster entry
	private void handleIncomingSubscribe(Presence request) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, request.getFrom());
		values.put(RosterConstants.ALIAS, request.getFrom());
		values.put(RosterConstants.GROUP, "");

		values.put(RosterConstants.STATUS_MODE, getStatusInt(request));
		values.put(RosterConstants.STATUS_MESSAGE, request.getStatus());
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);		
		debugLog("handleIncomingSubscribe: faked " + uri);
	}

	public void setStatusFromConfig() {
		// TODO: only call this when carbons changed, not on every presence
		// change
		CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(
				mConfig.messageCarbons);

		Presence presence = new Presence(Presence.Type.available);
		Mode mode = Mode.valueOf(mConfig.statusMode);
		presence.setMode(mode);
		presence.setStatus(mConfig.statusMessage);
		presence.setPriority(mConfig.priority);
		mXMPPConnection.sendPacket(presence);
		mConfig.presence_required = false;
	}

	public void sendOfflineMessages() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);
		final int _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
		final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int PACKETID_COL = cursor
				.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS,
				ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext()) {
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String message = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			long ts = cursor.getLong(TS_COL);
			LogUtils.d("sendOfflineMessages: " + toJID + " > " + message);
			final Message newMessage = new Message(toJID, Message.Type.chat);
			newMessage.setBody(message);
			DelayInformation delay = new DelayInformation(new Date(ts));
			newMessage.addExtension(delay);
			newMessage.addExtension(new DelayInfo(delay));
			newMessage.addExtension(new DeliveryReceiptRequest());
			if ((packetID != null) && (packetID.length() > 0)) {
				newMessage.setPacketID(packetID);
			} else {
				packetID = newMessage.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}
			Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
					+ ChatProvider.TABLE_NAME + "/" + _id);
			mContentResolver.update(rowuri, mark_sent, null, null);
			mXMPPConnection.sendPacket(newMessage); // must be after marking
													// delivered, otherwise it
													// may override the
													// SendFailListener
		}
		cursor.close();
	}

	public static void sendOfflineMessage(ContentResolver cr, String toJID,
			String message) {
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public void sendReceipt(String toJID, String id) {
		LogUtils.d("sending XEP-0184 ack to " + toJID + " id=" + id);
		final Message ack = new Message(toJID, Message.Type.normal);
		ack.addExtension(new DeliveryReceipt(id));
		mXMPPConnection.sendPacket(ack);
	}

	public void sendMessage(String toJID, String message) {
		final Message newMessage = new Message(toJID, Message.Type.chat);
		newMessage.setBody(message);
		newMessage.addExtension(new DeliveryReceiptRequest());
		if (isAuthenticated()) {
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
					ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
					newMessage.getPacketID());
			mXMPPConnection.sendPacket(newMessage);
		} else {
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
					ChatConstants.DS_NEW, System.currentTimeMillis(),
					newMessage.getPacketID());
		}
	}

	public boolean isAuthenticated() {
		if (mXMPPConnection != null) {
			return (mXMPPConnection.isConnected() && mXMPPConnection
					.isAuthenticated());
		}
		return false;
	}

	public void registerCallback(XMPPServiceCallback callBack) {
		this.mServiceCallBack = callBack;
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(
				PING_ALARM));
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(
				PONG_TIMEOUT_ALARM));
	}

	public void unRegisterCallback() {
		debugLog("unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try {
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection.removePacketListener(mPresenceListener);

			mXMPPConnection.removePacketListener(mPongListener);
			unregisterPongListener();
		} catch (Exception e) {
			// ignore it!
			e.printStackTrace();
		}
		requestConnectionState(ConnectionState.OFFLINE);
		setStatusOffline();
		mService.unregisterReceiver(mPingAlarmReceiver);
		mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		this.mServiceCallBack = null;
	}

	public String getNameForJID(String jid) {
		if (null != this.mRoster.getEntry(jid)
				&& null != this.mRoster.getEntry(jid).getName()
				&& this.mRoster.getEntry(jid).getName().length() > 0) {
			return this.mRoster.getEntry(jid).getName();
		} else {
			return jid;
		}
	}

	private void setStatusOffline() {
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, PrefConsts.offline);
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}

	private void registerRosterListener() {
		// flush roster on connecting.
		mRoster = mXMPPConnection.getRoster();
		mRoster.setSubscriptionMode(Roster.SubscriptionMode.manual);

		if (mRosterListener != null)
			mRoster.removeRosterListener(mRosterListener);
		
		mRosterListener = new RosterListener() {
			private boolean first_roster = true;

			public void entriesAdded(Collection<String> entries) {
				debugLog("entriesAdded(" + entries + ")");

				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				// when getting the roster in the beginning, remove remains of
				// old one
				if (first_roster) {
					removeOldRosterEntries();
					first_roster = false;
					mServiceCallBack.connectionStateChanged();
				}
				debugLog("entriesAdded() done");
			}

			public void entriesDeleted(Collection<String> entries) {
				debugLog("entriesDeleted(" + entries + ")");

				for (String entry : entries) {
					deleteRosterEntryFromDB(entry);
				}
				mServiceCallBack.connectionStateChanged();
			}

			public void entriesUpdated(Collection<String> entries) {
				debugLog("entriesUpdated(" + entries + ")");

				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				mServiceCallBack.connectionStateChanged();
			}

			public void presenceChanged(Presence presence) {
				debugLog("presenceChanged(" + presence.getFrom() + "): "
						+ presence);

				String jabberID = getBareJID(presence.getFrom());
				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntryInDB(rosterEntry);
				mServiceCallBack.connectionStateChanged();
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}

	private String getBareJID(String from) {
		String[] res = from.split("/");
		return res[0].toLowerCase(Locale.CHINESE);
	}

	public boolean changeMessageDeliveryStatus(String packetID, int new_status) {
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME);
		return mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID
				+ " = ? AND " + ChatConstants.DELIVERY_STATUS + " != "
				+ ChatConstants.DS_ACKED + " AND " + ChatConstants.DIRECTION
				+ " = " + ChatConstants.OUTGOING, new String[] { packetID }) > 0;
	}

	/**
	 * Check the server connection, reconnect if needed.
	 * 
	 * This function will try to ping the server if we are connected, and try to
	 * reestablish a connection otherwise.
	 */
	public void sendServerPing() {
		if (mXMPPConnection == null || !mXMPPConnection.isAuthenticated()) {
			debugLog("Ping: requested, but not connected to server.");
			requestConnectionState(ConnectionState.ONLINE, false);
			return;
		}
		if (mPingID != null) {
			debugLog("Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}

		mPingTimestamp = System.currentTimeMillis();
		if (mStreamHandler.isSmEnabled()) {
			debugLog("Ping: sending SM request");
			mPingID = "" + mStreamHandler.requestAck();
		} else {
			Ping ping = new Ping();
			ping.setType(Type.GET);
			ping.setTo(mConfig.customServer);
			mPingID = ping.getPacketID();
			debugLog("Ping: sending ping " + mPingID);
			mXMPPConnection.sendPacket(ping);
		}

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).set(
				AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
						+ PACKET_TIMEOUT + 3000, mPongTimeoutAlarmPendIntent);
	}

	private void gotServerPong(String pongID) {
		long latency = System.currentTimeMillis() - mPingTimestamp;
		if (pongID != null && pongID.equals(mPingID))
			LogUtils.i(String.format("Ping: server latency %1.3fs",
					latency / 1000.));
		else
			LogUtils.i(String.format("Ping: server latency %1.3fs (estimated)",
					latency / 1000.));
		mPingID = null;
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
				.cancel(mPongTimeoutAlarmPendIntent);
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			debugLog("Ping: timeout for " + mPingID);
			onDisconnected("Ping timeout");
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			sendServerPing();
		}
	}

	/**
	 * Registers a smack packet listener for IQ packets, intended to recognize
	 * "pongs" with a packet id matching the last "ping" sent to the server.
	 * 
	 * Also sets up the AlarmManager Timer plus necessary intents.
	 */
	private void registerPongListener() {
		// reset ping expectation on new connection
		mPingID = null;

		if (mPongListener != null)
			mXMPPConnection.removePacketListener(mPongListener);

		mPongListener = new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				if (packet == null)
					return;

				gotServerPong(packet.getPacketID());
			}

		};

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(
				IQ.class));
		mPingAlarmPendIntent = PendingIntent.getBroadcast(
				mService.getApplicationContext(), 0, mPingAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(
				mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
				.setInexactRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis()
								+ AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						mPingAlarmPendIntent);
	}

	private void unregisterPongListener() {
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
				.cancel(mPingAlarmPendIntent);
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
				.cancel(mPongTimeoutAlarmPendIntent);
	}

	private void registerMessageListener() {
		// do not register multiple packet listeners
		if (mPacketListener != null)
			mXMPPConnection.removePacketListener(mPacketListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;

						String fromJID = getBareJID(msg.getFrom());
						int direction = ChatConstants.INCOMING;
						Carbon cc = CarbonManager.getCarbon(msg);

						// extract timestamp
						long ts;
						DelayInfo timestamp = (DelayInfo) msg.getExtension(
								"delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo) msg.getExtension("x",
									"jabber:x:delay");
						if (cc != null) // Carbon timestamp overrides packet
										// timestamp
							timestamp = cc.getForwarded().getDelayInfo();
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();

						// try to extract a carbon
						if (cc != null) {
							LogUtils.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded()
									.getForwardedPacket();

							// outgoing carbon: fromJID is actually chat peer's
							// JID
							if (cc.getDirection() == Carbon.Direction.sent) {
								fromJID = getBareJID(msg.getTo());
								direction = ChatConstants.OUTGOING;
							} else {
								fromJID = getBareJID(msg.getFrom());

								// hook off carbonated delivery receipts
								DeliveryReceipt dr = (DeliveryReceipt) msg
										.getExtension(DeliveryReceipt.ELEMENT,
												DeliveryReceipt.NAMESPACE);
								if (dr != null) {
									LogUtils.d("got CC'ed delivery receipt for "
													+ dr.getId());
									changeMessageDeliveryStatus(dr.getId(),
											ChatConstants.DS_ACKED);
								}
							}
						}

						String chatMessage = msg.getBody();

						// display error inline
						if (msg.getType() == Message.Type.error) {
							if (changeMessageDeliveryStatus(msg.getPacketID(),
									ChatConstants.DS_FAILED))
								mServiceCallBack.messageError(fromJID, msg
										.getError().toString(), (cc != null));
							return; // we do not want to add errors as
									// "incoming messages"
						}

						// ignore empty messages
						if (chatMessage == null) {
							LogUtils.d("empty message.");
							return;
						}

						// carbons are old. all others are new
						int is_new = (cc == null) ? ChatConstants.DS_NEW
								: ChatConstants.DS_SENT_OR_READ;
						if (msg.getType() == Message.Type.error)
							is_new = ChatConstants.DS_FAILED;

						addChatMessageToDB(direction, fromJID, chatMessage,
								is_new, ts, msg.getPacketID());
						if (direction == ChatConstants.INCOMING)
							mServiceCallBack.newMessage(fromJID, chatMessage,
									(cc != null));
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					LogUtils.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, filter);
	}

	private void registerPresenceListener() {
		// do not register multiple packet listeners
		if (mPresenceListener != null)
			mXMPPConnection.removePacketListener(mPresenceListener);

		mPresenceListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					Presence p = (Presence) packet;
					switch (p.getType()) {
					case subscribe:
						handleIncomingSubscribe(p);
						break;
					case unsubscribe:
					case available:
					case error:
					case subscribed:
					case unavailable:
					case unsubscribed:
					default:
						break;
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					LogUtils.e("failed to process presence:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPresenceListener,
				new PacketTypeFilter(Presence.class));
	}

	private void addChatMessageToDB(int direction, String JID, String message,
			int delivery_status, long ts, String packetID) {
		ContentValues values = new ContentValues();

		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, JID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);
		values.put(ChatConstants.USER, StringUtils.parseName(mXMPPConnection.getUser()));
		
		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
	}

	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));

		Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));
		values.put(RosterConstants.USER, StringUtils.parseName(mXMPPConnection.getUser()));
		LogUtils.e("Values:"+values.toString());
		return values;
	}

	private void addRosterEntryToDB(final RosterEntry entry) {
		ContentValues values = getContentValuesForRosterEntry(entry);
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		debugLog("addRosterEntryToDB: Inserted " + uri);
	}

	private void deleteRosterEntryFromDB(final String jabberID) {
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				RosterConstants.JID + " = ? and "+RosterConstants.USER +" = ?", new String[] { jabberID,StringUtils.parseName(mXMPPConnection.getUser())});
		debugLog("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	private void updateRosterEntryInDB(final RosterEntry entry) {
		final ContentValues values = getContentValuesForRosterEntry(entry);

		if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
				RosterConstants.JID + " = ?", new String[] { entry.getUser() }) == 0)
			addRosterEntryToDB(entry);
	}

	private String getGroup(Collection<RosterGroup> groups) {
		for (RosterGroup group : groups) {
			return group.getName();
		}
		return "";
	}

	private String getName(RosterEntry rosterEntry) {
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0) {
			return name;
		}
		return rosterEntry.getUser();
	}

	private int getStatusInt(final Presence presence) {
		if (presence.getType() == Presence.Type.subscribe)
			return PrefConsts.subscribe;
		if (presence.getType() == Presence.Type.available) {
			if (presence.getMode() != null) {
				return PrefConsts.getStatus(presence.getMode());
			}
			return PrefConsts.available;
		}
		return PrefConsts.offline;
	}

	private void debugLog(String data) {
		LogUtils.d(data);
	}

	@Override
	public String getLastError() {
		return mLastError;
	}
	
	/**
	 * 自定义的添加好友函数，替代Roster类的默认添加函数createEntry(String user, String name, String[] groups)
	 * >>>>>>增加验证信息一项
	 * @param user  要添加的用户名
	 * @param name  设置的昵称
	 * @param groups  用户所属的组
	 * @param msg  验证信息
	 * @throws XMPPException
	 */
	private void createEntry(String user, String name, String[] groups,String msg) throws XMPPException {
        if (!mXMPPConnection.isAuthenticated()) {
            throw new IllegalStateException("Not logged in to server.");
        }
        if (mXMPPConnection.isAnonymous()) {
            throw new IllegalStateException("Anonymous users can't have a roster.");
        }

        // Create and send roster entry creation packet.
        RosterPacket rosterPacket = new RosterPacket();
        rosterPacket.setType(IQ.Type.SET);
        RosterPacket.Item item = new RosterPacket.Item(user, name);
        if (groups != null) {
            for (String group : groups) {
                if (group != null && group.trim().length() > 0) {
                    item.addGroupName(group);
                }
            }
        }
        rosterPacket.addRosterItem(item);
        // Wait up to a certain number of seconds for a reply from the server.
        PacketCollector collector = mXMPPConnection.createPacketCollector(
                new PacketIDFilter(rosterPacket.getPacketID()));
        mXMPPConnection.sendPacket(rosterPacket);
        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }

        // Create a presence subscription packet and send.
        Presence presencePacket = new Presence(Presence.Type.subscribe);
        presencePacket.setTo(user);
        presencePacket.setStatus(msg);
        mXMPPConnection.sendPacket(presencePacket);              
    }

	@Override
	public Collection<HostedRoom> getHostedRooms() {
		try {
			return MultiUserChat.getHostedRooms(mXMPPConnection,
					"conference."+mXMPPConnection.getServiceName());		
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
