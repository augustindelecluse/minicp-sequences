package minicp.state;

import java.util.IdentityHashMap;
import java.util.Map;

public class CopyMap<K,V> implements Storage,StateMap<K,V> {
    private Copier sc;
    protected Map<K,V> map = new IdentityHashMap<>();
    protected CopyMap(Copier sc) { this.sc = sc;}

    public void put(K k, V v) {
        map.put(k,v);

    }
    public V get(K k) {
        return map.get(k);
    }

    @Override
    public Storage save() {
        return new CopyMap(null);
    }

    @Override public void restore(Storage s) { map = s.getMap();}
    @Override public int  getInt() { return 0;}
    @Override public boolean  getBool() { return false;}
    @Override public Map getMap() { return map;}
}
