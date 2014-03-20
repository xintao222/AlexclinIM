package alexclin.data;

import umeox.xmpp.data.ChatProvider;

public class PhoneChatProvider extends ChatProvider {
	
	@Override
	protected void initAuthority(){
		AUTHORITY = "umeox.jim.provider.Chats";
	}	

}
