package alexclin.ui.base;

import android.os.Environment;

public class GlobalConfig {
	static{
		RootDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Aim";
	}	
	public static final String RootDir;
	public static final String ImageCaheDir = RootDir+"/imagecahe";
	public static final String VoiceCaheDir = RootDir+"/voicerecord";
	public static final int MaxThreads = 10;
	public static final boolean Debug = true;

}
