package umeox.xmpp.service;

import java.util.Collection;

import org.jivesoftware.smackx.muc.HostedRoom;

import umeox.xmpp.aidl.XMPPServiceCallback;
import umeox.xmpp.base.UmeoxException;
import umeox.xmpp.util.ConnectionState;


public interface Smackable {
	boolean doConnect(boolean create_account) throws UmeoxException;
	boolean isAuthenticated();
	void requestConnectionState(ConnectionState new_state);
	void requestConnectionState(ConnectionState new_state, boolean create_account);
	ConnectionState getConnectionState();
	String getLastError();

	void addRosterItem(String user, String alias, String group,String msg) throws UmeoxException;
	void removeRosterItem(String user) throws UmeoxException;
	void renameRosterItem(String user, String newName) throws UmeoxException;
	void moveRosterItemToGroup(String user, String group) throws UmeoxException;
	void renameRosterGroup(String group, String newGroup);
	void sendPresenceRequest(String user, String type);
	void addRosterGroup(String group);
	
	void setStatusFromConfig();
	void sendMessage(String user, String message);
	void sendServerPing();
	
	void registerCallback(XMPPServiceCallback callBack);
	void unRegisterCallback();
	
	String getNameForJID(String jid);
	
	Collection<HostedRoom> getHostedRooms();
}
