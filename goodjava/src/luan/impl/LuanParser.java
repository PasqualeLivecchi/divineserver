package luan.impl;

//import java.io.StringWriter;
//import java.io.PrintWriter;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import luan.Luan;
import luan.LuanTable;
import luan.modules.PackageLuan;


final class LuanParser {

	private interface Sym {
		public Expr exp();
	}

	private int symCounter = 0;

	private class LocalSym implements Sym {
		final String name;
		final String javaName;
		boolean isPointer = false;

		LocalSym(String name) {
			this.name = name;
			this.javaName = name + "_" + (++symCounter);
		}

		Stmts declaration(Expr value) {
			Stmts stmt = new Stmts();
			if( value==null ) {
				stmt.add( new Object() {
					@Override public String toString() {
						if( !isPointer )
							return "Object " + javaName + ";  ";
						else
							return "final Pointer " + javaName + " = new Pointer();  ";
					}
				} );
			} else {
				if( value.valType != Val.SINGLE )  throw new RuntimeException();
				stmt.add( new Object() {
					@Override public String toString() {
						if( !isPointer )
							return "Object " + javaName + " = ";
						else
							return "final Pointer " + javaName + " = new Pointer(";
					}
				} );
				stmt.addAll(value);
				stmt.add( new Object() {
					@Override public String toString() {
						if( !isPointer )
							return ";  ";
						else
							return ");  ";
					}
				} );
			}
			return stmt;
		}

		@Override public Expr exp() {
			Expr exp = new Expr(Val.SINGLE,false);
			exp.add( new Object() {
				@Override public String toString() {
					if( !isPointer )
						return javaName;
					else
						return javaName + ".o";
				}
			} );
			return exp;
		}
	}

	private class UpSym implements Sym {
		final String name;
		final int i;
		final String value;

		UpSym(String name,int i,String value) {
			this.name = name;
			this.i = i;
			this.value = value;
		}
/*
		String init() {
			return "upValues[" + i + "] = " + value + ";  ";
		}
*/
		@Override public Expr exp() {
			Expr exp = new Expr(Val.SINGLE,false);
			exp.add( new Object() {
				@Override public String toString() {
					return "upValues[" + i + "].o";
				}
			} );
			return exp;
		}
	}

	private final class Frame {
		final Frame parent;
		final List<LocalSym> symbols = new ArrayList<LocalSym>();
		int loops = 0;
		boolean isVarArg = false;
		final List<UpSym> upValueSymbols = new ArrayList<UpSym>();

		Frame() {
			this.parent = null;
		}

		Frame(Frame parent) {
			this.parent = parent;
		}

		LocalSym addLocalSym(String name) {
			LocalSym sym = new LocalSym(name);
			symbols.add(sym);
			return sym;
		}

		UpSym addUpSym(String name,String value) {
			UpSym sym = new UpSym( name, upValueSymbols.size(), value );
			upValueSymbols.add(sym);
			return sym;
		}

		LocalSym getLocalSym(String name) {
			int i = symbols.size();
			while( --i >= 0 ) {
				LocalSym sym = symbols.get(i);
				if( sym.name.equals(name) )
					return sym;
			}
			return null;
		}

		UpSym getUpSym(String name) {
			for( UpSym upSym : upValueSymbols ) {
				if( upSym.name.equals(name) )
					return upSym;
			}
			if( parent != null ) {
				LocalSym sym = parent.getLocalSym(name);
				if( sym != null ) {
					sym.isPointer = true;
					return addUpSym(name,sym.javaName);
				}
				UpSym upSym = parent.getUpSym(name);
				if( upSym != null ) {
					return addUpSym(name,"parentUpValues["+upSym.i+"]");
				}
			}
			return null;
		}

		Sym getSym(String name) {
			Sym sym = getLocalSym(name);
			return sym != null ? sym : getUpSym(name);
		}

	}

	private int innerCounter = 0;
	private final List<Inner> inners = new ArrayList<Inner>();

	private Frame frame;
	private final Parser parser;
	private final Stmts top;

	LuanParser(String sourceText,String sourceName) {
		this.frame = new Frame();
		this.parser = new Parser(sourceText,sourceName);
		this.top = new Stmts();
	}

	void addVar(String name) {
		UpSym upSym = frame.addUpSym( "-ADDED-" ,"new Pointer()");
		LocalSym sym = frame.addLocalSym( name );
		sym.isPointer = true;
		top.add( "final Pointer " + sym.javaName + " = upValues[" + upSym.i + "];  " );
	}

	private int symbolsSize() {
		return frame.symbols.size();
	}

	private Stmts addSymbol(String name,Expr value) {
		final LocalSym sym = frame.addLocalSym(name);
		return sym.declaration(value);
	}

	private Sym getSym(String name) {
		return frame.getSym(name);
	}

	private void popSymbols(int n) {
		List<LocalSym> symbols = frame.symbols;
		while( n-- > 0 ) {
			symbols.remove(symbols.size()-1);
		}
	}

	private void incLoops() {
		frame.loops++;
	}

	private void decLoops() {
		frame.loops--;
	}

	private <T> T required(T t) throws ParseException {
		if( t==null )
			throw parser.exception();
		return t;
	}

	private <T> T required(T t,String msg) throws ParseException {
		if( t==null )
			throw parser.exception(msg);
		return t;
	}

	private Expr newFnExp(Stmts stmt,String name) {
		String className = "INNER" + ++innerCounter;
		Inner inner = new Inner( stmt, name, className );
		inners.add(inner);
		return inner.toInnerFnExp( frame.upValueSymbols );
//		return toFnExp( stmt, frame.upValueSymbols, name );
	}

	Compiled RequiredModule() throws ParseException {
		GetRequiredModule();
		String className = "EXP";
		String classCode = toFnString( top, frame.upValueSymbols, className, inners );
		return Compiled.compile("luan.impl."+className,parser.sourceName,classCode);
	}

	String RequiredModuleSource() throws ParseException {
		GetRequiredModule();
		String className = "EXP";
		return toFnString( top, frame.upValueSymbols, className, inners );
	}

	void GetRequiredModule() throws ParseException {
		//Spaces();
		parser.begin();
		frame.isVarArg = true;
		top.add( "final Object[] varArgs = LuanImpl.varArgs(args,0);  " );
		Stmts block = RequiredBlock();
		top.addAll( block );
		top.hasReturn = block.hasReturn;
		if( !parser.endOfInput() )
			throw parser.exception();
		parser.success();
	}

	private Stmts RequiredBlock() throws ParseException {
		Stmts stmts = new Stmts();
		int stackStart = symbolsSize();
		do {
			Spaces();
			stmts.addNewLines();
			Stmts stmt = Stmt();
			if( stmt != null ) {
				stmts.addAll(stmt);
				stmts.hasReturn = stmt.hasReturn;
			}
		} while( !stmts.hasReturn && (StmtSep() || TemplateSep(stmts)) );
		Spaces();
		while( StmtSep() )
			Spaces();
		stmts.addNewLines();
		int stackEnd = symbolsSize();
		popSymbols( stackEnd - stackStart );
		return stmts;
	}

	private boolean StmtSep() throws ParseException {
		return parser.match( ';' ) || EndOfLine();
	}

	private boolean TemplateSep(Stmts stmts) throws ParseException {
		Stmts stmt = TemplateStmt();
		if( stmt != null ) {
			stmts.addAll(stmt);
			return true;
		}
		return false;
	}

	private boolean EndOfLine() {
		if( MatchEndOfLine() ) {
			parser.sb().append('\n');
			return true;
		} else {
			return false;
		}
	}

	private boolean MatchEndOfLine() {
		return parser.match( "\r\n" ) || parser.match( '\r' ) || parser.match( '\n' );
	}

	private Stmts Stmt() throws ParseException {
		Stmts stmt;
		if( (stmt=ReturnStmt()) != null
			|| (stmt=FunctionStmt()) != null
			|| (stmt=LocalStmt()) != null
			|| (stmt=LocalFunctionStmt()) != null
			|| (stmt=BreakStmt()) != null
			|| (stmt=ContinueStmt()) != null
			|| (stmt=ForStmt()) != null
			|| (stmt=DoStmt()) != null
			|| (stmt=WhileStmt()) != null
			|| (stmt=RepeatStmt()) != null
			|| (stmt=IfStmt()) != null
			|| (stmt=TryStmt()) != null
			|| (stmt=SetStmt()) != null
			|| (stmt=ExpressionsStmt()) != null
		) {
			return stmt;
		}
		return null;
	}

	private Expr indexExpStr(Expr exp1,Expr exp2) {
		Expr exp = new Expr(Val.SINGLE,false);
		exp.add( "luan.index(" );
		exp.addAll( exp1.single() );
		exp.add( "," );
		exp.addAll( exp2.single() );
		exp.add( ")" );
		return exp;
	}

	private Expr callExpStr(Expr fn,Expr args) {
		Expr exp = new Expr(null,true);
		exp.add( "Luan.checkFunction(" );
		exp.addAll( fn.single() );
		exp.add( ").call(" );
		exp.addAll( args.array() );
		exp.add( ")" );
		return exp;
	}

	private Stmts TemplateStmt() throws ParseException {
		Expr exprs = TemplateExpressions();
		if( exprs == null )
			return null;
		Stmts stmt = new Stmts();
		stmt.add( "Luan.checkFunction(luan.index(PackageLuan.require(luan,\"luan:Io.luan\"),\"template_write\")).call(" );
		stmt.addAll( exprs.array() );
		stmt.add( ");  " );
		return stmt;
	}

	private Expr TemplateExpressions() throws ParseException {
		int start = parser.begin();
		if( !parser.match( "%>" ) )
			return parser.failure(null);
		EndOfLine();
		List<Expr> builder = new ArrayList<Expr>();
		while(true) {
			if( parser.match( "<%=" ) ) {
				Spaces();
				Expr exp = new Expr(Val.SINGLE,false);
				exp.addAll( RequiredExpr().single() );
				builder.add(exp);
				RequiredMatch( "%>" );
			} else if( parser.match( "<%" ) ) {
				Spaces();
				return parser.success(expString(builder));
			} else {
				Expr exp = new Expr(Val.SINGLE,false);
				int i = parser.currentIndex();
				do {
					if( parser.match( "%>" ) )
						throw parser.exception("'%>' unexpected");
					if( !(EndOfLine() || parser.anyChar()) )
						throw parser.exception("Unclosed template expression");
				} while( !parser.test( "<%" ) );
				String match = parser.textFrom(i);
				String rtns = parser.sb().toString();
				parser.sb().setLength(0);
				exp.addAll( constExpStr(match) );
				if( rtns.length() > 0 )
					exp.add(rtns);
				builder.add(exp);
			}
		}
	}

	private Stmts ReturnStmt() throws ParseException {
		parser.begin();
		if( !Keyword("return") )
			return parser.failure(null);
		Expr exprs = ExpStringList();
		Stmts stmt = new Stmts();
		stmt.add( "return " );
		if( exprs != null )
			stmt.addAll( exprs );
		else
			stmt.add( "LuanFunction.NOTHING" );
		stmt.add( ";  " );
		stmt.hasReturn = true;
		return parser.success( stmt );
	}

	private Stmts FunctionStmt() throws ParseException {
		parser.begin();
		if( !Keyword("function") )
			return parser.failure(null);

		parser.currentIndex();
		String name = RequiredName();
		Var var = nameVar(name);
		while( parser.match( '.' ) ) {
			Spaces();
//			Expr exp = NameExpr();
			name = Name();
			if( name==null )
				return parser.failure(null);
			var = indexVar( var.exp(), constExpStr(name) );
		}

		Expr fnDef = RequiredFunction(name);
		return parser.success( var.set(fnDef) );
	}

	private Stmts LocalFunctionStmt() throws ParseException {
		parser.begin();
		if( !(Keyword("local") && Keyword("function")) )
			return parser.failure(null);
		Stmts stmt = new Stmts();
		String name = RequiredName();
		stmt.addAll( addSymbol(name,null) );
		Expr fnDef = RequiredFunction(name);
		stmt.addAll( nameVar(name).set(fnDef) );
		return parser.success( stmt );
	}

	private Stmts BreakStmt() throws ParseException {
		parser.begin();
		if( !Keyword("break") )
			return parser.failure(null);
		if( frame.loops <= 0 )
			throw parser.exception("'break' outside of loop");
		Stmts stmt = new Stmts();
		stmt.add( "break;  " );
		return parser.success( stmt );
	}

	private Stmts ContinueStmt() throws ParseException {
		parser.begin();
		if( !Keyword("continue") )
			return parser.failure(null);
		if( frame.loops <= 0 )
			throw parser.exception("'continue' outside of loop");
		Stmts stmt = new Stmts();
		stmt.add( "continue;  " );
		return parser.success( stmt );
	}

	int forCounter = 0;

	private Stmts ForStmt() throws ParseException {
		parser.begin();
		int stackStart = symbolsSize();
		if( !Keyword("for") )
			return parser.failure(null);
		List<String> names = RequiredNameList();
		if( !Keyword("in") )
			return parser.failure(null);
		Expr expr = RequiredExpr().single();
		RequiredKeyword("do");

		String fnVar = "fn" + ++forCounter;
		Expr fnExp = new Expr(null,false);
		fnExp.add( fnVar + ".call()" );
		Stmts stmt = new Stmts();
		stmt.add( ""
			+"LuanFunction "+fnVar+" = Luan.checkFunction("
		);
		stmt.addAll( expr );
		stmt.add( ");  " );
		stmt.add( "while(true) {  " );
		stmt.addAll( makeLocalSetStmt(names,fnExp) );
		stmt.add( "if( " );
		stmt.addAll( nameVar(names.get(0)).exp() );
 		stmt.add( "==null )  break;  " );
		Stmts loop = RequiredLoopBlock();
		RequiredEnd("end_for");
		stmt.addAll( loop );
		stmt.add( "}  " );
		popSymbols( symbolsSize() - stackStart );
		return parser.success(stmt);
	}

	private Stmts DoStmt() throws ParseException {
		parser.begin();
		if( !Keyword("do") )
			return parser.failure(null);
		Stmts stmt = RequiredBlock();
		RequiredEnd("end_do");
		return parser.success(stmt);
	}

	private Stmts LocalStmt() throws ParseException {
		parser.begin();
		if( !Keyword("local") )
			return parser.failure(null);
		List<String> names = NameList();
		if( names==null ) {
			if( Keyword("function") )
				return parser.failure(null);  // handled later
			throw parser.exception("Invalid local statement");
		}
		Stmts stmt = new Stmts();
		if( parser.match( '=' ) ) {
			Spaces();
			Expr values = ExpStringList();
			if( values==null )
				throw parser.exception("Expressions expected");
			stmt.addAll( makeLocalSetStmt(names,values) );
		} else {
			Expr value = new Expr(Val.SINGLE,false);
			value.add( "null" );
			for( String name : names ) {
				stmt.addAll( addSymbol(name,value) );
			}
		}
		return parser.success(stmt);
	}

	private List<String> RequiredNameList() throws ParseException {
		parser.begin();
		List<String> names = NameList();
		if( names==null )
			throw parser.exception("Name expected");
		return parser.success(names);
	}

	private List<String> NameList() throws ParseException {
		String name = Name();
		if( name==null )
			return null;
		List<String> names = new ArrayList<String>();
		names.add(name);
		while( (name=anotherName()) != null ) {
			names.add(name);
		}
		return names;
	}

	private String anotherName() throws ParseException {
		parser.begin();
		if( !parser.match( ',' ) )
			return parser.failure(null);
		Spaces();
		String name = Name();
		if( name==null )
			return parser.failure(null);
		return parser.success(name);
	}

	private Stmts WhileStmt() throws ParseException {
		parser.begin();
		if( !Keyword("while") )
			return parser.failure(null);
		Expr cnd = RequiredExpr().single();
		RequiredKeyword("do");
		Stmts loop = RequiredLoopBlock();
		RequiredEnd("end_while");
		Stmts stmt = new Stmts();
		stmt.add( "while( Luan.checkBoolean(" );
		stmt.addAll( cnd );
		stmt.add( ") ) {  " );
		stmt.addAll( loop );
		stmt.add( "}  " );
		return parser.success( stmt );
	}

	private Stmts RepeatStmt() throws ParseException {
		parser.begin();
		if( !Keyword("repeat") )
			return parser.failure(null);
		Stmts loop = RequiredLoopBlock();
		RequiredKeyword("until");
		Expr cnd = RequiredExpr().single();
		Stmts stmt = new Stmts();
		stmt.add( "do {  " );
		stmt.addAll( loop );
		stmt.add( "} while( !Luan.checkBoolean(" );
		stmt.addAll( cnd );
		stmt.add( ") );  " );
		return parser.success( stmt );
	}

	private Stmts RequiredLoopBlock() throws ParseException {
		incLoops();
		Stmts stmt = RequiredBlock();
		decLoops();
		return stmt;
	}

	private Stmts IfStmt() throws ParseException {
		parser.begin();
		if( !Keyword("if") )
			return parser.failure(null);
		Stmts stmt = new Stmts();
		Expr cnd;
		Stmts block;
		boolean hasReturn = true;
		cnd = RequiredExpr().single();
		RequiredKeyword("then");
		block = RequiredBlock();
		stmt.add( "if( Luan.checkBoolean(" );
		stmt.addAll( cnd );
		stmt.add( ") ) {  " );
		stmt.addAll( block );
		if( !block.hasReturn )
			hasReturn = false;
		while( Keyword("elseif") ) {
			cnd = RequiredExpr().single();
			RequiredKeyword("then");
			block = RequiredBlock();
			stmt.add( "} else if( Luan.checkBoolean(" );
			stmt.addAll( cnd );
			stmt.add( ") ) {  " );
			stmt.addAll( block );
			if( !block.hasReturn )
				hasReturn = false;
		}
		if( Keyword("else") ) {
			block = RequiredBlock();
			stmt.add( "} else {  " );
			stmt.addAll( block );
			if( !block.hasReturn )
				hasReturn = false;
		} else {
			hasReturn = false;
		}
		RequiredEnd("end_if");
		stmt.add( "}  " );
		stmt.hasReturn = hasReturn;
		return parser.success( stmt );
	}

	int catchCounter = 0;

	private Stmts TryStmt() throws ParseException {
		parser.begin();
		if( !Keyword("try") )
			return parser.failure(null);
		Stmts tryBlock = RequiredBlock();
		Stmts catchBlock = null;
		Stmts finallyBlock = null;
		Stmts stmt = new Stmts();
		stmt.add( "try {  LuanImpl.nopTry();  " );
		stmt.addAll( tryBlock );
		if( Keyword("catch") ) {
			String name = Name();
			Expr exp = new Expr(Val.SINGLE,false);
			String var = "e" + ++catchCounter;
			exp.add( var+".table(luan)" );
			stmt.add( "} catch(LuanException "+var+") {  " );
			stmt.addAll( addSymbol(name,exp) );
			catchBlock = RequiredBlock();
			stmt.addAll( catchBlock );
			popSymbols(1);
		}
		if( Keyword("finally") ) {
			finallyBlock = RequiredBlock();
			stmt.add( "} finally {  " );
			stmt.addAll( finallyBlock );
		}
		RequiredEnd("end_try");
		if( catchBlock==null && finallyBlock==null )
			stmt.add( "} finally {  " );
		stmt.add( "}  " );
		stmt.hasReturn = finallyBlock!=null && finallyBlock.hasReturn || tryBlock.hasReturn && (catchBlock==null || catchBlock.hasReturn);
		return parser.success( stmt );
	}

	private Stmts SetStmt() throws ParseException {
		parser.begin();
		List<Var> vars = new ArrayList<Var>();
		Var v = SettableVar();
		if( v == null )
			return parser.failure(null);
		vars.add(v);
		while( parser.match( ',' ) ) {
			Spaces();
			v = SettableVar();
			if( v == null )
				return parser.failure(null);
			vars.add(v);
		}
		if( !parser.match( '=' ) )
			return parser.failure(null);
		Spaces();
		Expr values = ExpStringList();
		if( values==null )
//			throw parser.exception("Expressions expected");
			return parser.failure(null);
		return parser.success( makeSetStmt(vars,values) );
	}

	private Stmts makeSetStmt(List<Var> vars,Expr values) throws ParseException {
		int n = vars.size();
		if( n == 1 )
			return vars.get(0).set(values);
		Stmts stmt = new Stmts();
		String varName = values.valType==Val.ARRAY ? "a" : "t";
		stmt.add( varName + " = " );
		stmt.addAll( values );
		stmt.add( ";  " );
		Expr t = new Expr(values.valType,false);
		t.add( varName );
		t = t.single();
		stmt.addAll( vars.get(0).set(t) );
		for( int i=1; i<n; i++ ) {
			t.clear();
			t.add( "LuanImpl.pick(" + varName + ","+i+")" );
			stmt.addAll( vars.get(i).set(t) );
		}
		return stmt;
	}

	private Stmts makeLocalSetStmt(List<String> names,Expr values) throws ParseException {
		int n = names.size();
		if( n == 1 )
			return addSymbol(names.get(0),values.single());
		Stmts stmt = new Stmts();
		String varName = values.valType==Val.ARRAY ? "a" : "t";
		stmt.add( varName + " = " );
		stmt.addAll( values );
		stmt.add( ";  " );
		Expr t = new Expr(values.valType,false);
		t.add( varName );
		t = t.single();
		stmt.addAll( addSymbol(names.get(0),t) );
		for( int i=1; i<n; i++ ) {
			t.clear();
			t.add( "LuanImpl.pick(" + varName + ","+i+")" );
			stmt.addAll( addSymbol(names.get(i),t) );
		}
		return stmt;
	}

	private Stmts ExpressionsStmt() throws ParseException {
		parser.begin();
		Expr exp = Expression();
		if( exp != null && exp.isStmt ) {
			Stmts stmt = new Stmts();
			if( exp.valType==Val.SINGLE ) {
				stmt.add( "LuanImpl.nop(" );
				stmt.addAll( exp );
				stmt.add( ")" );
			} else {
				stmt.addAll( exp );
			}
			stmt.add( ";  " );
			return parser.success( stmt );
		}
		return parser.failure(null);
	}

	private Var SettableVar() throws ParseException {
		int start = parser.begin();
		Var var = VarZ();
		if( var==null || !var.isSettable() )
			return parser.failure(null);
		return parser.success( var );
	}

	private Expr RequiredExpr() throws ParseException {
		parser.begin();
		return parser.success(required(Expression(),"Bad expression"));
	}

	private Expr Expression() throws ParseException {
		return OrExpr();
	}

	private Expr OrExpr() throws ParseException {
		parser.begin();
		Expr exp = AndExpr();
		if( exp==null )
			return parser.failure(null);
		while( Keyword("or") ) {
			exp = exp.single();
			Expr exp2 = required(AndExpr()).single();
			Expr newExp = new Expr(Val.SINGLE,true);
			newExp.add( "(LuanImpl.cnd(t = " );
			newExp.addAll( exp );
			newExp.add( ") ? t : (" );
			newExp.addAll( exp2 );
			newExp.add( "))" );
			exp = newExp;
		}
		return parser.success(exp);
	}

	private Expr AndExpr() throws ParseException {
		parser.begin();
		Expr exp = RelExpr();
		if( exp==null )
			return parser.failure(null);
		while( Keyword("and") ) {
			exp = exp.single();
			Expr exp2 = required(RelExpr()).single();
			Expr newExp = new Expr(Val.SINGLE,true);
			newExp.add( "(LuanImpl.cnd(t = " );
			newExp.addAll( exp );
			newExp.add( ") ? (" );
			newExp.addAll( exp2 );
			newExp.add( ") : t)" );
			exp = newExp;
		}
		return parser.success(exp);
	}

	private Expr RelExpr() throws ParseException {
		parser.begin();
		Expr exp = ConcatExpr();
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match("==") ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(ConcatExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.eq(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( parser.match("~=") ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(ConcatExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "!LuanImpl.eq(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( parser.match("<=") ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(ConcatExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.le(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( parser.match(">=") ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(ConcatExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.le(" );
				newExp.addAll( exp2 );
				newExp.add( "," );
				newExp.addAll( exp );
				newExp.add( ")" );
				exp = newExp;
			} else if( parser.match("<") ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(ConcatExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.lt(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( parser.match(">") ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(ConcatExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.lt(" );
				newExp.addAll( exp2 );
				newExp.add( "," );
				newExp.addAll( exp );
				newExp.add( ")" );
				exp = newExp;
			} else
				break;
		}
		return parser.success(exp);
	}

	private Expr ConcatExpr() throws ParseException {
		parser.begin();
		Expr exp = SumExpr();
		if( exp==null )
			return parser.failure(null);
		if( parser.match("..") ) {
			Spaces();
			exp = exp.single();
			Expr exp2 = required(ConcatExpr()).single();
			Expr newExp = new Expr(Val.SINGLE,false);
			newExp.add( "LuanImpl.concat(" );
			newExp.addAll( exp );
			newExp.add( "," );
			newExp.addAll( exp2 );
			newExp.add( ")" );
			exp = newExp;
		}
		return parser.success(exp);
	}

	private Expr SumExpr() throws ParseException {
		parser.begin();
		Expr exp = TermExpr();
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match('+') ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(TermExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.add(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( Minus() ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(TermExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.sub(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else
				break;
		}
		return parser.success(exp);
	}

	private boolean Minus() {
		parser.begin();
		return parser.match('-') && !parser.match('-') ? parser.success() : parser.failure();
	}

	private Expr TermExpr() throws ParseException {
		parser.begin();
		Expr exp = UnaryExpr();
		if( exp==null )
			return parser.failure(null);
		while(true) {
			if( parser.match('*') ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(UnaryExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.mul(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( parser.match('/') ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(UnaryExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.div(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else if( Mod() ) {
				Spaces();
				exp = exp.single();
				Expr exp2 = required(UnaryExpr()).single();
				Expr newExp = new Expr(Val.SINGLE,false);
				newExp.add( "LuanImpl.mod(" );
				newExp.addAll( exp );
				newExp.add( "," );
				newExp.addAll( exp2 );
				newExp.add( ")" );
				exp = newExp;
			} else
				break;
		}
		return parser.success(exp);
	}

	private boolean Mod() {
		parser.begin();
		return parser.match('%') && !parser.match('>') ? parser.success() : parser.failure();
	}

	private Expr UnaryExpr() throws ParseException {
		parser.begin();
		if( parser.match('#') ) {
			Spaces();
			Expr exp = required(UnaryExpr()).single();
			Expr newExp = new Expr(Val.SINGLE,false);
			newExp.add( "LuanImpl.len(" );
			newExp.addAll( exp );
			newExp.add( ")" );
			return parser.success(newExp);
		}
		if( Minus() ) {
			Spaces();
			Expr exp = required(UnaryExpr()).single();
			Expr newExp = new Expr(Val.SINGLE,false);
			newExp.add( "LuanImpl.unm(" );
			newExp.addAll( exp );
			newExp.add( ")" );
			return parser.success(newExp);
		}
		if( Keyword("not") ) {
			Spaces();
			Expr exp = required(UnaryExpr()).single();
			Expr newExp = new Expr(Val.SINGLE,false);
			newExp.add( "!Luan.checkBoolean(" );
			newExp.addAll( exp );
			newExp.add( ")" );
			return parser.success(newExp);
		}
		Expr exp = PowExpr();
		if( exp==null )
			return parser.failure(null);
		return parser.success(exp);
	}

	private Expr PowExpr() throws ParseException {
		parser.begin();
		Expr exp1 = SingleExpr();
		if( exp1==null )
			return parser.failure(null);
		if( parser.match('^') ) {
			Spaces();
			Expr exp2 = required(UnaryExpr());
			Expr newExp = new Expr(Val.SINGLE,false);
			newExp.add( "LuanImpl.pow(" );
			newExp.addAll( exp1.single() );
			newExp.add( "," );
			newExp.addAll( exp2.single() );
			newExp.add( ")" );
			exp1 = newExp;
		}
		return parser.success(exp1);
	}

	private Expr SingleExpr() throws ParseException {
		parser.begin();
		Expr exp = FunctionExpr();
		if( exp != null )
			return parser.success(exp);
		exp = VarExp();
		if( exp != null )
			return parser.success(exp);
		exp = VarArgs();
		if( exp != null )
			return parser.success(exp);
		return parser.failure(null);
	}

	private Expr FunctionExpr() throws ParseException {
		if( !Keyword("function") )
			return null;
		return RequiredFunction(null);
	}

	private Expr RequiredFunction(String name) throws ParseException {
		parser.begin();
		RequiredMatch('(');
		Spaces();
		frame = new Frame(frame);
		Stmts stmt = new Stmts();
		List<String> names = NameList();
		if( names != null ) {
/*
			Expr args = new Expr(Val.ARRAY,false);
			args.add( "args" );
			stmt.addAll( makeLocalSetStmt(names,args) );
*/
			int n = names.size();
			Expr t = new Expr(Val.SINGLE,false);
			for( int i=0; i<n; i++ ) {
				t.clear();
				t.add( "LuanImpl.pick(args,"+i+")" );
				stmt.addAll( addSymbol(names.get(i),t) );
			}

			if( parser.match(',') ) {
				Spaces();
				if( !parser.match("...") )
					throw parser.exception();
				Spaces();
				frame.isVarArg = true;
				stmt.add( "final Object[] varArgs = LuanImpl.varArgs(args," + names.size() + ");  " );
			} else {
				stmt.add( "LuanImpl.noMore(args,"+n+");  " );
			}
		} else if( parser.match("...") ) {
			Spaces();
			frame.isVarArg = true;
			stmt.add( "final Object[] varArgs = LuanImpl.varArgs(args,0);  " );
		} else {
			stmt.add( "LuanImpl.noMore(args,0);  " );
		}
		RequiredMatch(')');
		Spaces();
		Stmts block = RequiredBlock();
		stmt.addAll( block );
		stmt.hasReturn = block.hasReturn;
		Expr fnDef = newFnExp(stmt,name);
		RequiredEnd("end_function");
		frame = frame.parent;
		return parser.success(fnDef);
	}

	private Expr VarArgs() throws ParseException {
		parser.begin();
		if( !frame.isVarArg || !parser.match("...") )
			return parser.failure(null);
		Spaces();
		Expr exp = new Expr(Val.ARRAY,false);
		exp.add("varArgs");
		return parser.success(exp);
	}

	private Expr TableExpr() throws ParseException {
		parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		Expr tblExp = new Expr(Val.SINGLE,false);
		tblExp.add( "LuanImpl.table(luan," );
		Expr lastExp = tblExp;
		List<Expr> builder = new ArrayList<Expr>();
/*
		Spaces();
		Field(builder);
		while( FieldSep() ) {
			Spaces();
			Field(builder);
		}
*/
		do {
			Spaces();  lastExp.addNewLines();
			Expr exp = Field();
			if( exp != null ) {
				builder.add(exp);
				lastExp = exp;
				Spaces();  lastExp.addNewLines();
			}
		} while( FieldSep() );
		if( !parser.match('}') )
			throw parser.exception("Expected table element or '}'");
		tblExp.addAll( expString(builder).array() );
		tblExp.add( ")" );
		Spaces();
		tblExp.addNewLines();
		return parser.success( tblExp );
	}

	private boolean FieldSep() throws ParseException {
		return parser.anyOf(",;") || EndOfLine();
	}

	private Expr Field() throws ParseException {
		parser.begin();
		Expr exp = SubExpr();
		if( exp==null )
			exp = NameExpr();
		if( exp!=null && parser.match('=') ) {
			Spaces();
			Expr val = RequiredExpr().single();
			Expr newExp = new Expr(Val.SINGLE,false);
			newExp.add( "new TableField(" );
			newExp.addAll( exp );
			newExp.add( "," );
			newExp.addAll( val );
			newExp.add( ")" );
			return parser.success(newExp);
		}
		parser.rollback();
		Expr exprs = Expression();
		if( exprs != null ) {
			return parser.success(exprs);
		}
		return parser.failure(null);
	}

	private Expr VarExp() throws ParseException {
		Var var = VarZ();
		return var==null ? null : var.exp();
	}

	private Var VarZ() throws ParseException {
		parser.begin();
		Var var = VarStart();
		if( var==null )
			return parser.failure(null);
		Var var2;
		while( (var2=Var2(var.exp())) != null ) {
			var = var2;
		}
		return parser.success(var);
	}

	private Var VarStart() throws ParseException {
		if( parser.match('(') ) {
			Spaces();
			Expr exp = RequiredExpr().single();
			RequiredMatch(')');
			Spaces();
			return exprVar(exp);
		}
		String name = Name();
		if( name != null )
			return nameVar(name);
		Expr exp;
		exp = TableExpr();
		if( exp != null )
			return exprVar(exp);
		exp = Literal();
		if( exp != null )
			return exprVar(exp);
		return null;
	}

	private Var Var2(Expr exp1) throws ParseException {
		parser.begin();
		Expr exp2 = SubExpr();
		if( exp2 != null )
			return parser.success(indexVar(exp1,exp2));
		if( parser.match('.') ) {
			Spaces();
			exp2 = NameExpr();
			if( exp2!=null )
				return parser.success(indexVar(exp1,exp2));
			return parser.failure(null);
		}
		Expr fnCall = Args( exp1, new ArrayList<Expr>() );
		if( fnCall != null )
			return parser.success(exprVar(fnCall));
		return parser.failure(null);
	}

	private interface Var {
		public Expr exp() throws ParseException;
//		public Settable settable() throws ParseException;
		public boolean isSettable();
		public Stmts set(Expr val) throws ParseException;
	}

	private Expr env() {
		Sym sym = getSym("_ENV");
		if( sym != null )
			return sym.exp();
		return null;
	}

	private Var nameVar(final String name) {
		return new Var() {
			private Expr exp = null;

			public Expr exp() throws ParseException {
				if( exp == null ) {
					String sp = parser.sb().toString();
					parser.sb().setLength(0);
					exp = calcExp();
					if( sp.length() > 0 )
						exp.add(sp);
				}
				return exp;
			}

			private Expr calcExp() throws ParseException {
				Sym sym = getSym(name);
				if( sym != null )
					return sym.exp();
				Expr envExpr = env();
				if( envExpr != null )
					return indexExpStr( envExpr, constExpStr(name) );
				parser.failure(null);
				throw parser.exception("name '"+name+"' not defined");
			}

			public boolean isSettable() {
				return true;
			}

			public Stmts set(Expr val) throws ParseException {
				Sym sym = getSym(name);
				if( sym != null ) {
					Stmts stmt = new Stmts();
					stmt.addAll( sym.exp() );
					stmt.add( " = " );
					stmt.addAll( val.single() );
					stmt.add( ";  " );
					return stmt;
				}
				Expr envExpr = env();
				if( envExpr != null )
					return indexVar( envExpr, constExpStr(name) ).set(val);
				parser.failure(null);
				throw parser.exception("name '"+name+"' not defined");
			}
		};
	}

	private Var exprVar(final Expr expr) {
		return new Var() {

			public Expr exp() {
				return expr;
			}

			public boolean isSettable() {
				return false;
			}

			public Stmts set(Expr val) {
				throw new RuntimeException();
			}
		};
	}

	private Var indexVar(final Expr table,final Expr key) {
		return new Var() {

			public Expr exp() {
				return indexExpStr( table, key );
			}

			public boolean isSettable() {
				return true;
			}

			public Stmts set(Expr val) {
				Stmts stmt = new Stmts();
				stmt.add( "LuanImpl.put(luan," );
				stmt.addAll( table.single() );
				stmt.add( "," );
				stmt.addAll( key.single() );
				stmt.add( "," );
				stmt.addAll( val.single() );
				stmt.add( ");  " );
				return stmt;
			}
		};
	}

	private Expr Args(Expr fn,List<Expr> builder) throws ParseException {
		parser.begin();
		return args(builder)
			? parser.success( callExpStr( fn, expString(builder) ) )
			: parser.failure((Expr)null);
	}

	private boolean args(List<Expr> builder) throws ParseException {
		parser.begin();
		if( parser.match('(') ) {
			Spaces();
			ExpList(builder);  // optional
			if( !parser.match(')') )
				throw parser.exception("Expression or ')' expected");
			Spaces();
			parser.upSb();
			return parser.success();
		}
		Expr exp = TableExpr();
		if( exp != null ) {
			builder.add(exp);
			return parser.success();
		}
		exp = StringLiteral();
		if( exp != null ) {
			builder.add(exp);
			return parser.success();
		}
		return parser.failure();
	}

	private Expr ExpStringList() throws ParseException {
		List<Expr> builder = new ArrayList<Expr>();
		return ExpList(builder) ? expString(builder) : null;
	}

	private boolean ExpList(List<Expr> builder) throws ParseException {
		parser.begin();
		Expr exp = Expression();
		if( exp==null )
			return parser.failure();
		exp.addNewLines();
		builder.add(exp);
		while( parser.match(',') ) {
			Spaces();
			exp = RequiredExpr();
			exp.prependNewLines();
			builder.add(exp);
		}
		return parser.success();
	}

	private Expr SubExpr() throws ParseException {
		parser.begin();
		if( !parser.match('[') || parser.test("[") || parser.test("=") )
			return parser.failure(null);
		Spaces();
		Expr exp = RequiredExpr().single();
		RequiredMatch(']');
		Spaces();
		return parser.success(exp);
	}

	private Expr NameExpr() throws ParseException {
		parser.begin();
		String name = Name();
		if( name==null )
			return parser.failure(null);
		return parser.success(constExpStr(name));
	}

	private String RequiredName() throws ParseException {
		parser.begin();
		String name = Name();
		if( name==null )
			throw parser.exception("Name expected");
		return parser.success(name);
	}

	private String Name() throws ParseException {
		int start = parser.begin();
		if( !NameFirstChar() )
			return parser.failure(null);
		while( NameChar() );
		String match = parser.textFrom(start);
		if( keywords.contains(match) )
			return parser.failure(null);
		Spaces();
		parser.upSb();
		return parser.success(match);
	}

	private boolean NameChar() {
		return NameFirstChar() || Digit();
	}

	private boolean NameFirstChar() {
		return parser.inCharRange('a', 'z') || parser.inCharRange('A', 'Z') || parser.match('_');
	}

	private void RequiredMatch(char c) throws ParseException {
		if( !parser.match(c) )
			throw parser.exception("'"+c+"' expected");
	}

	private void RequiredMatch(String s) throws ParseException {
		if( !parser.match(s) )
			throw parser.exception("'"+s+"' expected");
	}

	private void RequiredEnd(String keyword) throws ParseException {
		if( !Keyword("end") && !Keyword(keyword) )
			throw parser.exception("'"+keyword+"' or 'end' expected");
	}

	private void RequiredKeyword(String keyword) throws ParseException {
		if( !Keyword(keyword) )
			throw parser.exception("'"+keyword+"' expected");
	}

	private boolean Keyword(String keyword) throws ParseException {
		parser.begin();
		if( !parser.match(keyword) || NameChar() )
			return parser.failure();
		Spaces();
		parser.upSb();
		return parser.success();
	}

	private static final Set<String> keywords = new HashSet<String>(Arrays.asList(
		"and",
		"break",
		"catch",
		"continue",
		"do",
		"else",
		"elseif",
		"end",
		"end_do",
		"end_for",
		"end_function",
		"end_if",
		"end_try",
		"end_while",
		"false",
		"finally",
		"for",
		"function",
		"goto",
		"if",
		"in",
		"local",
		"nil",
		"not",
		"or",
		"repeat",
		"return",
		"then",
		"true",
		"until",
		"while"
	));

	private Expr Literal() throws ParseException {
		parser.begin();
		Expr exp = new Expr(Val.SINGLE,false);
		if( NilLiteral() ) {
			exp.add( "null" );
			exp.addNewLines();
			return parser.success(exp);
		}
		Boolean b = BooleanLiteral();
		if( b != null ) {
			exp.add( b.toString() );
			exp.addNewLines();
			return parser.success(exp);
		}
		Number n = NumberLiteral();
		if( n != null ) {
			String s = n.toString();
			if( n instanceof Long )
				s += "L";
			exp.add( s );
			exp.addNewLines();
			return parser.success(exp);
		}
		Expr s = StringLiteral();
		if( s != null )
			return parser.success(s);
		return parser.failure(null);
	}

	private static int STR_LIM = 65000;

	private Expr constExpStr(String s) {
		s = s
			.replace("\\","\\\\")
			.replace("\"","\\\"")
			.replace("\n","\\n")
			.replace("\r","\\r")
			.replace("\t","\\t")
			.replace("\b","\\b")
		;
		if( s.length() > STR_LIM ) {
			int len = s.length();
			StringBuilder sb = new StringBuilder();
			sb.append( "LuanImpl.strconcat(" );
			int start = 0;
			while(true) {
				int end = start + STR_LIM;
				if( end >= len )
					break;
				sb.append( "\"" ).append( s.substring(start,end) ).append( "\"," );
				start = end;
			}
			sb.append( "\"" ).append( s.substring(start) ).append( "\")" );
			s = sb.toString();
		} else
			s = "\"" + s + "\"";
		Expr exp = new Expr(Val.SINGLE,false);
		exp.add( s );
		return exp;
	}

	private boolean NilLiteral() throws ParseException {
		return Keyword("nil");
	}

	private Boolean BooleanLiteral() throws ParseException {
		if( Keyword("true") )
			return true;
		if( Keyword("false") )
			return false;
		return null;
	}

	private Number NumberLiteral() throws ParseException {
		parser.begin();
		Number n;
		if( parser.matchIgnoreCase("0x") ) {
			n = HexNumber();
		} else {
			n = DecNumber();
		}
		if( n==null || NameChar() )
			return parser.failure(null);
		Spaces();
		parser.upSb();
		return parser.success(n);
	}

	private Number DecNumber() {
		int start = parser.begin();
		boolean isInt = true;
		if( Int() ) {
			if( parser.match('.') ) {
				isInt = false;
				Int();  // optional
			}
		} else if( parser.match('.') && Int() ) {
			// ok
			isInt = false;
		} else
			return parser.failure(null);
		if( Exponent() )  // optional
			isInt = false;
		String s = parser.textFrom(start);
		if( isInt ) {
			try {
				return parser.success(Integer.valueOf(s));
			} catch(NumberFormatException e) {}
			try {
				return parser.success(Long.valueOf(s));
			} catch(NumberFormatException e) {}
		}
		return parser.success(Double.valueOf(s));
	}

	private boolean Exponent() {
		parser.begin();
		if( !parser.matchIgnoreCase("e") )
			return parser.failure();
		parser.anyOf("+-");  // optional
		if( !Int() )
			return parser.failure();
		return parser.success();
	}

	private boolean Int() {
		if( !Digit() )
			return false;
		while( Digit() );
		return true;
	}

	private boolean Digit() {
		return parser.inCharRange('0', '9');
	}

	private Number HexNumber() {
		int start = parser.begin();
		long nLong = 0;
		double n;
		if( HexInt() ) {
			nLong = Long.parseLong(parser.textFrom(start),16);
			n = (double)nLong;
			if( parser.match('.') ) {
				start = parser.currentIndex();
				if( HexInt() ) {
					String dec = parser.textFrom(start);
					n += (double)Long.parseLong(dec,16) / Math.pow(16,dec.length());
				}
			}
		} else if( parser.match('.') && HexInt() ) {
			String dec = parser.textFrom(start+1);
			n = (double)Long.parseLong(dec,16) / Math.pow(16,dec.length());
		} else {
			return parser.failure(null);
		}
		if( parser.matchIgnoreCase("p") ) {
			parser.anyOf("+-");  // optional
			start = parser.currentIndex();
			if( !HexInt() )
				return parser.failure(null);
			n *= Math.pow(2,(double)Long.parseLong(parser.textFrom(start)));
		}
		if( nLong == n ) {
			int nInt = (int)nLong;
			if( nInt == nLong )
				return parser.success(Integer.valueOf(nInt));
			return parser.success(Long.valueOf(nLong));
		}
		return parser.success(Double.valueOf(n));
	}

	private boolean HexInt() {
		if( !HexDigit() )
			return false;
		while( HexDigit() );
		return true;
	}


	private boolean HexDigit() {
		return Digit() || parser.anyOf("abcdefABCDEF");
	}

	private Expr StringLiteral() throws ParseException {
		Expr s;
		if( (s=QuotedString('"'))==null
			&& (s=QuotedString('\''))==null
			&& (s=LongString())==null
		)
			return null;
		Spaces();
		s.addNewLines();
		return s;
	}

	private Expr LongString() throws ParseException {
		parser.begin();
		if( !parser.match('[') )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( parser.match('=') );
		int nEquals = parser.currentIndex() - start;
		if( !parser.match('[') )
			return parser.failure(null);
		EndOfLine();
		start = parser.currentIndex();
		while( !LongBracketsEnd(nEquals) ) {
			if( !(EndOfLine() || parser.anyChar()) )
				throw parser.exception("Unclosed long string");
		}
		String s = parser.text.substring( start, parser.currentIndex() - nEquals - 2 );
		String rtns = parser.sb().toString();
		parser.sb().setLength(0);
		Expr exp = constExpStr(s);
		if( rtns.length() > 0 )
			exp.add(rtns);
		return parser.success(exp);
	}

	private Expr QuotedString(char quote) throws ParseException {
		parser.begin();
		if( !parser.match(quote) )
			return parser.failure(null);
		StringBuilder buf = new StringBuilder();
		while( !parser.match(quote) ) {
			Character c = EscSeq();
			if( c != null ) {
				buf.append(c);
			} else {
				if( parser.test('\r') || parser.test('\n') || !parser.anyChar() )
					throw parser.exception("Unclosed string");
				buf.append(parser.lastChar());
			}
		}
		return parser.success(constExpStr(buf.toString()));
	}

	private Character EscSeq() {
		parser.begin();
		if( !parser.match('\\') )
			return parser.failure(null);
		if( parser.match('a') )  return parser.success('\u0007');
		if( parser.match('b') )  return parser.success('\b');
		if( parser.match('f') )  return parser.success('\f');
		if( parser.match('n') )  return parser.success('\n');
		if( parser.match('r') )  return parser.success('\r');
		if( parser.match('t') )  return parser.success('\t');
		if( parser.match('v') )  return parser.success('\u000b');
		if( parser.match('\\') )  return parser.success('\\');
		if( parser.match('"') )  return parser.success('"');
		if( parser.match('\'') )  return parser.success('\'');
		int start = parser.currentIndex();
		if( parser.match('x') && HexDigit() && HexDigit() )
			return parser.success((char)Integer.parseInt(parser.textFrom(start+1),16));
		if( parser.match('u') && HexDigit() && HexDigit() && HexDigit() && HexDigit() )
			return parser.success((char)Integer.parseInt(parser.textFrom(start+1),16));
		if( Digit() ) {
			if( Digit() ) Digit();  // optional
			return parser.success((char)Integer.parseInt(parser.textFrom(start)));
		}
		if( MatchEndOfLine() ) {
			return parser.success('\n');
		}
		return parser.failure(null);
	}

	private void Spaces() throws ParseException {
		while( parser.anyOf(" \t") || Comment() || ContinueOnNextLine() );
	}

	private boolean ContinueOnNextLine() {
		parser.begin();
		if( parser.match('\\') && EndOfLine() ) {
			parser.upSb();
			return parser.success();
		} else
			return parser.failure();
	}

	private boolean Comment() throws ParseException {
		if( LongComment() )
			return true;
		if( parser.match("--") ) {
			while( parser.noneOf("\r\n") );
			return true;
		}
		return false;
	}

	private boolean LongComment() throws ParseException {
		parser.begin();
		if( !parser.match("--[") )
			return parser.failure();
		int start = parser.currentIndex();
		while( parser.match('=') );
		int nEquals = parser.currentIndex() - start;
		if( !parser.match('[') )
			return parser.failure();
		while( !LongBracketsEnd(nEquals) ) {
			if( !(EndOfLine() || parser.anyChar()) )
				throw parser.exception("Unclosed comment");
		}
		parser.upSb();
		return parser.success();
	}

	private boolean LongBracketsEnd(int nEquals) {
		parser.begin();
		if( !parser.match(']') )
			return parser.failure();
		while( nEquals-- > 0 ) {
			if( !parser.match('=') )
				return parser.failure();
		}
		if( !parser.match(']') )
			return parser.failure();
		return parser.success();
	}



	private class ParseList extends ArrayList {

		void addNewLines() {
			if( parser.sb().length() > 0 ) {
				add( parser.sb().toString() );
				parser.sb().setLength(0);
/*
if( parser.sourceName.equals("stdin") ) {
	StringWriter sw = new StringWriter();
	new Throwable().printStackTrace(new PrintWriter(sw,true));
//	add(sw.toString());
}
*/
			}
		}

		void prependNewLines() {
			if( parser.sb().length() > 0 ) {
				add( 0, parser.sb().toString() );
				parser.sb().setLength(0);
			}
		}

		ParseList() {
			addNewLines();
		}

		@Override public boolean add(Object obj) {
			if( obj instanceof List )  throw new RuntimeException();
			return super.add(obj);
		}

		@Override public void add(int index,Object obj) {
			if( obj instanceof List )  throw new RuntimeException();
			super.add(index,obj);
		}

		@Override public String toString() {
			StringBuilder sb = new StringBuilder();
			for( Object o : this ) {
				sb.append( o.toString() );
			}
			return sb.toString();
		}
	}


	private enum Val { SINGLE, ARRAY }

	private class Expr extends ParseList {
		final Val valType;
		final boolean isStmt;

		Expr(Val valType,boolean isStmt) {
			this.valType = valType;
			this.isStmt = isStmt;
		}

		Expr single() {
			if( valType==Val.SINGLE )
				return this;
			Expr exp = new Expr(Val.SINGLE,isStmt);
			exp.add( valType==Val.ARRAY ? "LuanImpl.first(" : "Luan.first(" );
			exp.addAll( this );
			exp.add( ")" );
			return exp;
		}

		Expr array() {
			if( valType==Val.ARRAY )
				return this;
			Expr exp = new Expr(Val.ARRAY,isStmt);
			if( valType==Val.SINGLE ) {
				exp.add( "new Object[]{" );
				exp.addAll( this );
				exp.add( "}" );
			} else {
				exp.add( "Luan.array(" );
				exp.addAll( this );
				exp.add( ")" );
			}
			return exp;
		}

	}

	private Expr expString(List<Expr> list) {
		switch(list.size()) {
		case 0:
			{
				Expr exp = new Expr(Val.ARRAY,false);
				exp.add("LuanFunction.NOTHING");
				return exp;
			}
		case 1:
			{
				Expr exp = list.get(0);
				exp.prependNewLines();
				return exp;
			}
		default:
			{
				Expr exp = new Expr(Val.ARRAY,false);
				int lastI = list.size() - 1;
				exp.add( "new Object[]{" );
				for( int i=0; i<lastI; i++ ) {
					exp.addAll( list.get(i).single() );
					exp.add( "," );
				}
				Expr last = list.get(lastI);
				if( last.valType==Val.SINGLE ) {
					exp.addAll( last );
					exp.add( "}" );
				} else {
					exp.add( "}" );
					exp.add( 0, "LuanImpl.concatArgs(" );
					exp.add( "," );
					exp.addAll( last );
					exp.add( ")" );
				}
				return exp;
			}
		}
	}

	private class Stmts extends ParseList {
		boolean hasReturn = false;
	}

	private static String toFnString(Stmts stmts,List<UpSym> upValueSymbols,String className,List<Inner> inners) {
		if( !stmts.hasReturn )
			stmts.add( "\nreturn LuanFunction.NOTHING;" );
		StringBuilder sb = new StringBuilder();
		sb.append( ""
			+"package luan.impl;  "
			+"import luan.LuanClosure;  "
			+"import luan.Luan;  "
			+"import luan.LuanFunction;  "
			+"import luan.LuanException;  "
			+"import luan.modules.PackageLuan;  "

			+"public class " + className +" extends LuanClosure {  "
				+"public "+className+"(Luan luan,boolean javaOk,String sourceName) throws LuanException {  "
					+"super(luan,"+toUpValues(upValueSymbols)+",javaOk,sourceName);  "
				+"}  "

				+"@Override public Object doCall(Luan luan,Object[] args) throws LuanException {  "
					+"final Pointer[] parentUpValues = upValues;  "
					+"Object t;  "
					+"Object[] a;  "
					+ stmts
				+"\n}\n"
		);
		for( Inner inner : inners ) {
			sb.append( '\n' );
			sb.append( inner.toInnerFnString(lines(sb.toString())) + '\n' );
		}
		sb.append( ""
			+"}\n"
		);
		return sb.toString();
	}

	private class Inner {
		private final Stmts stmts;
		private final String name;
		private final String className;
		private final int lines;
		private final int endLine;

		Inner(Stmts stmts,String name,String className) {
			this.stmts = stmts;
			this.name = name;
			this.className = className;

			stmts.addNewLines();
			if( !stmts.hasReturn )
				stmts.add( "return LuanFunction.NOTHING;  " );
			this.lines = lines( stmts.toString() );
			this.endLine = lines( parser.textFrom(0) );
		}

		Expr toInnerFnExp(List<UpSym> upValueSymbols) {
			StringBuilder sb = new StringBuilder();
			sb.append(
				"new "+className+"(luan(),"+toUpValues(upValueSymbols)+",javaOk,sourceName)"
			);
			for( int i=0; i<lines; i++ ) {
				sb.append('\n');
			}

			Expr exp = new Expr(Val.SINGLE,false);
			exp.add( sb.toString() );
			return exp;
		}

		String toInnerFnString(int line) {
			int diff = line + lines - endLine;
			String name = this.name!=null ? this.name : "";
			name += "$" + diff;
			//name += "_" + lines + "_" + endLine + "_" + line;
			StringBuilder sb = new StringBuilder();
			sb.append( ""
				+"private static class " + className +" extends LuanClosure {  "
					+className+"(Luan luan,Pointer[] upValues,boolean javaOk,String sourceName) throws LuanException {  "
						+"super(luan,upValues,javaOk,sourceName);  "
					+"}  "
					+"@Override public Object doCall(Luan luan,Object[] args) throws LuanException {  "
						+"return _" + name + "(luan,args);  "
					+"}  "
					+"private Object _" + name + "(Luan luan,Object[] args) throws LuanException {  "
						+"final Pointer[] parentUpValues = upValues;  "
						+"Object t;  "
						+"Object[] a;  "
						+ stmts
					+"}  "
				+"}  "
			);
			return sb.toString();
		}
	}

	private static int lines(String s) {
		int lines = 0;
		final int n = s.length();
		for( int i=0; i<n; i++ ) {
			if( s.charAt(i) == '\n' )
				lines++;
		}
		return lines;
	}
/*
	private Expr toFnExp(Stmts stmt,List<UpSym> upValueSymbols,String name) {
		stmt.addNewLines();
		if( !stmt.hasReturn )
			stmt.add( "return LuanFunction.NOTHING;  " );
		Expr exp = new Expr(Val.SINGLE,false);
		exp.add( ""
			+"new LuanClosure(luan(),"+upValueSymbols.size()+",javaOk,sourceName) {  "
				+"{  "
				+ init(upValueSymbols)
				+"}  "
				+"@Override public Object doCall(Luan luan,Object[] args) throws LuanException {  "
		);
		if( name != null ) {
			exp.add( ""
					+"return _" + name + "(luan,args);  "
				+"}  "
				+"private Object _" + name + "(Luan luan,Object[] args) throws LuanException {  "
			);
		}
		exp.add( ""
					+"final Pointer[] parentUpValues = upValues;  "
					+"Object t;  "
					+"Object[] a;  "
		);
		exp.addAll( stmt );
		exp.add( ""
				+"}  "
			+"}  "
		);
		return exp;
	}

	private static String init(List<UpSym> upValueSymbols) {
		StringBuilder sb = new StringBuilder();
		for( UpSym upSym : upValueSymbols ) {
			sb.append( upSym.init() );
		}
		return sb.toString();
	}
*/
	private static String toUpValues(List<UpSym> upValueSymbols) {
		StringBuilder sb = new StringBuilder();
		sb.append( "new Pointer[]{ " );
		for( UpSym upSym : upValueSymbols ) {
			sb.append( upSym.value + ", " );
		}
		sb.append( "}" );
		return sb.toString();
	}

}
