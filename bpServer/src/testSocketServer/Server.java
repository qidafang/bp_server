package testSocketServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import testSocketServer.component.RequestDealer;
import testSocketServer.component.Service;
import testSocketServer.model.Game;

public class Server {

	public ServerSocket serverSocket;
	public List<Service> services = new ArrayList<Service>();
	
	public List<Game> games = new ArrayList<Game>();
	public RequestDealer dealer;
	
	public void start() throws Exception{
		dealer = new RequestDealer(this);
		
		serverSocket = new ServerSocket(42769);
		while(true){
			Socket socket = serverSocket.accept();
			
			synchronized (this) {
				//创建一个“处理者”来处理新的连接
				Service d = new Service(this, socket);
				this.services.add(d);
				new Thread(d).start();
				System.out.println("客户端打开了一个连接，当前连接数：" + services.size());
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		Server server = new Server();
		server.start();
	}
}
