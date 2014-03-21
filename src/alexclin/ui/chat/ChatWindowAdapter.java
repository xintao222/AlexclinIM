package alexclin.ui.chat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.ChatProvider.ChatConstants;
import umeox.xmpp.transfer.AudioUtil;
import umeox.xmpp.transfer.FileSender;
import alexclin.base.GlobalConfig;
import alexclin.http.BaseApi.Callback;
import alexclin.http.FileApi;
import alexclin.xmpp.jabberim.R;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.lidroid.xutils.util.LogUtils;

class ChatWindowAdapter extends ResourceCursorAdapter implements Callback {
	private SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.CHINA);
	private static final int DELAY_NEWMSG = 2000;
	private ChatActivity mAct;
	private Handler handler = new Handler();

	public ChatWindowAdapter(Context context, Cursor c) {
		super(context, R.layout.listitem_chatitem, c, FLAG_AUTO_REQUERY);
		this.mAct = (ChatActivity) context;
	}

	@Override
	public void bindView(View view, Context ctx, Cursor c) {
		ViewHolder holder = initViewHolder(view);
		String msg = c.getString(c
				.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
		int _id = c.getInt(c.getColumnIndex(ChatProvider.ChatConstants._ID));
		int status = c.getInt(c.getColumnIndex(ChatConstants.DELIVERY_STATUS));
		long time = c.getLong(c.getColumnIndex(ChatConstants.DATE));
		int direct = c.getInt(c.getColumnIndex(ChatConstants.DIRECTION));
		holder.setContent(msg, time, direct, status);
		boolean from_me = (direct ==ChatConstants.OUTGOING);
		if (!from_me && status == ChatConstants.DS_NEW) {
			markAsReadDelayed(_id, DELAY_NEWMSG);
		}
	}
	
	private void markAsReadDelayed(final int id, int delay) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				markAsRead(id);
			}
		}, delay);
	}
	
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME + "/" + id);
		LogUtils.d("markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		mAct.getContentResolver().update(rowuri, values, null, null);
	}

	private ViewHolder initViewHolder(View view) {
		if (view.getTag() == null) {
			ViewHolder holder = new ViewHolder();
			holder.timeTv = (TextView) view.findViewById(R.id.ChatItem_Time);
			holder.outStatus = (TextView) view
					.findViewById(R.id.ChatItem_OutStatus);
			holder.inHolder.rootView = view
					.findViewById(R.id.ChatItem_InLayout);
			holder.inHolder.headIcon = (ImageView) view
					.findViewById(R.id.ChatItem_InHead);
			holder.inHolder.img = (ImageView) view
					.findViewById(R.id.ChatItem_InImage);
			holder.inHolder.msgTv = (TextView) view
					.findViewById(R.id.ChatItem_InContent);
			holder.inHolder.voiceBtn = (Button) view
					.findViewById(R.id.ChatItem_InVoice);
			holder.outHolder.rootView = view
					.findViewById(R.id.ChatItem_OutLayout);
			holder.outHolder.headIcon = (ImageView) view
					.findViewById(R.id.ChatItem_OutHead);
			holder.outHolder.img = (ImageView) view
					.findViewById(R.id.ChatItem_OutImage);
			holder.outHolder.msgTv = (TextView) view
					.findViewById(R.id.ChatItem_OutContent);
			holder.outHolder.voiceBtn = (Button) view
					.findViewById(R.id.ChatItem_OutVoice);
			view.setTag(holder);
			return holder;
		} else {
			return (ViewHolder) view.getTag();
		}
	}

	class ViewHolder {
		TextView timeTv;
		ChildHolder outHolder;
		ChildHolder inHolder;
		TextView outStatus;

		public ViewHolder() {
			this.outHolder = new ChildHolder();
			this.inHolder = new ChildHolder();
		}

		public void setContent(String msg, long time, int direct, int status) {
			timeTv.setText(sdf.format(new Date(time)));			
			ChildHolder ch = null;
			if (direct == ChatConstants.INCOMING) {
				outHolder.setVisbility(View.GONE);
				inHolder.setVisbility(View.VISIBLE);
				ch = inHolder;
			} else {
				outHolder.setVisbility(View.VISIBLE);
				inHolder.setVisbility(View.GONE);
				ch = outHolder;
			}
			if(FileSender.isWrappedMsg(msg)){
				FileSender.Msg fm = FileSender.unwrappMessage(msg);
				String path = null;
				if(fm.getType()==FileSender.TYPE_IMAGE){
					ch.setType(ChildHolder.TYPE_IMG);
					path = GlobalConfig.ImageCaheDir+"/" + AudioUtil.getTime()+".jpg";
				}else{
					ch.setType(ChildHolder.TYPE_VOICE);
					path = GlobalConfig.VoiceCaheDir+"/" + AudioUtil.getTime()+".amr";
					ch.voiceBtn.setOnClickListener(new VoiceBtnListener(fm.getUrl()));
				}	
				if(mAct.mAudioDb.getPath(fm.getUrl())==null){
				FileApi.downloadFile(ChatWindowAdapter.this, fm.getUrl(),path,fm);
				}
			}else{
				ch.setType(ChildHolder.TYPE_TEXT);
				ch.msgTv.setText(msg);
			}			
			outStatus.setText(getStatusText(status));
		}
	}

	class ChildHolder {
		public static final int TYPE_TEXT = 0;
		public static final int TYPE_IMG = 1;
		public static final int TYPE_VOICE = 2;
		View rootView;
		ImageView headIcon;
		TextView msgTv;
		ImageView img;
		Button voiceBtn;

		public void setVisbility(int visibility) {
			rootView.setVisibility(visibility);
		}

		public void setType(int type) {
			msgTv.setVisibility(type == TYPE_TEXT ? View.VISIBLE : View.GONE);
			img.setVisibility(type == TYPE_IMG ? View.VISIBLE : View.GONE);
			voiceBtn.setVisibility(type == TYPE_VOICE ? View.VISIBLE
					: View.GONE);
		}
	}

	public CharSequence getStatusText(int status) {
		switch (status) {
		case ChatConstants.DS_NEW:
			return "未发送";
		case ChatConstants.DS_ACKED:
			return "已读";
		case ChatConstants.DS_SENT_OR_READ:
			return "已发送";
		case ChatConstants.DS_FAILED:
			return "发送失败";
		}
		return "未知";
	}
	
	class VoiceBtnListener implements OnClickListener{
		private String url;
		
		public VoiceBtnListener(String url) {
			super();
			this.url = url;
		}

		@Override
		public void onClick(View arg0) {
			String path = mAct.mAudioDb.getPath(url);
			if(path!=null){
				mAct.playMedia(path);
			}else{
				//TODO 放大显示图片
			}
		}		
	}

	@Override
	public void onLoading(long total, long current, int apiInt, Object tag) {}

	@Override
	public void onStart(int apiInt, Object tag) {}

	@Override
	public void onFailure(int error, int apiInt, Object tag) {}

	@Override
	public void onSuccess(Object result, int apiInt, Object tag) {
		String path = (String) result;
		FileSender.Msg fm = (FileSender.Msg) tag;
		mAct.mAudioDb.insert(fm.getUrl(), path);
	}

}
