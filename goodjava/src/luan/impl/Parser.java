package luan.impl;


final class Parser {

	private static class Frame {
		int i;
		StringBuilder sb;
	}

//	private final LuanSource src;
	public final String text;
	public final String sourceName;
	private final int len;
	private Frame[] stack = new Frame[256];
	private int frame = 0;
	private int iHigh;

	public Parser(String text,String sourceName) {
//		this.src = src;
		this.text = text;
		this.sourceName = sourceName;
		this.len = text.length();
		stack[0] = new Frame();
	}

	private int i() {
		return stack[frame].i;
	}

	private void i(int i) {
		Frame f = stack[frame];
		f.i += i;
		if( iHigh < f.i )
			iHigh = f.i;
	}

	public int begin() {
		frame++;
		if( frame == stack.length ) {
			Frame[] a = new Frame[2*frame];
			System.arraycopy(stack,0,a,0,frame);
			stack = a;
		}
		Frame f = new Frame();
		f.i = stack[frame-1].i;
		stack[frame] = f;
		return i();
	}

	public void rollback() {
		Frame f = stack[frame];
		f.i = stack[frame-1].i;
		f.sb = null;
	}

	public <T> T success(T t) {
		success();
		return t;
	}

	public boolean success() {
		Frame f = stack[frame];
		if( f.sb != null && f.sb.length() > 0 )  throw new RuntimeException("sb not emtpy");
		frame--;
		stack[frame].i = f.i;
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

	public ParseException exception(String msg) {
		return new ParseException(msg,text,sourceName,i(),iHigh);
	}

	public ParseException exception() {
		return exception("Invalid input");
	}

	public StringBuilder sb() {
		Frame f = stack[frame];
		if( f.sb == null )
			f.sb = new StringBuilder();
		return f.sb;
	}

	public void upSb() {
		Frame f = stack[frame];
		StringBuilder sb = f.sb;
		if( sb != null && sb.length() > 0 ) {
			Frame fUp = stack[frame-1];
			if( fUp.sb == null )
				fUp.sb = sb;
			else
				fUp.sb.append(sb.toString());
			f.sb = null;
		}
	}

	public int currentIndex() {
		return i();
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

	public String textFrom(int start) {
		return text.substring(start,i());
	}

}
