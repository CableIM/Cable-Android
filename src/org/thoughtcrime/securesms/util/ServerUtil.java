package org.thoughtcrime.securesms.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerUtil {
    private static final Logger logger = Logger.getLogger(ServerUtil.class.getName());

    private HashMap<String, HashMap<String, String>> servers;
    private static HashMap<String, String> server;

    public ServerUtil(String severUrl){
        servers = new HashMap<>();
        HashMap<String, String> s1 = new HashMap<>() ;
        s1.put("GIPHY_PROXY_HOST", "giphy-proxy.cable.im");
        s1.put("GIPHY_PROXY_PORT", "80");
        s1.put("SIGNAL_URL", "https://cable-service-ca.pantelegrafo.cable.im");
        //s1.put("SIGNAL_URL", "https://cable-service.cable.im");
        servers.put("cable-service-ca.pantelegrafo.cable.im", s1);

        HashMap<String, String> s2 = new HashMap<>() ;
        s2.put("GIPHY_PROXY_HOST", "giphy-proxy.cable.im");
        s2.put("GIPHY_PROXY_PORT", "80");
        s2.put("SIGNAL_URL", "https://cable-service-ca.lattuga.cable.im");
        servers.put("cable-service-ca.lattuga.cable.im", s2);

        server = servers.get(severUrl) ;
    }

    public static HashMap<String, String> getServerConfig() {
        return server;
    }

    public static String getServerUrl() {
        return server.get("SIGNAL_URL");
    }
}