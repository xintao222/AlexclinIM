package alexclin.ui;

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



/**
 * 群组界面
 * @author alex
 *
 */
public class GroupsFragment extends Fragment {
	public class GroupAdapter extends BaseAdapter {
//		private List<>
		public GroupAdapter(){
			
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private RosterObserver mRosterObserver;
	private ListView mListView;
	
	private GroupAdapter mAdapter;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mRosterObserver = new RosterObserver();
		getActivity().getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI, true, mRosterObserver);
		getActivity().getContentResolver().registerContentObserver(ChatProvider.CONTENT_URI, true, mRosterObserver);		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.frg_grouplist, container, false);
		mListView = (ListView) view.findViewById(R.id.GroupList_List);
		return view;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	public Handler getHandler(){
		return ((MainTabActivity)getActivity()).getHadnler();
	}
	
	private class RosterObserver extends ContentObserver{
		public RosterObserver() {
			super(getHandler());
		}

		@Override
		public void onChange(boolean selfChange) {
			
		}
	}
}
