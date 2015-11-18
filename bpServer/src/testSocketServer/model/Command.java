package testSocketServer.model;

import java.util.HashMap;
import java.util.Map;

public class Command {
	public String name;
	public Map<String, String> params = new HashMap<String, String>();
	
	public static Command fromString(String str){
		try{
			Command c = new Command();
			
			int sepLocation = str.indexOf(':');
			if(sepLocation != -1){
				c.name = str.substring(0, sepLocation);
				String paramsStr = str.substring(sepLocation + 1);
				String[] paramPairs = paramsStr.split(",");
				for(String paramPair : paramPairs){
					String[] kv = paramPair.split("=");
					String k = kv[0];
					String v = kv[1];
					c.params.put(k, v);
				}
			}else{
				c.name = str;
			}
			
			return c;
		}catch(Exception e){
			return null;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.name);
		if(params.keySet().size() > 0){
			sb.append(":");
			for(String k : params.keySet()){
				sb.append(k).append("=").append(params.get(k)).append(",");
			}
			sb.deleteCharAt(sb.length() - 1);//去掉最后一个逗号
		}
		
		return sb.toString();
	}
	
	public static void main(String[] args) {
		Command c1 = Command.fromString("getrooms");
		Command c2 = Command.fromString("createroom:name=a");
		System.out.println(c1);
		System.out.println(c2);
	}
}
