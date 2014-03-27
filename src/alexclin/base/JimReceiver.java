package alexclin.base;

import android.content.Context;
import android.content.Intent;
import umeox.xmpp.service.XmppReceiver;

public class JimReceiver extends XmppReceiver {

	@Override
	public Intent initServiceIntent(Context context) {
		return new Intent(context, JimService.class);
	}

}
