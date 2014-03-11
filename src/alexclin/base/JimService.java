package alexclin.base;

import umeox.xmpp.service.XMPPService;
import umeox.xmpp.util.ConnectionState;
import alexclin.ui.MainTabActivity;
import alexclin.ui.chat.ChatActivity;
import alexclin.xmpp.jabberim.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
public class JimService extends XMPPService {

	@Override
	protected void setNotification(String fromJid, String fromUserId,
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
		} else
		if (mConfig.ticker) {
			int newline = message.indexOf('\n');
			int limit = 0;
			String messageSummary = message;
			if (newline >= 0)
				limit = newline;
			if (limit > MAX_TICKER_MSG_LEN || message.length() > MAX_TICKER_MSG_LEN)
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
		mNotificationIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, fromUserId);
		mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		//need to set flag FLAG_UPDATE_CURRENT to get extras transferred
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mNotification.setLatestEventInfo(this, title, message, pendingIntent);
		if (mNotificationCounter > 1)
			mNotification.number = mNotificationCounter;
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
	}

	@Override
	protected void updateServiceNotification() {		
		if (!mConfig.foregroundService)
			return;
		Notification n = new Notification(R.drawable.ic_offline, mConfig.jabberID,
				System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		Intent notificationIntent = new Intent(this, MainTabActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		String message = mLastConnectionError;
		if (message != null)
			message += mReconnectInfo;
		if (mIsConnected.get()) {
			message = MainTabActivity.getStatusTitle(this, mConfig.statusMode, mConfig.statusMessage);
			n.icon = R.drawable.ic_online;
		}
		n.setLatestEventInfo(this, mConfig.jabberID, message, n.contentIntent);

		mServiceNotification.showNotification(this, SERVICE_NOTIFICATION,
				n);
	}

	@Override
	protected void addNotificationMGR() {
		mNotificationIntent = new Intent(this, ChatActivity.class);
	}

	@Override
	public String getConnectStr(int state) {
		switch (state) {
		case conn_connecting:
			return getString(R.string.conn_connecting);
		case conn_online:
			return getString(R.string.conn_online);
		case conn_offline:
			return getString(R.string.conn_offline);
		case conn_empty_roster:
			return getString(R.string.conn_empty_roster);
		case conn_reconnect:
			return getString(R.string.conn_reconnect);
		case conn_disconnected:
			return getString(R.string.conn_disconnected);
		case conn_networkchg:
			return getString(R.string.conn_networkchg);
		}
		return null;
	}
}
