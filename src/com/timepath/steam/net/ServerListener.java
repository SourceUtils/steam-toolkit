package com.timepath.steam.net;

/**
 *
 * @author TimePath
 */
public interface ServerListener {

    public static ServerListener DUMMY = new ServerListener() {
        public void inform(String update) {
        }
    };

    /**
     *
     * @param update
     */
    void inform(String update);

}
