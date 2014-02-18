package gobject.internals;

public interface GParamFlags {
	public static final int READABLE = 1 << 0;
	public static final int WRITABLE = 1 << 1;
	public static final int CONSTRUCT = 1 << 2;
	public static final int CONSTRUCT_ONLY = 1 << 3;
	public static final int LAX_VALIDATION = 1 << 4;
	public static final int STATIC_NAME = 1 << 5;
	public static final int STATIC_NICK = 1 << 6;
	public static final int STATIC_BLURB = 1 << 7;
	
}
