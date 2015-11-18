package testSocketServer.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import testSocketServer.Server;
import testSocketServer.model.Command;
import testSocketServer.model.Game;
import testSocketServer.model.Player;
import testSocketServer.model.ResponseAndType;

//对客户端连接的处理者
public class Service implements Runnable{
	Socket socket;
	Server server;
	BufferedReader br;
	PrintWriter os;
	
	Player player;

	public Service(Server server, Socket socket) {
		try{
			this.socket = socket;
			this.server = server;
			this.br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
			this.os = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"));
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void run(){
		String line;
		try{
			while((line = br.readLine()) != null){
				try{
					Command command = Command.fromString(line);
					if(command == null){
						this.os.println("命令格式错误");
						this.os.flush();
					}else{
						//先获得我的service和对方service对象，因为在dealCommand时可能将player和service断开连接（quit指令）
						Service myService = this;
						Service otherService = null;
						if(myService.player != null && myService.player.game != null && myService.player.game.p2 != null){
							Game theGame = myService.player.game;
							otherService = (theGame.p1 == myService.player) ? theGame.p2.service : theGame.p1.service;
						}
						
						List<ResponseAndType> rats = server.dealer.dealCommand(command, this);
						for(ResponseAndType rat : rats){
							String cmdStr = rat.command.toString();
							if(rat.type.equals("response")){//向请求者返回响应
								this.os.println(cmdStr);
								this.os.flush();
							}else if(rat.type.equals("game")){//向同一游戏中玩家返回响应
								myService.os.println(cmdStr);
								myService.os.flush();
								if(otherService == null){//如果是在dealCommand时才建立的连接，尝试将它赋值
									if(myService.player != null && myService.player.game != null && myService.player.game.p2 != null){
										otherService = myService.player == myService.player.game.p1 ? 
												myService.player.game.p2.service : myService.player.game.p1.service;
									}
								}
								if(otherService != null){
									otherService.os.println(cmdStr);
									otherService.os.flush();
								}
							}else if(rat.type.equals("all")){//向所有玩家返回响应
								for(Service service : server.services){
									service.os.println(cmdStr);
									service.os.flush();
								}
							}
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}catch(SocketException se){
			synchronized (server) {
				if(this.player != null && this.player.game != null && this.player.game.p2 != null){//可能需要通知他的对手退出了bp
					Service theOtherService = (this.player.game.p1 == this.player 
							? this.player.game.p2.service : this.player.game.p1.service);
					theOtherService.player = null;
					//向另一个玩家发送消息
					theOtherService.os.println("gameover:msg=" + this.player.name + "断开了连接，BP结束。");
					theOtherService.os.flush();
				}
				try{
					this.os.close();
					this.br.close();
					this.socket.close();
					server.services.remove(this);
					if(this.player != null && this.player.game != null){
						server.games.remove(this.player.game);
					}
				}catch(IOException ioe2){
					ioe2.printStackTrace();
				}
				System.out.println("客户端关闭了一个连接，当前连接数：" + server.services.size());
			}
		}catch(Exception ioe){
			ioe.printStackTrace();
		}
	}
	
}