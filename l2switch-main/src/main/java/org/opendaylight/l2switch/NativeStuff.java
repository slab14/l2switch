package org.opendaylight.l2switch;

public class NativeStuff {

    static {
	System.loadLibrary("NativeFuncs");
    }

    public native void helloNative();

    public native int add(int in_a, int in_b);

    public native void revData(String javaString, int len);

    public native String rev(String javaString, int len);

}
