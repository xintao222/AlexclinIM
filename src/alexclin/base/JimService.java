package alexclin.base;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.Presence;

import umeox.xmpp.data.RosterProvider;
import umeox.xmpp.data.RosterProvider.RosterConstants;
import umeox.xmpp.service.Smackable.ConnectionState;
import umeox.xmpp.service.SmackableImp;
import umeox.xmpp.service.XMPPService;
import umeox.xmpp.transfer.FileSender;
import umeox.xmpp.transfer.FileSender.Msg;
import alexclin.ui.MainTabActivity;
import alexclin.ui.chat.ChatActivity;
import alexclin.xmpp.jabberim.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.widget.Toast;

public class JimService extends XMPPService {
	protected static final int SERVICE_NOTIFICATION = 1;
	private Intent mNotificationIntent;
	private WakeLock mWakeLock;

	private Vibrator mVibrator;
	private int lastNotificationId = 2;
	private Notification mNotification;

	protected Map<String, Integer> notificationCount = new HashMap<String, Integer>(
			2);
	protected Map<String, Integer> notificationId = new HashMap<String, Integer>(
			2);
	protected NotificationManager mNotifManager;

	public static String getConnectStr(Context ctx, int s) {
		ConnectionState state = ConnectionState.values()[s];
		switch (state) {
		case CONNECTING:
			return ctx.getString(R.string.conn_connecting);
		case DISCONNECTING:
			// TODO
			break;
		case DISCONNECTED:
			return ctx.getString(R.string.conn_disconnected);
		case OFFLINE:
			return ctx.getString(R.string.conn_offline);
		case ONLINE:
			return ctx.getString(R.string.conn_online);
		case RECONNECT_DELAYED:
			break;
		case RECONNECT_NETWORK:
			break;
		default:
			break;
		}
		return null;
	}

	protected void showMessageNotification(String fromJid, String fromUserId,
			String message, boolean is_error) {
		int mNotificationCounter = 0;
		if (notificationCount.containsKey(fromJid)) {
			mNotificationCounter = notificationCount.get(fromJid);
		}
		mNotificationCounter++;
		notificationCount.put(fromJid, mNotificationCounter);
		String author;
		if (null == fromUserId || fromUserId.length() == 0) {
			author = fromJid;
		} else {
			author = fromUserId;
		}
		String title = getString(R.string.notification_message, author);
		String ticker;
		if (is_error) {
			title = getString(R.string.notification_error);
			ticker = title;
			message = author + ": " + message;
		} else if (mConfig.ticker) {
			int newline = message.indexOf('\n');
			int limit = 0;
			if (FileSender.isWrappedMsg(message)) {
				message = "语音消息";
			}
			String messageSummary = message;
			if (newline >= 0)
				limit = newline;
			if (limit > MAX_TICKER_MSG_LEN
					|| message.length() > MAX_TICKER_MSG_LEN)
				limit = MAX_TICKER_MSG_LEN;
			if (limit > 0)
				messageSummary = message.substring(0, limit) + " [...]";
			ticker = title + ":\n" + messageSummary;
		} else
			ticker = getString(R.string.notification_anonymous_message);

		mNotification = new Notification(R.drawable.sb_message, ticker,
				System.currentTimeMillis());
		Uri userNameUri = Uri.parse(fromJid);
		mNotificationIntent.setData(userNameUri);
		mNotificationIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME,
				fromUserId);
		mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		// need to set flag FLAG_UPDATE_CURRENT to get extras transferred
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mNotification.setLatestEventInfo(this, title, message, pendingIntent);
		if (mNotificationCounter > 1)
			mNotification.number = mNotificationCounter;
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
	}

	@Override
	protected void showStatusNotification(ConnectionState state) {
		Notification n = new Notification(R.drawable.ic_offline,
				mConfig.jabberID, System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		Intent notificationIntent = new Intent(this, MainTabActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		n.setLatestEventInfo(this, mConfig.jabberID,
				getConnectStr(this, state.ordinal()), n.contentIntent);
		startForeground(SERVICE_NOTIFICATION, n);
	}

	private void shortToastNotify(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	protected void showMessageNotification(String fromJid, String fromUserName,
			String message, boolean showNotification,
			boolean silent_notification, boolean is_error) {
		if (!showNotification) {
			if (is_error)
				// TODO
				// shortToastNotify(getConnectStr(Status.Conn_Disconnected, 0)
				// + " " + message);
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

		showMessageNotification(fromJid, fromUserName, message, is_error);
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
		// the notification and let the system decide.
		if (!silent_notification && "SYSTEM".equals(mConfig.vibraNotify)) {
			mNotification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		mNotifManager.notify(notifyId, mNotification);

		// If vibration is forced, vibrate now.
		if (!silent_notification && "ALWAYS".equals(mConfig.vibraNotify)) {
			mVibrator.vibrate(400);
		}
		mWakeLock.release();
	}

	private void setLEDNotification() {
		if (mConfig.isLEDNotify) {
			mNotification.ledARGB = Color.MAGENTA;
			mNotification.ledOnMS = 300;
			mNotification.ledOffMS = 1000;
			mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
		}
	}

	@Override
	protected void init() {
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass()
						.getSimpleName());
		mNotifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	protected void release() {
		// TODO Auto-generated method stub

	}

	protected void clearMessageNotification(String userJid) {
		int notifyId = 0;
		if (notificationId.containsKey(userJid)) {
			notifyId = notificationId.get(userJid);
			mNotifManager.cancel(notifyId);
		}
		notificationCount.remove(userJid);
	}

	@Override
	protected void receivePresence(Presence request) {
		Cursor c = getContentResolver().query(
				RosterProvider.CONTENT_URI,	null,
				RosterConstants.JID + "= ? AND " + RosterConstants.USER
						+ " = ?",
				new String[] { request.getFrom(), mConfig.jabberID }, null);
		boolean has = c.moveToNext();
		c.close();
		if(!has){
			ContentValues values = new ContentValues();
			values.put(RosterConstants.JID, request.getFrom());
			values.put(RosterConstants.ALIAS, request.getFrom());
			values.put(RosterConstants.GROUP, "");

			values.put(RosterConstants.STATUS_MODE,
					SmackableImp.getStatusInt(request));
			values.put(RosterConstants.STATUS_MESSAGE, request.getStatus());
			values.put(RosterConstants.USER, mConfig.jabberID);
			getContentResolver().insert(RosterProvider.CONTENT_URI, values);
		}else{
			mSmackable.sendPresenceRequest(request.getFrom(), Presence.Type.subscribed.name());
		}
	}
}
