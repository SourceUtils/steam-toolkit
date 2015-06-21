package com.timepath.steam.net

public interface ServerListener {

    public fun inform(update: String)

    object NULL : ServerListener {
        override fun inform(update: String) = Unit
    }
}
