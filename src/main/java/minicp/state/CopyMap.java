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
    public Storage saveTo() {
        return new CopyMap(null);
    }

    @Override
    public void restoreFrom(Storage s) {
        CopyMap<K,V> ts = (CopyMap<K,V>)s;
        map = ts.map;
    }
}
