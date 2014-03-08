package alexclin.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.RosterProvider;
import umeox.xmpp.data.ChatProvider.ChatConstants;
import umeox.xmpp.data.RosterProvider.RosterConstants;
import umeox.xmpp.util.XmppHelper;
import alexclin.xmpp.jabberim.R;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


/**
 * 会话界面
 * @author alex
 *
 */
public class ChatsFragment extends Fragment {	
	private ListView mListView;
	private ChatListAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.frag_chat,container,false);		
		initViews(view);
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	public MainTabActivity getAct(){
		return (MainTabActivity)getActivity();
	}
	
	private void initViews(View view) {
		mListView = (ListView) view.findViewById(R.id.ChatsList_FragChat);
		String[] projection = {ChatConstants._ID,
				ChatConstants.JID,
				ChatConstants.DATE,ChatConstants.MESSAGE};
		String selction  =  ChatConstants.DATE+" in( select max("+ChatConstants.DATE+") from "+ChatProvider.TABLE_NAME+" group by "
				+ChatConstants.JID+")";
		
//		selction  =  "select * from  user where  date in( select max(date) from user  group by user ) order by date desc";
		Cursor c = getAct().getContentResolver().query(ChatProvider.CONTENT_URI, null, selction, null, ChatConstants.DATE+" desc");
		mAdapter = new ChatListAdapter(getAct(), c);
		mListView.setAdapter(mAdapter);
	}
	
	public class ChatListAdapter extends ResourceCursorAdapter{
		private SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 hh:mm",Locale.CHINESE);

		public ChatListAdapter(Context context, Cursor c) {
			super(context, R.layout.listitem_fragchat, c, FLAG_REGISTER_CONTENT_OBSERVER);
		}

		@Override
		public void bindView(View view, Context ctx, Cursor c) {
			ImageView icon = (ImageView) view.findViewById(R.id.FragChat_HeadIcon);
			TextView userName = (TextView) view.findViewById(R.id.FragChat_UserName);
			TextView lasttime = (TextView) view.findViewById(R.id.FragChat_LastTime);
			TextView lastmsg = (TextView) view.findViewById(R.id.FragChat_LastMsg);
			userName.setText(XmppHelper.getUser(c.getString(c.getColumnIndex(ChatConstants.JID))));
			lasttime.setText(sdf.format(new Date(c.getLong(c.getColumnIndex(ChatConstants.DATE)))));
			lastmsg.setText(c.getString(c.getColumnIndex(ChatConstants.MESSAGE)));
		}
		
	}
}
