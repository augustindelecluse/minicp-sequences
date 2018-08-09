package minicp.state;
import java.util.Map;

public class CopyBool implements Storage,StateBool {
    private boolean _value;
    public CopyBool(boolean initial)          { _value = initial;}
    @Override public void setValue(boolean v) { _value = v;}
    @Override public boolean getValue()       { return _value;}
    @Override public Storage save()           { return new CopyBool(_value);}
    @Override public void restore(Storage s)  { _value = ((CopyBool)s).getValue();}
}
