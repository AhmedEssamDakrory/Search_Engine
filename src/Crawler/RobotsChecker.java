import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;



public class RobotsChecker {
	
	private ConcurrentHashMap<String, RobotsRules> concMap;
	
	public RobotsChecker() {
		this.concMap = new ConcurrentHashMap<String, RobotsRules>();
	}
	
	private class RobotsRules{
		public List<String> disallowedUrls;
		public boolean ready;
		
		public RobotsRules() {
			disallowedUrls = new ArrayList<String>();
			ready = false;
		}
	}
	
	public boolean isUrlAllowed(String url) {
		this.putifAbsentRules(url);
		String hostName = URI.create(url).getHost();
		List<String> disallowedUrls = this.concMap.get(hostName).disallowedUrls;
		for(String disallowdUrl : disallowedUrls) {
			// match
			Pattern p = Pattern.compile(disallowdUrl);//. represents single character  
			Matcher m = p.matcher(url);  
			if(m.find()) {
				return false;
			}
		}
		return true;
	}
	
	private void putifAbsentRules(String url) {
		
		String hostName = URI.create(url).getHost();
		RobotsRules rules = this.concMap.putIfAbsent(hostName, new RobotsRules());

		if(rules == null) {
			this.update(hostName, this.parseRobotsFile(this.readRobotsFile(url)));
			return;
		}
		
		synchronized(rules) {
			while(!rules.ready) {
				try {
					rules.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public List<String> readRobotsFile(String url){
		url = URI.create(url).getScheme()+"://"+URI.create(url).getHost()+"/robots.txt";
		List<String> lines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader( new InputStreamReader(new URL(url).openStream()));
			String line = null;
	        while((line = br.readLine()) != null) {
	            lines.add(line);
	        }
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public List<String> parseRobotsFile(List<String> fileLines){
		List<String> disallowedUrls = new ArrayList<String>() ;
		String userAgent = "";
		for(String line : fileLines) {
			line = line.toLowerCase();
			if(line.startsWith("user-agent")) {
				userAgent = line.substring(line.indexOf(":")+1).trim();
			} else if(line.startsWith("disallow")) {
				if(userAgent.equals(Constants.AGENT)){
					disallowedUrls.add(this.preparePattern(line.substring(line.indexOf(":")+1).trim()));
				}
			}
		}
		return disallowedUrls;
	}
	
	public String preparePattern(String p) {
		p = p.replaceAll("\\?", "[?]"); // match "?" mark.
		p = p.replaceAll("\\*", ".*"); // if "*" match any sequence of characters.
		return p;
	}
	
	private void update(String hostName, List<String> disallowedUrls) {
		RobotsRules rules = this.concMap.get(hostName);
		synchronized(rules) {
			rules.ready = true;
			rules.disallowedUrls = disallowedUrls;
			rules.notifyAll();
		}
	}
}
