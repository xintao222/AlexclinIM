package alexclin.xmpp.androidclient.ui.base;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled") public class Webview extends BaseActivity {	
	public static final String URL = "url";
	private WebView mWevView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		mWevView = new WebView(this);
		mWevView.setWebViewClient(new WebViewClient());
		WebSettings webSettings = mWevView.getSettings();
		webSettings.setSupportZoom(true);
		webSettings.setJavaScriptEnabled(true);
		mWevView.requestFocus();
		setContentView(mWevView,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
		String url = getIntent().getStringExtra(URL);
		if(url!=null){
			mWevView.loadUrl(url);
//			mWevView.loadUrl("file:///android_asset/procotol_user.html");
		}
		
	}
}
