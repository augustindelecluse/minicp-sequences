package minicp.state;

public class CopyBool implements Storage, StateBool {

    class CopyBoolStateEntry implements StateEntry {
        private final boolean v;

        public CopyBoolStateEntry(boolean v) {
            this.v = v;
        }

        public void restore() {
            CopyBool.this.v = v;
        }
    }

    private boolean v;

    public CopyBool(boolean initial) {
        v = initial;
    }

    @Override
    public void setValue(boolean v) {
        this.v = v;
    }

    @Override
    public boolean value() {
        return v;
    }

    @Override
    public StateEntry save() {
        return new CopyBoolStateEntry(v);
    }
}
