package umeox.xmpp.base;

public class UmeoxException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public UmeoxException(Throwable e) {
		super(e);
	}
	
	public UmeoxException(String message) {
		super(message);
	}

	public UmeoxException(String message, Throwable cause) {
		super(message, cause);
	}
}
