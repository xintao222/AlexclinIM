package alexclin.data;

import umeox.xmpp.data.RosterProvider;

public class PhoneRosterProvider extends RosterProvider {
	
	@Override
	protected void initAuthority() {
		AUTHORITY = "umeox.jim.provider.Roster";		
	}

}
