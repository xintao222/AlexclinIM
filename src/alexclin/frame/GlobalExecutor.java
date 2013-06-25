package alexclin.frame;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalExecutor {
	private static ExecutorService executorService;
	
	public static void init() {
		executorService = Executors.newFixedThreadPool(GlobalConfig.MaxThreads);
	}
	
	public static void release() {
		executorService.shutdown();
		executorService = null;
	}
	
	public static void executeAsyncRunnable(Runnable action) {
		executorService.submit(action);
	}
}
