package goodjava.parser;


public final class ParseException extends Exception {
	public final String text;
	public final int errorIndex;
	public final int highIndex;

	public ParseException(Parser parser,String msg) {
		super(msg);
		this.text = parser.text;
		this.errorIndex = parser.currentIndex();
		this.highIndex = parser.highIndex();
	}

	public ParseException(Parser parser,Exception cause) {
		this(parser,cause.getMessage(),cause);
	}

	public ParseException(Parser parser,String msg,Exception cause) {
		super(msg,cause);
		this.text = parser.text;
		this.errorIndex = parser.currentIndex();
		this.highIndex = parser.highIndex();
	}

	private class Location {
		final int line;
		final int pos;

		Location(int index) {
			int line = 0;
			int i = -1;
			while(true) {
				int j = text.indexOf('\n',i+1);
				if( j == -1 || j >= index )
					break;
				i = j;
				line++;
			}
			this.line = line;
			this.pos = index - i - 1;
		}
	}

	private String[] lines() {
		return text.split("\n",-1);
	}

	@Override public String getMessage() {
		String line;
		int pos;
		StringBuilder sb = new StringBuilder(super.getMessage());
		if( text.indexOf('\n') == -1 ) {
			line = text;
			pos = errorIndex;
			sb.append( " (position " + (pos+1) + ")\n" );
		} else {
			Location loc = new Location(errorIndex);
			line = lines()[loc.line];
			pos = loc.pos;
			sb.append( " (line " + (loc.line+1) + ", pos " + (pos+1) + ")\n" );
		}
		sb.append( line + "\n" );
		for( int i=0; i<pos; i++ ) {
			sb.append( line.charAt(i)=='\t' ? '\t' : ' ' );
		}
		sb.append("^\n");
		return sb.toString();
	}
}
