package alexclin.base;

import umeox.xmpp.service.XMPPService;
import umeox.xmpp.transfer.FileSender;
import alexclin.ui.MainTabActivity;
import alexclin.ui.chat.ChatActivity;
import alexclin.util.StringUtil;
import alexclin.xmpp.jabberim.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

import com.lidroid.xutils.util.LogUtils;
public class JimService extends XMPPService {
	private Intent mNotificationIntent;
	
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
		} else if (mConfig.ticker) {
			int newline = message.indexOf('\n');
			int limit = 0;
			if(FileSender.isWrappedMsg(message)){
				message = "语音消息";
			}
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
	protected void updateForeNotification() {		
		if (!mConfig.foregroundService)
			return;
		LogUtils.i("updateForeNotification");
		Notification n = getForeNotification();
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
			if(StringUtil.isNullOrEmpty(message)){
				message = "在线";
			}
		}
		n.setLatestEventInfo(this, mConfig.jabberID, message, n.contentIntent);

		showNotification(this, SERVICE_NOTIFICATION,n);
	}

	protected Notification getForeNotification() {
		Notification n = new Notification(R.drawable.ic_offline, mConfig.jabberID,
				System.currentTimeMillis());
		n.setLatestEventInfo(this, mConfig.jabberID, "在线", n.contentIntent);
		return n;
	}

	@Override
	protected void addNotificationMGR() {
		mNotificationIntent = new Intent(this, ChatActivity.class);
	}

	@Override
	public String getConnectStr(Status state,int param) {
		switch (state) {
		case Conn_Connecting:
			return getString(R.string.conn_connecting);
		case Conn_Online:
			return getString(R.string.conn_online);
		case Conn_Offline:
			return getString(R.string.conn_offline);
		case Conn_Empty_Roster:
			return getString(R.string.conn_empty_roster);
		case Conn_Reconnect:
			return getString(R.string.conn_reconnect,param);
		case Conn_Disconnected:
			return getString(R.string.conn_disconnected);
		case Conn_Networkchg:
			return getString(R.string.conn_networkchg);
		case Conn_No_Network:
			break;
		default:
			break;
		}
		return null;
	}
}
