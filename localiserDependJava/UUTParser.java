// Generated from UUT by ANTLR 4.8
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class UUTParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		MUL=1, DIV=2, ADD=3, MIN=4, LPAR=5, RPAR=6, ID=7, NUM=8, WS=9;
	public static final int
		RULE_program = 0, RULE_jeyujenmus = 1, RULE_tdolgrauay = 2, RULE_lhplowmtzg = 3, 
		RULE_gtmzywiqtu = 4, RULE_jtzldcsbuy = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "jeyujenmus", "tdolgrauay", "lhplowmtzg", "gtmzywiqtu", "jtzldcsbuy"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'*'", "'/'", "'+'", "'-'", "'('", "')'", "'x'", "'0'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "MUL", "DIV", "ADD", "MIN", "LPAR", "RPAR", "ID", "NUM", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "UUT"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public UUTParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ProgramContext extends RuleContextWithAltNum {
		public JeyujenmusContext jeyujenmus() {
			return getRuleContext(JeyujenmusContext.class,0);
		}
		public TerminalNode EOF() { return getToken(UUTParser.EOF, 0); }
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).exitProgram(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(12);
			jeyujenmus();
			setState(13);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JeyujenmusContext extends RuleContextWithAltNum {
		public LhplowmtzgContext lhplowmtzg() {
			return getRuleContext(LhplowmtzgContext.class,0);
		}
		public TerminalNode ADD() { return getToken(UUTParser.ADD, 0); }
		public TdolgrauayContext tdolgrauay() {
			return getRuleContext(TdolgrauayContext.class,0);
		}
		public JeyujenmusContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jeyujenmus; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).enterJeyujenmus(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).exitJeyujenmus(this);
		}
	}

	public final JeyujenmusContext jeyujenmus() throws RecognitionException {
		JeyujenmusContext _localctx = new JeyujenmusContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_jeyujenmus);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(15);
			lhplowmtzg();
			setState(16);
			match(ADD);
			setState(17);
			tdolgrauay();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TdolgrauayContext extends RuleContextWithAltNum {
		public List<TerminalNode> LPAR() { return getTokens(UUTParser.LPAR); }
		public TerminalNode LPAR(int i) {
			return getToken(UUTParser.LPAR, i);
		}
		public TerminalNode MUL() { return getToken(UUTParser.MUL, 0); }
		public TerminalNode MIN() { return getToken(UUTParser.MIN, 0); }
		public TerminalNode NUM() { return getToken(UUTParser.NUM, 0); }
		public TdolgrauayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tdolgrauay; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).enterTdolgrauay(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).exitTdolgrauay(this);
		}
	}

	public final TdolgrauayContext tdolgrauay() throws RecognitionException {
		TdolgrauayContext _localctx = new TdolgrauayContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_tdolgrauay);
		try {
			setState(27);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(19);
				match(LPAR);
				setState(20);
				match(LPAR);
				setState(21);
				match(MUL);
				setState(22);
				match(MIN);
				setState(23);
				match(NUM);
				}
				break;
			case EOF:
			case NUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(25);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(24);
					match(NUM);
					}
					break;
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LhplowmtzgContext extends RuleContextWithAltNum {
		public List<TerminalNode> NUM() { return getTokens(UUTParser.NUM); }
		public TerminalNode NUM(int i) {
			return getToken(UUTParser.NUM, i);
		}
		public TerminalNode RPAR() { return getToken(UUTParser.RPAR, 0); }
		public TerminalNode LPAR() { return getToken(UUTParser.LPAR, 0); }
		public LhplowmtzgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lhplowmtzg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).enterLhplowmtzg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).exitLhplowmtzg(this);
		}
	}

	public final LhplowmtzgContext lhplowmtzg() throws RecognitionException {
		LhplowmtzgContext _localctx = new LhplowmtzgContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_lhplowmtzg);
		try {
			setState(34);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(29);
				match(NUM);
				setState(30);
				match(RPAR);
				setState(31);
				match(LPAR);
				setState(32);
				match(NUM);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(33);
				match(NUM);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GtmzywiqtuContext extends RuleContextWithAltNum {
		public TerminalNode ID() { return getToken(UUTParser.ID, 0); }
		public TerminalNode MIN() { return getToken(UUTParser.MIN, 0); }
		public TerminalNode DIV() { return getToken(UUTParser.DIV, 0); }
		public TerminalNode NUM() { return getToken(UUTParser.NUM, 0); }
		public GtmzywiqtuContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gtmzywiqtu; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).enterGtmzywiqtu(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).exitGtmzywiqtu(this);
		}
	}

	public final GtmzywiqtuContext gtmzywiqtu() throws RecognitionException {
		GtmzywiqtuContext _localctx = new GtmzywiqtuContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_gtmzywiqtu);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(36);
			match(ID);
			setState(37);
			match(MIN);
			setState(38);
			match(DIV);
			setState(39);
			match(NUM);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JtzldcsbuyContext extends RuleContextWithAltNum {
		public List<TerminalNode> ID() { return getTokens(UUTParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(UUTParser.ID, i);
		}
		public JeyujenmusContext jeyujenmus() {
			return getRuleContext(JeyujenmusContext.class,0);
		}
		public List<TerminalNode> NUM() { return getTokens(UUTParser.NUM); }
		public TerminalNode NUM(int i) {
			return getToken(UUTParser.NUM, i);
		}
		public JtzldcsbuyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jtzldcsbuy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).enterJtzldcsbuy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof UUTListener ) ((UUTListener)listener).exitJtzldcsbuy(this);
		}
	}

	public final JtzldcsbuyContext jtzldcsbuy() throws RecognitionException {
		JtzldcsbuyContext _localctx = new JtzldcsbuyContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_jtzldcsbuy);
		int _la;
		try {
			setState(54);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(44);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==ID) {
					{
					{
					setState(41);
					match(ID);
					}
					}
					setState(46);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case NUM:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(47);
				jeyujenmus();
				setState(51);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NUM) {
					{
					{
					setState(48);
					match(NUM);
					}
					}
					setState(53);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\13;\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\5\4\34\n\4\5\4\36\n\4\3\5\3\5\3\5\3\5\3\5\5\5%\n\5\3"+
		"\6\3\6\3\6\3\6\3\6\3\7\7\7-\n\7\f\7\16\7\60\13\7\3\7\3\7\7\7\64\n\7\f"+
		"\7\16\7\67\13\7\5\79\n\7\3\7\2\2\b\2\4\6\b\n\f\2\2\2:\2\16\3\2\2\2\4\21"+
		"\3\2\2\2\6\35\3\2\2\2\b$\3\2\2\2\n&\3\2\2\2\f8\3\2\2\2\16\17\5\4\3\2\17"+
		"\20\7\2\2\3\20\3\3\2\2\2\21\22\5\b\5\2\22\23\7\5\2\2\23\24\5\6\4\2\24"+
		"\5\3\2\2\2\25\26\7\7\2\2\26\27\7\7\2\2\27\30\7\3\2\2\30\31\7\6\2\2\31"+
		"\36\7\n\2\2\32\34\7\n\2\2\33\32\3\2\2\2\33\34\3\2\2\2\34\36\3\2\2\2\35"+
		"\25\3\2\2\2\35\33\3\2\2\2\36\7\3\2\2\2\37 \7\n\2\2 !\7\b\2\2!\"\7\7\2"+
		"\2\"%\7\n\2\2#%\7\n\2\2$\37\3\2\2\2$#\3\2\2\2%\t\3\2\2\2&\'\7\t\2\2\'"+
		"(\7\6\2\2()\7\4\2\2)*\7\n\2\2*\13\3\2\2\2+-\7\t\2\2,+\3\2\2\2-\60\3\2"+
		"\2\2.,\3\2\2\2./\3\2\2\2/9\3\2\2\2\60.\3\2\2\2\61\65\5\4\3\2\62\64\7\n"+
		"\2\2\63\62\3\2\2\2\64\67\3\2\2\2\65\63\3\2\2\2\65\66\3\2\2\2\669\3\2\2"+
		"\2\67\65\3\2\2\28.\3\2\2\28\61\3\2\2\29\r\3\2\2\2\b\33\35$.\658";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}