// Generated from /home/jaco/Code/honsProj/antlr-localizer/default/src/main/antlr4/za/ac/sun/cs/localizer/UUT.g4 by ANTLR 4.7.1
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
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LPAR=1, RPAR=2, Digit=3, Mulop=4, Addop=5, WS=6;
	public static final int
		RULE_program = 0, RULE_expression = 1, RULE_term = 2, RULE_factor = 3, 
		RULE_constant = 4;
	public static final String[] ruleNames = {
		"program", "expression", "term", "factor", "constant"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'('", "')'", "'0'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "LPAR", "RPAR", "Digit", "Mulop", "Addop", "WS"
	};
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
	public String getGrammarFileName() { return "UUT.g4"; }

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
	public static class ProgramContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode EOF() { return getToken(UUTParser.EOF, 0); }
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(10);
			expression();
			setState(11);
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

	public static class ExpressionContext extends ParserRuleContext {
		public TerminalNode Digit() { return getToken(UUTParser.Digit, 0); }
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(13);
			match(Digit);
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

	public static class TermContext extends ParserRuleContext {
		public List<TerminalNode> Addop() { return getTokens(UUTParser.Addop); }
		public TerminalNode Addop(int i) {
			return getToken(UUTParser.Addop, i);
		}
		public TerminalNode Mulop() { return getToken(UUTParser.Mulop, 0); }
		public FactorContext factor() {
			return getRuleContext(FactorContext.class,0);
		}
		public TermContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_term; }
	}

	public final TermContext term() throws RecognitionException {
		TermContext _localctx = new TermContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_term);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(15);
			match(Addop);
			{
			setState(16);
			match(Mulop);
			setState(17);
			factor();
			setState(18);
			match(Addop);
			}
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

	public static class FactorContext extends ParserRuleContext {
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public TerminalNode LPAR() { return getToken(UUTParser.LPAR, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(UUTParser.RPAR, 0); }
		public FactorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_factor; }
	}

	public final FactorContext factor() throws RecognitionException {
		FactorContext _localctx = new FactorContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_factor);
		try {
			setState(26);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Digit:
				enterOuterAlt(_localctx, 1);
				{
				setState(20);
				constant();
				}
				break;
			case LPAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(21);
				match(LPAR);
				setState(22);
				expression();
				setState(23);
				match(RPAR);
				}
				break;
			case Addop:
				enterOuterAlt(_localctx, 3);
				{
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

	public static class ConstantContext extends ParserRuleContext {
		public List<TerminalNode> Digit() { return getTokens(UUTParser.Digit); }
		public TerminalNode Digit(int i) {
			return getToken(UUTParser.Digit, i);
		}
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_constant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(28);
			match(Digit);
			setState(32);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Digit) {
				{
				{
				setState(29);
				match(Digit);
				}
				}
				setState(34);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\b&\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\5\5\35\n\5\3\6\3\6\7\6!\n\6\f\6\16\6$\13\6\3\6\2"+
		"\2\7\2\4\6\b\n\2\2\2#\2\f\3\2\2\2\4\17\3\2\2\2\6\21\3\2\2\2\b\34\3\2\2"+
		"\2\n\36\3\2\2\2\f\r\5\4\3\2\r\16\7\2\2\3\16\3\3\2\2\2\17\20\7\5\2\2\20"+
		"\5\3\2\2\2\21\22\7\7\2\2\22\23\7\6\2\2\23\24\5\b\5\2\24\25\7\7\2\2\25"+
		"\7\3\2\2\2\26\35\5\n\6\2\27\30\7\3\2\2\30\31\5\4\3\2\31\32\7\4\2\2\32"+
		"\35\3\2\2\2\33\35\3\2\2\2\34\26\3\2\2\2\34\27\3\2\2\2\34\33\3\2\2\2\35"+
		"\t\3\2\2\2\36\"\7\5\2\2\37!\7\5\2\2 \37\3\2\2\2!$\3\2\2\2\" \3\2\2\2\""+
		"#\3\2\2\2#\13\3\2\2\2$\"\3\2\2\2\4\34\"";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}