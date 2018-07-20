package minicp.state;

public class CopyBool implements Storage,StateBool {
    private boolean _value;
    public CopyBool(Copier cs,boolean initial) {
        _value = initial;
    }
    @Override
    public void setValue(boolean v) {
        _value = v;
    }

    @Override
    public boolean getValue() {
        return _value;
    }

    @Override
    public Storage saveTo() {
        return new CopyBool(null,_value);
    }

    @Override
    public void restoreFrom(Storage s) {
        _value = ((StateBool)s).getValue();
    }
}
