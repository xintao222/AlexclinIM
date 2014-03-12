package alexclin.base;

import umeox.xmpp.base.BaseApp;
import umeox.xmpp.base.BaseConfig;
import alexclin.base.CrashHandler.CrashAble;
import android.content.Context;
import android.preference.PreferenceManager;

public class MyApplication extends BaseApp implements CrashAble {
	private MyConfig mConfig;

	@Override
	public void onCreate() {
		mConfig = new MyConfig(PreferenceManager.getDefaultSharedPreferences(this));
		Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance(this, this));
	}

	public static MyApplication getApp(Context ctx) {
		return (MyApplication) ctx.getApplicationContext();
	}

	@Override
	public void sendErrorToServer(String errorInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BaseConfig getConfig() {
		return mConfig;
	}
}
