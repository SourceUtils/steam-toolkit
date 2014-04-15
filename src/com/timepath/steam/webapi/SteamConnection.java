package com.timepath.steam.webapi;

import com.timepath.web.api.base.Connection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.logging.Logger;

/**
 * http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v1/
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=60547&start=0&sid=937cd736a46b29a2492fc8770b733bd6
 * https://steamcommunity.com/public/javascript/login.js
 * https://github.com/Jessecar96/SteamBot/blob/a7ef3f402f5b7e2527b6be82d9e8c770d43eb5a0/SteamTrade/SteamWeb.cs
 * https://github.com/echeese/TradeOfferLib/blob/master/TradeOfferLib/src/main/java/com/ryanspeets/tradeoffer/TradeUser.java
 * <p>
 * @author TimePath
 */
public class SteamConnection extends Connection {

    private static final Logger LOG = Logger.getLogger(SteamConnection.class.getName());

    public SteamConnection() throws MalformedURLException {
    }

    @Override
    public String getBaseUrl() {
        return "https://steamcommunity.com/";
    }

    @Override
    public String getUserAgent() {
        return "Java steam wrapper";
    }

    @Override
    protected long mindelay() {
        return 1000L;
    }

    @Override
    protected void onConnect(HttpURLConnection con) {
        
    }

}
