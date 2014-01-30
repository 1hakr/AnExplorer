package dev.dworks.apps.anexplorer.libcore.io;

public class LongCompat {
	public static int compare(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}
}
