package alexclin.mediatransfer;

import com.google.gson.Gson;

import alexclin.http.FileMessage;

public class FileMessager {
	public static final int TYPE_VOICE = 1;
	public static final int TYPE_IMAGE = 2;
	private static final String MARK = "::|:|::!@#";
	private static Gson gson = new Gson();

	public static String wrapMessage(String url, int type) {
		FileMessage fm = new FileMessage();
		fm.setType(type);
		fm.setUrl(url);
		return MARK + gson.toJson(fm) + MARK;
	}
	
	public static final boolean isWrappedMsg(String msg){
		return msg.startsWith(MARK)&&msg.endsWith(MARK);
	}
	
	public static FileMessage unwrappMessage(String msg){
		return gson.fromJson(msg.replace(MARK, ""), FileMessage.class);
	}
}
