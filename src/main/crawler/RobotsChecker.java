package main.crawler;

import main.utilities.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RobotsChecker {
	// map to hold the hosts which are visited and their robots file is prepared or is being prepared.
    private ConcurrentHashMap<String, RobotsRules> concMap;

    public RobotsChecker() {
        this.concMap = new ConcurrentHashMap<String, RobotsRules>();
    }
    
    /**
     * Holds the disallowed URLs for each host.
     * ready flag to indicate that the robots file of this host is prepared.
     * 
     * @author AhmedEssam
     *
     */
    private class RobotsRules {
        public List<String> disallowedUrls;
        public boolean ready;

        public RobotsRules() {
            disallowedUrls = new ArrayList<String>();
            ready = false;
        }
    }

    /**
     * Return true if the URLs is allowed by robots.
     * @param url
     * @return
     */
    public boolean isUrlAllowed(String url) {
        this.putifAbsentRules(url);
        String hostName = null;
        try {
            hostName = new URL(url).getHost();
        } catch (MalformedURLException e) {
            return false;
        }

        List<String> disallowedUrls = this.concMap.get(hostName).disallowedUrls;
        for (String disallowdUrl : disallowedUrls) {
            // match
            Pattern p = Pattern.compile(disallowdUrl);//. represents single character
            Matcher m = p.matcher(url);
            if (m.find()) {
                LogOutput.printMessage("URL disallowed By robots.txt : " + url);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the host and its rules not visited before, put it in the hash map then go to prepare the rules making the ready flag
     * false to prevent the other threads to do the same thing and wait for the rule to be ready. 
     * @param url
     */
    private void putifAbsentRules(String url) {
        String hostName = null;
        try {
            hostName = new URL(url).getHost();
        } catch (MalformedURLException e1) {
            //e1.printStackTrace();
        }

        if (hostName == null) {
            return;
        }
        // if absent put it in the map
        RobotsRules rules = this.concMap.putIfAbsent(hostName, new RobotsRules());
        // if null this means it was not visited.
        if (rules == null) {
            LogOutput.printMessage("Host : " + hostName + " first time prepare robots.txt");
            // go prepare the rules for this host.
            this.update(hostName, this.parseRobotsFile(this.readRobotsFile(url)));
            return;
        }
        // if rules not equal null, that means that the robots text of this host is being prepared,
        // wait for it to be ready by checking the ready flag.
        synchronized (rules) {
            while (!rules.ready) {
                try {
                    LogOutput.printMessage("Host : " + hostName + " Waiting for robots.txt file");
                    rules.wait();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    /**
     *  read robots file and return list of its lines
     * @param url
     * @return
     */
    public List<String> readRobotsFile(String url) {
        List<String> lines = new ArrayList<String>();
        try {
            URL ur = new URL(url);
            url = ur.getProtocol() + "://" + ur.getHost() + "/robots.txt";
        } catch (MalformedURLException e1) {
            return lines;
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return lines;
    }

    /**
     * parse the robots file lines and extract the disallowed URLs.
     * @param fileLines
     * @return
     */
    public List<String> parseRobotsFile(List<String> fileLines) {
        List<String> disallowedUrls = new ArrayList<String>();
        String userAgent = "";
        for (String line : fileLines) {
            line = line.toLowerCase();
            if (line.startsWith("user-agent")) {
                userAgent = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("disallow")) {
                if (userAgent.equals(Constants.AGENT)) {
                    disallowedUrls.add(this.preparePattern(line.substring(line.indexOf(":") + 1).trim()));
                }
            }
        }
        return disallowedUrls;
    }

    /**
     * Prepare Regex pattern of the disallowed URls so that can compare the sent URLs with them by regular expressions
     * @param p
     * @return
     */
    public String preparePattern(String p) {
        p = p.replaceAll("\\.", "\\\\.");
        p = p.replaceAll("\\?", "[?]"); // match "?" mark.
        p = p.replaceAll("\\*", ".*"); // if "*" match any sequence of characters.
        p = p.replaceAll("\\{", "%7B");
        p = p.replaceAll("\\}", "%7D");
        return p;
    }

    /**
     * After parsing the Robots file and all rules are ready, 
     * update the ready flag of the host rules and assign them.
     * 
     * @param hostName
     * @param disallowedUrls
     */
    private void update(String hostName, List<String> disallowedUrls) {
        RobotsRules rules = this.concMap.get(hostName);
        synchronized (rules) {
            rules.ready = true;
            rules.disallowedUrls = disallowedUrls;
            rules.notifyAll();
        }
    }
}
