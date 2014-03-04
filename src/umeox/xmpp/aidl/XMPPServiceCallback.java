package umeox.xmpp.aidl;

public interface XMPPServiceCallback {
	void newMessage(String from, String messageBody, boolean silent_notification);
	void messageError(String from, String errorBody, boolean silent_notification);
	void connectionStateChanged();
}
