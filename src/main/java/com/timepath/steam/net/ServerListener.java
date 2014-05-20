package com.timepath.steam.net;

/**
 * @author TimePath
 */
public interface ServerListener {

    static ServerListener NULL = new ServerListener() {
        @Override
        public void inform(String update) {
        }
    };

    /**
     * @param update
     */
    void inform(String update);
}
