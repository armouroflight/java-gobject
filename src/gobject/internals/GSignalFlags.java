package gobject.internals;

public enum GSignalFlags implements NativeEnum {
	RUN_FIRST,
	RUN_LAST,
	RUN_CLEANUP,
	NO_RECURSE,
	DETAILED,
	ACTION,
	NO_HOOKS;

	public int getNative() {
		return 1 << this.ordinal();
	};
}
