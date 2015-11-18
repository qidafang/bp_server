package testSocketServer.model;

public class ResponseAndType {
	public Command command;
	public String type;//user || game || all
	
	public ResponseAndType(Command command, String type) {
		super();
		this.command = command;
		this.type = type;
	}
}
