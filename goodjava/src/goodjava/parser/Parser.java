package goodjava.parser;


public class Parser {
	public final String text;
	private final int len;
	private int[] stack = new int[256];
	private int frame = 0;
	private int iHigh;

	public Parser(String text) {
		this.text = text;
		this.len = text.length();
	}

	private int i() {
		return stack[frame];
	}

	private void i(int i) {
		stack[frame] += i;
		if( iHigh < stack[frame] )
			iHigh = stack[frame];
	}

	public int begin() {
		frame++;
		if( frame == stack.length ) {
			int[] a = new int[2*frame];
			System.arraycopy(stack,0,a,0,frame);
			stack = a;
		}
		stack[frame] = stack[frame-1];
		return i();
	}

	public void rollback() {
		stack[frame] = frame==0 ? 0 : stack[frame-1];
	}

	public <T> T success(T t) {
		success();
		return t;
	}

	public boolean success() {
		frame--;
		stack[frame] = stack[frame+1];
		return true;
	}

	public <T> T failure(T t) {
		failure();
		return t;
	}

	public boolean failure() {
		frame--;
		return false;
	}

	public int currentIndex() {
		return i();
	}
/*
	public int errorIndex() {
		return frame > 0 ? stack[frame-1] : 0;
	}
*/
	public int highIndex() {
		return iHigh;
	}

	public char lastChar() {
		return text.charAt(i()-1);
	}

	public char currentChar() {
		return text.charAt(i());
	}

	public boolean endOfInput() {
		return i() >= len;
	}

	public boolean match(char c) {
		if( endOfInput() || text.charAt(i()) != c )
			return false;
		i(1);
		return true;
	}

	public boolean match(String s) {
		int n = s.length();
		if( !text.regionMatches(i(),s,0,n) )
			return false;
		i(n);
		return true;
	}

	public boolean matchIgnoreCase(String s) {
		int n = s.length();
		if( !text.regionMatches(true,i(),s,0,n) )
			return false;
		i(n);
		return true;
	}

	public boolean anyOf(String s) {
		if( endOfInput() || s.indexOf(text.charAt(i())) == -1 )
			return false;
		i(1);
		return true;
	}

	public boolean noneOf(String s) {
		if( endOfInput() || s.indexOf(text.charAt(i())) != -1 )
			return false;
		i(1);
		return true;
	}

	public boolean inCharRange(char cLow, char cHigh) {
		if( endOfInput() )
			return false;
		char c = text.charAt(i());
		if( !(cLow <= c && c <= cHigh) )
			return false;
		i(1);
		return true;
	}

	public boolean anyChar() {
		if( endOfInput() )
			return false;
		i(1);
		return true;
	}

	public boolean test(char c) {
		return !endOfInput() && text.charAt(i()) == c;
	}

	public boolean test(String s) {
		return text.regionMatches(i(),s,0,s.length());
	}

	public boolean testIgnoreCase(String s) {
		return text.regionMatches(true,i(),s,0,s.length());
	}

	public String textFrom(int start) {
		return text.substring(start,i());
	}

}
