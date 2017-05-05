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

    public ServerUtil(String serverName){
        servers = new HashMap<>();

        HashMap<String, String> pantelegrafo = new HashMap<>();
        // cable-service.pantelegrafo.cable.im
        pantelegrafo.put("GIPHY_PROXY_HOST", "giphy-proxy.pantelegrafo.cable.im");
        pantelegrafo.put("GIPHY_PROXY_PORT", "80");
        pantelegrafo.put("SIGNAL_URL", "https://cable-service.pantelegrafo.cable.im");
        servers.put("cable-service.pantelegrafo.cable.im", pantelegrafo);

        HashMap<String, String> pantelegrafo_staging = new HashMap<>();
        // cable-service-staging.pantelegrafo.cable.im
        pantelegrafo_staging.put("GIPHY_PROXY_HOST", "giphy-proxy.pantelegrafo.cable.im");
        pantelegrafo_staging.put("GIPHY_PROXY_PORT", "80");
        pantelegrafo_staging.put("SIGNAL_URL", "https://cable-service-staging.pantelegrafo.cable.im");
        servers.put("cable-service-staging.pantelegrafo.cable.im", pantelegrafo_staging);

        HashMap<String, String> lattuga = new HashMap<>();
        // cable-service.lattuga.cable.im
        lattuga.put("GIPHY_PROXY_HOST", "giphy-proxy.lattuga.cable.im");
        lattuga.put("GIPHY_PROXY_PORT", "80");
        lattuga.put("SIGNAL_URL", "https://cable-service.lattuga.cable.im");
        servers.put("cable-service.lattuga.cable.im", lattuga);

        server = servers.get(serverName) ;
    }

    public static HashMap<String, String> getServerConfig() {
        return server;
    }

    public static String getServerUrl() {
        return server.get("SIGNAL_URL");
    }
}