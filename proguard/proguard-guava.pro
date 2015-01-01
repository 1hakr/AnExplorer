# Configuration for Guava
# Note, requires inclusion of jsr305.jar & javax.inject.jar
# see http://www.marvinlabs.com/2013/01/22/android-proguard-and-guavas-event-bus-component/
# or
# see https://code.google.com/p/guava-libraries/wiki/UsingProGuardWithGuava

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclassmembers,allowoptimization class com.google.common.* {
    void finalizeReferent();
    void startFinalizer(java.lang.Class,java.lang.Object);
}
