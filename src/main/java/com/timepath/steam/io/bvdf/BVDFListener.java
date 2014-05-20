package com.timepath.steam.io.bvdf;

/**
 * @author TimePath
 */
interface BVDFListener {

    /**
     * Invoked when a value is read
     *
     * @param key
     * @param value
     */
    void emit(String key, Object value);

    /**
     * Invoked when leaving the previous section
     */
    void pop();

    /**
     * Invoked when moving into a nested section
     *
     * @param section
     */
    void push(Object section);
}
