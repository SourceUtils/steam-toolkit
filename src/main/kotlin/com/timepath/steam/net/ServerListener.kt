package com.timepath.steam.net

/**
 * @author TimePath
 */
public interface ServerListener {

    /**
     * @param update
     */
    public fun inform(update: String)

    object NULL : ServerListener {
        override fun inform(update: String) = Unit
    }
}
