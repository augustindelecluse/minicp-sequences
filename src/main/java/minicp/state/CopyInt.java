package minicp.state;

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
    @Override public Storage saveTo() {
        return new CopyInt(null,_value);
    }
    @Override public void  restoreFrom(Storage s) {
        _value = ((StateInt)s).getValue();
    }

}
