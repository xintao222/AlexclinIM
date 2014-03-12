package alexclin.http;


/**
 * @Title: AuthBean.java
 * @Description: TODO
 * @author 洪锦群
 * @date 2014-3-10 下午3:33:40
 * @version V1.0
 */
public class ReturnBean<T>{
	private int errorCode; // 错误代码，0标示正常
	private String errorMsg; // 错误代码对应的消息
	private String responseDesc; //描述信息
	private T result;

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getResponseDesc() {
		return responseDesc;
	}

	public void setResponseDesc(String responseDesc) {
		this.responseDesc = responseDesc;
	}
	
}
