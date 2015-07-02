package com.timepath.steam.io.bvdf

interface BVDFListener {

    /**
     * Invoked when a value is read
     */
    public fun emit(key: String, value: Any)

    /**
     * Invoked when leaving the previous section
     */
    public fun pop()

    /**
     * Invoked when moving into a nested section
     */
    public fun push(section: Any)
}
