package cn.edu.thu.jcpn.core.runtime.tokens;

/**
 * Created by leven on 2016/12/17.
 */
public class LocalAsTarget implements IOwner {

    @Override
    public String getName() {
        return null;
    }

    private static LocalAsTarget instance = new LocalAsTarget();
    private LocalAsTarget() {
        super();
    }

    public static LocalAsTarget getInstance() {
        return instance;
    }
}
