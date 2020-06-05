package org.opendaylight.l2switch;

public class NativeStuff {

    static {
	System.loadLibrary("NativeFuncs");
    }

    public native String rev(String javaString, int len);

    public native String decrypt(byte[] javaBytes, int len);

    public native void initState(int[] maxArray, int numStates);

    public native int getState(int IDnum);

    public native void transitionState(int IDnum);

}
