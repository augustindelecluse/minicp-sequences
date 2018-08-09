package minicp.state;

import java.util.IdentityHashMap;
import java.util.Map;

public class CopyMap<K,V> implements Storage,StateMap<K,V> {
    protected Map<K,V> map;
    protected CopyMap() {
        map = new IdentityHashMap<>();
    }
    protected CopyMap(Map<K,V> m) {
        map = new IdentityHashMap<>();
        for (Map.Entry<K,V> me : m.entrySet()) 
            m.put(me.getKey(),me.getValue());        
    }
    public void put(K k, V v)                { map.put(k,v);}
    public V get(K k)                        { return map.get(k);}
    @Override public Storage save()          { return new CopyMap(map);}
    @Override public void restore(Storage s) { map = ((CopyMap)s).map;}
}
