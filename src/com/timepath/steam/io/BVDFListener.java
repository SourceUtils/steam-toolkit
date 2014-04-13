package com.timepath.steam.io;

/**
 *
 * @author TimePath
 */
public interface BVDFListener {

    void emit(String key, Object val);

    void pop();

    void push(Object index);
    
}
