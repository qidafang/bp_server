package testSocketServer.component;

import java.util.ArrayList;
import java.util.List;

import testSocketServer.Server;
import testSocketServer.exception.ParamException;
import testSocketServer.model.Command;
import testSocketServer.model.Game;
import testSocketServer.model.Player;
import testSocketServer.model.ResponseAndType;

public class RequestDealer {
	
	Server server;
	
	public RequestDealer(Server server) {
		super();
		this.server = server;
	}

	public List<ResponseAndType> dealCommand(Command c, Service s){
		try{
			switch(c.name){
			case "getgames":
				return getgames();
			case "creategame":
				return creategame(c, s);
			case "joingame":
				return joingame(c, s);
			case "bp":
				return bp(c, s);
			case "quit":
				return quit(c, s);
			default:
				return badrequest("[允许的指令]getgames|creategame:name=xx|joingame:game=xx,name=yy|bp:hero=ww");
			}
		}catch(ParamException p){
			return badrequest(p.param);
		}catch(Exception e){
			e.printStackTrace();
			return badrequest("未知错误");
		}
	}
	
	private Command _games(){
		synchronized (server) {
			StringBuffer gamesStr = new StringBuffer();
			List<Game> games = server.games;
			for(Game g : games){
				if(g.p2 == null){
					gamesStr.append(g.p1.name).append("|");
				}
			}
			if(gamesStr.length() > 0){
				gamesStr.deleteCharAt(gamesStr.length() - 1);
			}
			Command result = new Command();
			result.name = "games";
			result.params.put("games", gamesStr.toString());
			return result;
		}
	}
	
	private boolean _checkNameValid(String name){
		synchronized (server) {
			for(Game g : server.games){
				if(g.p1.name.equals(name)){
					return false;
				}else if(g.p2 != null && g.p2.name.equals(name)){
					return false;
				}
			}
			return true;
		}
	}
	
	private List<ResponseAndType> _toList(ResponseAndType... rats){
		List<ResponseAndType> l = new ArrayList<ResponseAndType>();
		for(ResponseAndType rat : rats){
			l.add(rat);
		}
		return l;
	}
	
	private void assertParam(Command c, String... keys){
		for(String k : c.params.keySet()){
			String v = c.params.get(k);
			if(v == null || v.equals("")){
				throw new ParamException(k);
			}
		}
	}
	
	private List<ResponseAndType> getgames(){
		Command games = _games();
		return _toList(new ResponseAndType(games, "response"));
	}
	
	private List<ResponseAndType> creategame(Command c, Service s){
		assertParam(c, "name");
		synchronized (server) {
			String name = c.params.get("name");
			if(!_checkNameValid(name)){
				return badrequest("用户名已存在");
			}
			Game newGame = new Game();
			Player p1 = new Player();
			p1.name = name;
			p1.service = s;
			s.player = p1;
			newGame.p1 = p1;
			p1.game = newGame;
			newGame.turn = -1;
			server.games.add(newGame);
			
			Command games = _games();
			ResponseAndType gamesRat = new ResponseAndType(games, "all");
			Command game = newGame.toCommand();
			ResponseAndType gameRat = new ResponseAndType(game, "response");
			return _toList(gamesRat, gameRat);
		}
	}
	
	private List<ResponseAndType> joingame(Command c, Service s){
		assertParam(c, "game", "name");
		synchronized (server) {
			String game = c.params.get("game");
			String name = c.params.get("name");
			for(Game g : server.games){
				if(g.p1.name.equals(game)){
					if(g.p2 == null){
						if(_checkNameValid(name)){
							Player p2 = new Player();
							p2.name = name;
							p2.service = s; s.player = p2;
							g.p2 = p2; p2.game = g;
							g.turn = 0;

							Command gamesCmd = _games();
							ResponseAndType gamesRat = new ResponseAndType(gamesCmd, "all");
							Command gameCmd = g.toCommand();
							ResponseAndType gameRat = new ResponseAndType(gameCmd, "game");
							return _toList(gamesRat, gameRat);
						}else{
							return badrequest("用户名已存在");
						}
					}
				}
			}
			return badrequest("加入游戏错误");
		}
	}
	
	private List<ResponseAndType> bp(Command c, Service s){
		assertParam(c, "hero");
		synchronized (server) {
			Game g = s.player.game;
			String hero = c.params.get("hero");
			g.turn += 1;
			Command bp = new Command();
			bp.name = "bp";
			bp.params.put("hero", hero);
			ResponseAndType rat = new ResponseAndType(bp, "game");
			return _toList(rat);
		}
	}

	private List<ResponseAndType> quit(Command c, Service s){
		synchronized (server) {
			Game theGame = s.player.game;
			
			if(s.player.game.p2 != null){//有其他玩家
				boolean iamP1 = theGame.p1 == s.player;
				Player otherPlayer = iamP1 ? theGame.p2 : theGame.p1;
				Service otherService = otherPlayer.service;
				otherService.player = null;//其他玩家service->player解绑
				otherPlayer.service = null;//其他玩家player->service解绑
				otherPlayer.game = null;//其他玩家player->game解绑
			}
			
			Player me = s.player;
			s.player = null;//本玩家service->player解绑
			me.service = null;//本玩家player->service解绑
			me.game = null;//本玩家player->game解绑
			//本玩家和其他玩家game->player解绑
			theGame.p1 = null;
			theGame.p2 = null;
			
			server.games.remove(theGame);
			
			Command gameover = new Command();
			gameover.name = "gameover";
			gameover.params.put("msg", me.name + "退出了BP，BP结束。");
			ResponseAndType rat = new ResponseAndType(gameover, "game");
			

			Command games = _games();
			ResponseAndType gamesRat = new ResponseAndType(games, "all");
			return _toList(rat, gamesRat);
		}
	}
	
	private List<ResponseAndType> badrequest(String msg){
		Command bad = new Command();
		bad.name = "error";
		bad.params.put("msg", msg);
		ResponseAndType rat = new ResponseAndType(bad, "response");
		return _toList(rat);
	}
}
