package alexclin.ui;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class FragPagerAdapter extends FragmentPagerAdapter {
	private List<Fragment> mList;

	public FragPagerAdapter(FragmentManager fm,List<Fragment> list) {
		super(fm);
		this.mList = list;
		
	}

	@Override
	public Fragment getItem(int pos) {
		return mList.get(pos);
	}

	@Override
	public int getCount() {
		return mList.size();
	}
}
