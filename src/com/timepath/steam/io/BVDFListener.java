package com.timepath.steam.io;

/**
 * @author TimePath
 */
interface BVDFListener {

    void emit(String key, Object val);

    void pop();

    void push(Object index);
}
