package minicp.state;

public interface Storage {
    Storage saveTo();
    void  restoreFrom(Storage s);
}
