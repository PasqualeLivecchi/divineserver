package goodjava.logger;


public final class ListLayout implements Layout {
	private final Layout[] layouts;

	public ListLayout(final Object... args) {
		layouts = new Layout[args.length];
		for( int i=0; i<args.length; i++ ) {
			Object obj = args[i];
			if( obj instanceof Layout ) {
				layouts[i] = (Layout)obj;
			} else if( obj instanceof String ) {
				layouts[i] = new StringLayout((String)obj);
			} else {
				throw new IllegalArgumentException("arg "+i);
			}
		}
	}

	public String format(LoggingEvent event) {
		StringBuilder sb = new StringBuilder();
		for( Layout layout : layouts ) {
			sb.append( layout.format(event) );
		}
		return sb.toString();
	}

	private static final class StringLayout implements Layout {
		final String s;

		StringLayout(String s) {
			this.s = s;
		}

		public String format(LoggingEvent event) {
			return s;
		}
	}
}
