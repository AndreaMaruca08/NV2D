package nv.test;

import nv.core.NvContext;
import nv.core.components.NvComp;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class TestHelper {

    private static NvContext dummyContext;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            dummyContext = (NvContext) unsafe.allocateInstance(NvContext.class);
        } catch (Exception e) {
            dummyContext = null;
        }
    }

    public static void attachDummyContext(NvComp comp) {
        if (dummyContext == null) return;
        try {
            Field field = NvComp.class.getDeclaredField("context");
            field.setAccessible(true);
            field.set(comp, dummyContext);
        } catch (Exception ignored) {
        }
    }
}
