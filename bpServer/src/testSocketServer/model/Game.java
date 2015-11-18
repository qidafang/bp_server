package testSocketServer.model;

public class Game {
	public Player p1;
	public Player p2;
	public int turn;
	
	public Command toCommand(){
		Command c = new Command();
		c.name = "game";
		c.params.put("p1", p1.name);
		if(p2 != null){
			c.params.put("p2", p2.name);
		}
		c.params.put("turn", String.valueOf(turn));
		return c;
	}
}
