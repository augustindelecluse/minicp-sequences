package minicp.state;

import java.util.Map;

public interface Storage {
    Storage save();
    void restore(Storage s);
    int  getInt();
    boolean  getBool();
    Map      getMap();
}
