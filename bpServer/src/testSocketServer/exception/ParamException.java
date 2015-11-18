package testSocketServer.exception;

public class ParamException extends RuntimeException{
	
	public String param;

	public ParamException(String param) {
		super();
		this.param = param;
	}
	
}
