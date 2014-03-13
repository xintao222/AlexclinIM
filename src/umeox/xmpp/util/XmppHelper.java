package umeox.xmpp.util;

import java.util.Locale;

import org.jivesoftware.smack.XMPPConnection;

import gnu.inet.encoding.Stringprep;
import gnu.inet.encoding.StringprepException;
import umeox.xmpp.base.UmeoxException;
import android.content.Context;
import android.text.Editable;
import android.util.TypedValue;


public class XmppHelper {

	public static String verifyJabberID(String jid)
			throws UmeoxException {
		try {
			String parts[] = jid.split("@");
			if (parts.length != 2 || parts[0].length() == 0 || parts[1].length() == 0)
				throw new UmeoxException(
						"Configured Jabber-ID is incorrect!");
			StringBuilder sb = new StringBuilder();
			sb.append(Stringprep.nodeprep(parts[0]));
			sb.append("@");
			sb.append(Stringprep.nameprep(parts[1]));
			return sb.toString();
		} catch (StringprepException spe) {
			throw new UmeoxException(spe);
		} catch (NullPointerException e) {
			throw new UmeoxException("Jabber-ID wasn't set!");
		}
	}

	public static String verifyJabberID(Editable jid)
			throws UmeoxException {
		return verifyJabberID(jid.toString());
	}
	
	public static int tryToParseInt(String value, int defVal) {
		int ret;
		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException ne) {
			ret = defVal;
		}
		return ret;
	}

	public static String capitalizeString(String original) {
		return (original.length() == 0) ? original :
			original.substring(0, 1).toUpperCase(Locale.CHINA) + original.substring(1);
	}

	public static int getEditTextColor(Context ctx) {
		TypedValue tv = new TypedValue();
		boolean found = ctx.getTheme().resolveAttribute(android.R.attr.editTextColor, tv, true);
		if (found) {
			// SDK 11+
			return ctx.getResources().getColor(tv.resourceId);
		} else {
			// SDK < 11
			return ctx.getResources().getColor(android.R.color.primary_text_light);
		}
	}
	
	public static boolean verifyUserAndPW(String jid,String password){
		if(jid.matches(" *")||password.length()<6){
			return false;
		}else{			
			return true;
		}
	}
	
	public static String getUserAtHost(String user,XMPPConnection con){
		return user +"@"+con.getUser().split("@")[1].split("/")[0];
	}
	
	public static String getHostName(XMPPConnection con){
		return con.getUser().split("@")[1].split("/")[0];
	}
}
