package alexclin.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;

import umeox.xmpp.data.ChatProvider;
import umeox.xmpp.data.RosterProvider;
import alexclin.xmpp.jabberim.R;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;



/**
 * 群组界面
 * @author alex
 *
 */
public class GroupsFragment extends Fragment {
	public class GroupAdapter extends BaseAdapter {
        private List<HostedRoom> mRooms;
        private LayoutInflater mInflater;
		public GroupAdapter(){
			mInflater = LayoutInflater.from(getAct());
			mRooms = new ArrayList<HostedRoom>();
		}
		
		public void updateRooms(Collection<HostedRoom> rooms){
			mRooms.clear();
			mRooms.addAll(rooms);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mRooms.size();
		}

		@Override
		public Object getItem(int arg0) {
			return arg0;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup arg2) {
			if(convertView==null){
				convertView = mInflater.inflate(R.layout.frag_chat, null);
			}
			HostedRoom r = mRooms.get(pos);
			((TextView) convertView.findViewById(R.id.FragChat_UserName)).setText(r.getName());
			((TextView) convertView.findViewById(R.id.FragChat_LastTime)).setText("");
			((TextView) convertView.findViewById(R.id.FragChat_LastMsg)).setText("");
			return convertView;
		}

	}
	
	private ListView mListView;
	
	private GroupAdapter mAdapter;
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.frg_grouplist, container, false);
		mListView = (ListView) view.findViewById(R.id.GroupList_List);
		initViews();
		return view;
	}

	private void initViews() {
//		mAdapter = new GroupAdapter();
//		mListView.setAdapter(mAdapter);
//		try {
//			mAdapter.updateRooms();			
//		} catch (XMPPException e) {
//			e.printStackTrace();
//		} catch(Exception e){
//			e.printStackTrace();
//		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	public MainTabActivity getAct(){
		return (MainTabActivity)getActivity();
	}
}
