package alexclin.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jivesoftware.smack.util.StringUtils;

import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.ChatProvider.ChatConstants;
import umeox.xmpp.data.RosterProvider;
import umeox.xmpp.data.RosterProvider.RosterConstants;
import alexclin.xmpp.jabberim.R;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
		Cursor c = getAct().getContentResolver().query(ChatProvider.CONTENT_URI, projection, selction, null, ChatConstants.DATE+" desc");
		mAdapter = new ChatListAdapter(getAct(), c);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mAdapter);
	}
	
	public class ChatListAdapter extends ResourceCursorAdapter implements OnItemClickListener{
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
			String jid = c.getString(c.getColumnIndex(ChatConstants.JID));
			Cursor rc = getAct().getContentResolver().query(RosterProvider.CONTENT_URI,new String[]{RosterConstants._ID,RosterConstants.ALIAS},
					RosterConstants.JID+" = ?", new String[]{jid}, null);
			String name = StringUtils.parseName(jid);
			if(rc!=null&&rc.moveToNext()){
				name = rc.getString(rc.getColumnIndex(RosterConstants.ALIAS));
			}
			userName.setText(name);
			lasttime.setText(sdf.format(new Date(c.getLong(c.getColumnIndex(ChatConstants.DATE)))));
			lastmsg.setText(c.getString(c.getColumnIndex(ChatConstants.MESSAGE)));
		}

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			Cursor c = (Cursor) getItem(pos);
			String jid = c.getString(c.getColumnIndex(ChatConstants.JID));
			String name = getName(jid);
			Intent chatIntent = new Intent(getAct(),
					alexclin.ui.chat.ChatActivity.class);
			Uri userNameUri = Uri.parse(jid);
			chatIntent.setData(userNameUri);
			chatIntent.putExtra(alexclin.ui.chat.ChatActivity.INTENT_EXTRA_USERNAME, name);			
			startActivity(chatIntent);
		}
		
		private String getName(String jid){
			Cursor rc = getAct().getContentResolver().query(RosterProvider.CONTENT_URI,new String[]{RosterConstants._ID,RosterConstants.ALIAS},
					RosterConstants.JID+" = ?", new String[]{jid}, null);	
			String name = null;
			if(rc!=null&&rc.moveToNext()){
				name = rc.getString(rc.getColumnIndex(RosterConstants.ALIAS));
			}
			rc.close();
			if(name==null){
				name =StringUtils.parseName(jid); 
			}
			return name;
		}		
	}
}
