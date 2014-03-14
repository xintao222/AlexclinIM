package umeox.xmpp.base;


import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import umeox.xmpp.util.PrefConsts;
import umeox.xmpp.util.XmppHelper;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.util.Log;

public abstract class BaseConfig implements OnSharedPreferenceChangeListener {
	
	/**
	 * Path to the trust store in this system.
	 */
	public final static String TRUST_STORE_PATH;
	static {
		String path = System.getProperty("javax.net.ssl.trustStore");
		if (path == null)
			TRUST_STORE_PATH = System.getProperty("java.home") + File.separator
					+ "etc" + File.separator + "security" + File.separator
					+ "cacerts.bks";
		else
			TRUST_STORE_PATH = path;
	}

	private static final String TAG = "xmpp.Configuration";

	private static final HashSet<String> RECONNECT_PREFS = new HashSet<String>(Arrays.asList(
				PrefConsts.JID,
				PrefConsts.PASSWORD,
				PrefConsts.CUSTOM_SERVER,
				PrefConsts.PORT,
				PrefConsts.RESSOURCE,
				PrefConsts.FOREGROUND,
				PrefConsts.REQUIRE_SSL,
				PrefConsts.SMACKDEBUG
			));
	private static final HashSet<String> PRESENCE_PREFS = new HashSet<String>(Arrays.asList(
				PrefConsts.MESSAGE_CARBONS,
				PrefConsts.PRIORITY,
				PrefConsts.STATUS_MODE,
				PrefConsts.STATUS_MESSAGE
			));

	public String password;
	public String ressource;
	public int port;
	public int priority;
	public boolean foregroundService;
	public boolean autoConnect;
	public boolean messageCarbons;
	public boolean reportCrash;
	public String customServer;
	public String jabberID;
	public boolean require_ssl;

	public String statusMode;
	public String statusMessage;

	public boolean isLEDNotify;
	public String vibraNotify;
	public Uri notifySound;
	public boolean ticker;

	public boolean smackdebug;
    public String theme;
    public String chatFontSize;
    public boolean showOffline;

    public boolean reconnect_required = false;
    public boolean presence_required = false;

	private final SharedPreferences prefs;

	public BaseConfig(SharedPreferences _prefs) {
		initParam(_prefs);
		prefs = _prefs;
		prefs.registerOnSharedPreferenceChangeListener(this);
		loadPrefs(prefs);
	}

	protected abstract void initParam(SharedPreferences _prefs);

	@Override
	protected void finalize() {
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		Log.i(TAG, "onSharedPreferenceChanged(): " + key);
		loadPrefs(prefs);
		if (RECONNECT_PREFS.contains(key))
			reconnect_required = true;
		if (PRESENCE_PREFS.contains(key))
			presence_required = true;
	}

	private int validatePriority(int jabPriority) {
		if (jabPriority > 127)
			return 127;
		else if (jabPriority < -127)
			return -127;
		return jabPriority;
	}

	private void loadPrefs(SharedPreferences prefs) {
		this.isLEDNotify = prefs.getBoolean(PrefConsts.LEDNOTIFY,
				false);
		this.vibraNotify = prefs.getString(
				PrefConsts.VIBRATIONNOTIFY, "SYSTEM");
		this.notifySound = Uri.parse(prefs.getString(
				PrefConsts.RINGTONENOTIFY, ""));
		this.ticker = prefs.getBoolean(PrefConsts.TICKER,
				true);
		this.password = prefs.getString(PrefConsts.PASSWORD, "");
		this.ressource = prefs
				.getString(PrefConsts.RESSOURCE, "JabberIM");
		this.port = XmppHelper.tryToParseInt(prefs.getString(
				PrefConsts.PORT, PrefConsts.DEFAULT_PORT),
				PrefConsts.DEFAULT_PORT_INT);

		this.priority = validatePriority(XmppHelper.tryToParseInt(prefs
				.getString(PrefConsts.PRIORITY, "0"), 0));

		this.foregroundService = prefs.getBoolean(PrefConsts.FOREGROUND, true);

		this.autoConnect = prefs.getBoolean(PrefConsts.CONN_STARTUP,
				false);
		this.messageCarbons = prefs.getBoolean(
				PrefConsts.MESSAGE_CARBONS, true);

		this.smackdebug = prefs.getBoolean(PrefConsts.SMACKDEBUG,
				false);
		this.reportCrash = prefs.getBoolean(PrefConsts.REPORT_CRASH,
				false);
		this.jabberID = prefs.getString(PrefConsts.JID, "");
		this.customServer = prefs.getString(PrefConsts.CUSTOM_SERVER,
				"");
		this.require_ssl = prefs.getBoolean(PrefConsts.REQUIRE_SSL,
				false);
		this.statusMode = prefs.getString(PrefConsts.STATUS_MODE, "available");
		this.statusMessage = prefs.getString(PrefConsts.STATUS_MESSAGE, "");
        this.theme = prefs.getString(PrefConsts.THEME, "dark");
        this.chatFontSize = prefs.getString("setSizeChat", "18");
        this.showOffline = prefs.getBoolean(PrefConsts.SHOW_OFFLINE, false);		
	}
	
	/**
	 * identity name and type, see:http://xmpp.org/registrar/disco-categories.html
	 * @return
	 */
	public abstract String getIdentityName();
	public abstract String getIdentityType();
	public abstract int getTheme();
}
