package com.timepath.steam.webapi

import com.timepath.web.api.base.Connection

import java.net.HttpURLConnection
import java.util.logging.Logger

/**
 * http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v1/
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=60547&start=0&sid=937cd736a46b29a2492fc8770b733bd6
 * https://steamcommunity.com/public/javascript/login.js
 * https://github.com/Jessecar96/SteamBot/blob/a7ef3f402f5b7e2527b6be82d9e8c770d43eb5a0/SteamTrade/SteamWeb.cs
 * https://github.com/echeese/TradeOfferLib/blob/master/TradeOfferLib/src/main/java/com/ryanspeets/tradeoffer/TradeUser.java
 *
 * @author TimePath
 */
public class SteamConnection : Connection() {

    override fun getBaseUrl() = "https://steamcommunity.com/"

    override fun getUserAgent() = "Java steam wrapper"

    override fun onConnect(con: HttpURLConnection) = Unit

    override fun mindelay() = 1000L

    class object {

        private val LOG = Logger.getLogger(javaClass<SteamConnection>().getName())
    }
}
