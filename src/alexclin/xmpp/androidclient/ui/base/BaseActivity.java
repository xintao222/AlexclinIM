package alexclin.xmpp.androidclient.ui.base;

import java.util.ArrayList;
import java.util.List;

import alexclin.xmpp.androidclient.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 
 * @Title: AbstractActivity
 * @Package com.fullteem.smmsclient
 * @Description: 自定义Activity基类，包含一个顶部标题栏（标题，左按钮，右按钮）
 * @author Alexclin
 * @date 2013-4-10 下午6:04:59
 * @version V1.0
 */
public abstract class BaseActivity extends Activity {
	public static ArrayList<Activity> mActivityStack;
	static{
		mActivityStack = new ArrayList<Activity>();
	}
	private Button mLeftBtn;
	private Button mRightBtn;
	private TextView mTitleTv;
	private TextView mMenuTitleTv;
	private LinearLayout mTitleLayout;
	private LinearLayout mStandardLayout;
	private RelativeLayout mCoverLayout;
	private View mInnerCoverView;
	private List<String> mMenuList;
	private MenuAdapter mMenuAdapter;	
	private OnItemClickListener mItemClickListener;
	private ListView mMenuLv;
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActivityStack.add(this);
	}
	
	@Override
	protected void onDestroy() {
		if(mActivityStack.contains(this))
			mActivityStack.remove(this);
		super.onDestroy();
	}
	
	public static final void exit(){
		for (Activity activity : mActivityStack) {
			activity.finish();
		}
	}

	public final void setContentView(int layoutResID,boolean isTitleVisable) {
		LayoutInflater inflater = this.getLayoutInflater();
		RelativeLayout rootLayout = (RelativeLayout) inflater.inflate(
				R.layout.act_abstract, null);
		mStandardLayout = (LinearLayout) rootLayout
				.findViewById(R.id.StandardView_Abstract);
		mCoverLayout = (RelativeLayout) rootLayout
				.findViewById(R.id.CoverView_Abstract);
		View v = inflater.inflate(layoutResID, null);
		mStandardLayout.addView(v, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		mProgressDialog = new ProgressDialog(this);
		super.setContentView(rootLayout);
		initItems(isTitleVisable);
	}

	private void initItems(boolean isTitleVisable) {
		mLeftBtn = (Button) findViewById(R.id.LeftText_Abstract);
		mRightBtn = (Button) findViewById(R.id.RightText_Abstract);
		mTitleTv = (TextView) findViewById(R.id.TitleText_Abstract);
		mTitleLayout = (LinearLayout) findViewById(R.id.TitleLayout_Abstract);
		mLeftBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				BaseActivity.this.finish();
			}
		});	
		if(!isTitleVisable){
			mTitleLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * 设置左按钮是否显示
	 * @param isVisable
	 */
	public final void setLeftButtonVisable(boolean isVisable){
		if(isVisable){
			mLeftBtn.setVisibility(View.VISIBLE);
		}else{
			mLeftBtn.setVisibility(View.GONE);
		}
	}
	/**
	 * 设置左按钮文字
	 * 
	 * @param leftText
	 */
	public final void setLeftButton(CharSequence leftText) {
		mLeftBtn.setText(leftText);
	}

	/**
	 * 设置左按钮文字
	 * 
	 * @param resId
	 */
	public final void setLeftButton(int resId) {
		mLeftBtn.setText(resId);
	}

	/**
	 * 设置左按钮文字和监听器
	 * 
	 * @param leftText
	 * @param listener
	 */
	public final void setLeftButton(CharSequence leftText,
			OnClickListener listener) {
		mLeftBtn.setText(leftText);
		mLeftBtn.setOnClickListener(listener);
	}

	/**
	 * 设置左按钮文字和监听器
	 * 
	 * @param resId
	 * @param listener
	 */
	public final void setLeftButton(int resId, OnClickListener listener) {
		mLeftBtn.setText(resId);
		mLeftBtn.setOnClickListener(listener);
	}

	/**
	 * 设置左按钮文字，图片和监听器
	 * 
	 * @param leftText
	 * @param drawable
	 * @param listener
	 */
	public final void setLeftButton(CharSequence leftText, int drawable,
			OnClickListener listener) {
		mLeftBtn.setText(leftText);
		mLeftBtn.setOnClickListener(listener);
		mLeftBtn.setBackgroundResource(drawable);
	}

	/**
	 * 设置左按钮文字，图片和监听器
	 * 
	 * @param resId
	 * @param drawable
	 * @param listener
	 */
	public final void setLeftButton(int resId, int drawable,
			OnClickListener listener) {
		mLeftBtn.setText(resId);
		mLeftBtn.setOnClickListener(listener);
		mLeftBtn.setBackgroundResource(drawable);
	}

	public final void setLeftButton(OnClickListener listener) {
		mLeftBtn.setOnClickListener(listener);
	}

	public final void setLeftButton(OnClickListener listener, int drawable) {
		mLeftBtn.setOnClickListener(listener);
		mLeftBtn.setBackgroundResource(drawable);
	}

	public final void setRightButton(CharSequence leftText) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setText(leftText);
	}

	public final void setRightButton(int resId) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setText(resId);
	}

	public final void setRightButton(CharSequence leftText,
			OnClickListener listener) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setText(leftText);
		mRightBtn.setOnClickListener(listener);
	}

	public final void setRightButton(int resId, OnClickListener listener) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setText(resId);
		mRightBtn.setOnClickListener(listener);
	}

	public final void setRightButton(CharSequence leftText, int drawable,
			OnClickListener listener) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setText(leftText);
		mRightBtn.setOnClickListener(listener);
		mRightBtn.setBackgroundResource(drawable);
	}

	public final void setRightButton(int resId, int drawable,
			OnClickListener listener) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setText(resId);
		mRightBtn.setOnClickListener(listener);
		mRightBtn.setBackgroundResource(drawable);
	}

	public final void setRightButton(OnClickListener listener, int drawable) {
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setOnClickListener(listener);
		mRightBtn.setBackgroundResource(drawable);
	}

	public final void setRightButton(OnClickListener listener) {
		mRightBtn.setOnClickListener(listener);
		setRightButtonVisble(true);
	}

	public final void setRightButtonVisble(boolean isVisble) {
		if (isVisble) {
			mRightBtn.setVisibility(View.VISIBLE);
		} else {
			mRightBtn.setVisibility(View.GONE);
		}
	}

	public final void setTitle(CharSequence leftText) {
		mTitleTv.setText(leftText);
	}

	public final void setTitle(int strId) {
		mTitleTv.setText(strId);
	}

	public final void setTitleDrawable(int resId) {
		mTitleTv.setText("");
		mTitleTv.setBackgroundResource(resId);
	}

	public final void setTitlebarBackgroud(int resId) {
		mTitleLayout.setBackgroundResource(resId);
	}

	public final void setActivityBackgroud(int resId) {
		mStandardLayout.setBackgroundResource(resId);
	}

	/**
	 * 设置覆盖视图，用于需要覆盖整个Activity界面的View效果
	 * 
	 * @param v
	 */
	public final void setCoverView(View v) {
		mCoverLayout.removeAllViews();
		mCoverLayout.addView(v, new RelativeLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	/**
	 * 设置覆盖视图，用于需要覆盖整个Activity界面的View效果
	 * 
	 * @param resId
	 */
	public final void setCoverView(int resId) {
		View v = View.inflate(this, resId, null);
		setCoverView(v);
	}

	public final void setTitleShowable(boolean isShow) {
		if (isShow) {
			mTitleLayout.setVisibility(View.VISIBLE);
		} else {
			mTitleLayout.setVisibility(View.GONE);
		}
	}
	
	public final void switchCoverView(){
		if(mCoverLayout.getVisibility() == View.GONE){
			mCoverLayout.setVisibility(View.VISIBLE);
		}else{
			mCoverLayout.setVisibility(View.GONE);
		}
	}
	
	public final void showCoverMenu(){
		switchCoverView();
	}
	
	public final void showCoverView(boolean isShow){
		if(isShow){
			mCoverLayout.setVisibility(View.VISIBLE);
		}else{
			mCoverLayout.setVisibility(View.GONE);
		}
	}
	
	public final void showProgressDialog(String msg){
		mProgressDialog.setMessage(msg);
		mProgressDialog.show();
	}
	
	public final void showProgressDialog(int resId){
		String msg = getString(resId);
		showProgressDialog(msg);
	}
	
	public final void dismissProgressDialog(){
		mProgressDialog.dismiss();
	}
	
	public final void initCoverMenu(String titleStr,List<String> list,OnItemClickListener listener){
		mCoverLayout.setVisibility(View.GONE);
		mInnerCoverView = this.getLayoutInflater().inflate(R.layout.act_abstract_dialog, null);		
		mMenuTitleTv = (TextView)mInnerCoverView.findViewById(R.id.DialogTitle_Abstract);
		mMenuAdapter = new MenuAdapter();
		mCoverLayout.setOnClickListener(mMenuAdapter);
		mMenuLv = (ListView)mInnerCoverView.findViewById(R.id.DialogList_Abstract);
		mMenuLv.setAdapter(mMenuAdapter);
		mItemClickListener = listener;
		mMenuList = list;
		if(titleStr!=null){
			mMenuTitleTv.setText(titleStr);
			mMenuTitleTv.setVisibility(View.VISIBLE);
		}
		setCoverView(mInnerCoverView);
	}
	
	public final void initCoverMenu(List<String> list,OnItemClickListener listener){
		initCoverMenu(null, list, listener);
	}
	
	class MenuAdapter extends BaseAdapter implements OnClickListener{
		@Override
		public int getCount() {
			if(mMenuList == null || mMenuList.size()==0){
				return 1;
			}
			return mMenuList.size()+1;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				convertView = BaseActivity.this.getLayoutInflater().inflate(R.layout.act_abstract_item, null);				
			}
			Button btn = (Button)convertView.findViewById(R.id.Btn_AbstractList);
			btn.setOnClickListener(new BtnListener(position));
			if(position== (getCount()-1)){
				btn.setText("取消");
				btn.setTextColor(getResources().getColor(android.R.color.white));
			}else{
				btn.setText(mMenuList.get(position));
				btn.setTextColor(getResources().getColor(android.R.color.black));
			}
			return convertView;			
		}
		
		class BtnListener implements OnClickListener{
            private int pos;
            
			public BtnListener(int pos) {
				super();
				this.pos = pos;
			}

			@Override
			public void onClick(View v) {
				if(pos != getCount()-1){
					mItemClickListener.onItemClick(mMenuLv, mMenuLv.getChildAt(pos), pos, pos);
				}
				switchCoverView();
			}
			
		}

		@Override
		public void onClick(View v) {
			if(v.getId()==mCoverLayout.getId()){
				switchCoverView();
				return;
			}			
		}
	}
}
