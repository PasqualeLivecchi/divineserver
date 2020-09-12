package luan;


public final class LuanRuntimeException extends RuntimeException {
	public LuanRuntimeException(LuanException e) {
		super(e);
	}
}
