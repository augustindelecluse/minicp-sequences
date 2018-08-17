package minicp.state;

import java.util.IdentityHashMap;
import java.util.Map;

public class CopyMap<K,V> implements Storage,StateMap<K,V> {

    class CopyMapStateEntry implements StateEntry {
        private final Map<K,V> map;

        public CopyMapStateEntry(Map<K,V> map) {
            this.map = map;
        }

        public void restore() {
            CopyMap.this.map = map;
        }
    }

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
    @Override public StateEntry save()          {
        Map<K,V> mapCopy = new IdentityHashMap<>();
        for (Map.Entry<K,V> me : map.entrySet())
            mapCopy.put(me.getKey(),me.getValue());
        return new CopyMapStateEntry(mapCopy);

    }

}
