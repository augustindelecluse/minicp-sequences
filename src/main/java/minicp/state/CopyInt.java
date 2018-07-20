package minicp.state;

import java.util.Map;

public class CopyInt implements Storage,StateInt {
    private int _value;
    public CopyInt(Copier sc,int initial) {
        _value = initial;
    }
    @Override public int setValue(int v) {
        return _value = v;
    }
    @Override public int getValue() {
        return _value;
    }
    @Override public int increment() { _value += 1;return _value;}
    @Override public int decrement() { _value -= 1;return _value;}
    @Override public String toString() { return String.valueOf(_value);}
    @Override public Storage save() {
        return new CopyInt(null,_value);
    }
    @Override public void restore(Storage s) { _value = s.getInt();}
    @Override public int  getInt() { return _value;}
    @Override public boolean  getBool() { return false;}
    @Override public Map getMap() { return null;}
}
