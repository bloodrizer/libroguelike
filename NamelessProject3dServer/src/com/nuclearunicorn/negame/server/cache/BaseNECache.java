package com.nuclearunicorn.negame.server.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: bloodrizer
 * Date: 25.06.12
 * Time: 12:26
 * To change this template use File | Settings | File Templates.
 */
public class BaseNECache<Key, Value extends Serializable> implements INECache<Key, Value>{
    
    private Map<Key, Value> hashMapInternal;

    public BaseNECache(){
        hashMapInternal = new HashMap<Key, Value>();
    }
    
    @Override
    public Value get(Key key) {
        return hashMapInternal.get(key);
    }

    @Override
    public void put(Key key, Value value) {
        hashMapInternal.put(key, value);
    }
}
