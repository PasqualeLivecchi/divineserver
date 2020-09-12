package goodjava.logger;


public final class Level {
	public final static int DEBUG = 0;
	public final static int INFO = 1;
	public final static int WARN = 2;
	public final static int ERROR = 3;
	public final static int OFF = 4;

	private static final String[] names = {"DEBUG","INFO","WARN","ERROR"};

	public static String toString(int level) {
		return names[level];
	}

	private static final String[] paddedNames = {"DEBUG","INFO ","WARN ","ERROR"};

	public static String toPaddedString(int level) {
		return paddedNames[level];
	}

}
