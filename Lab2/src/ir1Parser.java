/* Generated By:JavaCC: Do not edit this line. ir1Parser.java */

import java.util.*;
import java.io.*;

public class ir1Parser implements ir1ParserConstants {
  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
        FileInputStream stream = new FileInputStream(args[0]);
        IR1.Program p = new ir1.ir1Parser(stream).Program();
        stream.close();
        System.out.print(p);
    } else {
        System.out.println("Need a file name as command-line argument.");
    }
  }

// Program -> {Func}
//
  static final public IR1.Program Program() throws ParseException {
  List<IR1.Func> funcs = new ArrayList<IR1.Func>();
  IR1.Func f;
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case Global:
      case 17:
        ;
        break;
      default:
        jj_la1[0] = jj_gen;
        break label_1;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case Global:
        f = Func();
               funcs.add(f);
        break;
      case 17:
        jj_consume_token(17);
        break;
      default:
        jj_la1[1] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    jj_consume_token(0);
    {if (true) return new IR1.Program(funcs);}
    throw new Error("Missing return statement in function");
  }

// Func -> <Global> VarList [VarList] "{" {Inst | "\n"} "}"
//
  static final public IR1.Func Func() throws ParseException {
  List<IR1.Inst> code = new ArrayList<IR1.Inst>();
  List<String> locals = new ArrayList<String>();
  List<String> params;
  IR1.Inst inst;
  IR1.Global g;
    g = Global();
    params = VarList();
    jj_consume_token(17);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 20:
      locals = VarList();
      jj_consume_token(17);
      break;
    default:
      jj_la1[2] = jj_gen;
      ;
    }
    jj_consume_token(18);
    label_2:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 5:
      case 6:
      case 7:
      case 8:
      case IntLit:
      case Temp:
      case Id:
      case 17:
      case 25:
        ;
        break;
      default:
        jj_la1[3] = jj_gen;
        break label_2;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 5:
      case 6:
      case 7:
      case 8:
      case IntLit:
      case Temp:
      case Id:
      case 25:
        inst = Inst();
                        code.add(inst);
        break;
      case 17:
        jj_consume_token(17);
        break;
      default:
        jj_la1[4] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    jj_consume_token(19);
    {if (true) return new IR1.Func(g.name,params,locals,code);}
    throw new Error("Missing return statement in function");
  }

// VarList -> "(" [<Id> {"," <Id>}] ")"
//
  static final public List<String> VarList() throws ParseException {
  Token t;
  List<String> vars = new ArrayList<String>();
    jj_consume_token(20);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case Id:
      t = jj_consume_token(Id);
                 vars.add(t.image);
      label_3:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case 21:
          ;
          break;
        default:
          jj_la1[5] = jj_gen;
          break label_3;
        }
        jj_consume_token(21);
        t = jj_consume_token(Id);
                     vars.add(t.image);
      }
      break;
    default:
      jj_la1[6] = jj_gen;
      ;
    }
    jj_consume_token(22);
    {if (true) return vars;}
    throw new Error("Missing return statement in function");
  }

// Inst -> ( Dest "=" Src AOP Src              	// Binop
//         | Dest "=" UOP Src                   // Unop
//         | Dest "=" Src                       // Move
//         | Dest "=" Addr                      // Load
//         | Addr "=" Src         	        // Store
//         | [Dest "="] "call" <Id> ArgList     // Call
//         | "if" Src ROP Src "goto" Label     	// CJump
//         | "goto" Label                      	// Jump
//         | Label ":"                         	// LabelDec
//         ) <EOL>
//
  static final public IR1.Inst Inst() throws ParseException {
  IR1.Inst inst=null;
  IR1.Addr addr;
  IR1.Dest dst;
  IR1.Src src=null, src2, arg;
  List<IR1.Src> args;
  IR1.Label lab;
  IR1.BOP bop;
  IR1.ROP rop;
  IR1.UOP uop;
  IR1.Global g;
    if (jj_2_2(2)) {
      dst = Dest();
      jj_consume_token(23);
      if (jj_2_1(2)) {
        addr = Addr();
                                            inst = new IR1.Load(dst,addr);
      } else {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case IntLit:
        case BoolLit:
        case StrLit:
        case Temp:
        case Id:
          src = Src();
          switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
          case 27:
          case 28:
          case 29:
          case 30:
          case 31:
          case 32:
          case 33:
          case 34:
          case 35:
          case 36:
          case 37:
          case 38:
            bop = BOP();
            src2 = Src();
                                            inst = new IR1.Binop(bop,dst,src,src2);
            break;
          default:
            jj_la1[7] = jj_gen;
            ;
          }
                                            if (inst==null) inst = new IR1.Move(dst,src);
          break;
        case 28:
        case 39:
          uop = UOP();
          src = Src();
                                            inst = new IR1.Unop(uop,dst,src);
          break;
        case 7:
          jj_consume_token(7);
          g = Global();
          args = ArgList();
                                            inst = new IR1.Call(g.name,args,dst);
          break;
        default:
          jj_la1[8] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
      }
    } else {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case IntLit:
      case 25:
        addr = Addr();
        jj_consume_token(23);
        src = Src();
                                            inst = new IR1.Store(addr,src);
        break;
      case 7:
        jj_consume_token(7);
        g = Global();
        args = ArgList();
                                            inst = new IR1.Call(g.name,args);
        break;
      case 8:
        jj_consume_token(8);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case IntLit:
        case BoolLit:
        case StrLit:
        case Temp:
        case Id:
          src = Src();
          break;
        default:
          jj_la1[9] = jj_gen;
          ;
        }
                                            inst = new IR1.Return(src);
        break;
      case 6:
        jj_consume_token(6);
        src = Src();
        rop = ROP();
        src2 = Src();
        jj_consume_token(5);
        lab = Label();
                                            inst = new IR1.CJump(rop,src,src2,lab);
        break;
      case 5:
        jj_consume_token(5);
        lab = Label();
                                            inst = new IR1.Jump(lab);
        break;
      case Id:
        lab = Label();
        jj_consume_token(24);
                                            inst = new IR1.LabelDec(lab.name);
        break;
      default:
        jj_la1[10] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    jj_consume_token(17);
    {if (true) return inst;}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.Label Label() throws ParseException {
  Token t;
    t = jj_consume_token(Id);
    {if (true) return new IR1.Label(t.image);}
    throw new Error("Missing return statement in function");
  }

// ArgList -> "(" [Src {"," Src}] ")"
//
  static final public List<IR1.Src> ArgList() throws ParseException {
  List<IR1.Src> args = new ArrayList<IR1.Src>();
  IR1.Src arg;
    jj_consume_token(20);
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case IntLit:
    case BoolLit:
    case StrLit:
    case Temp:
    case Id:
      arg = Src();
                    args.add(arg);
      label_4:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case 21:
          ;
          break;
        default:
          jj_la1[11] = jj_gen;
          break label_4;
        }
        jj_consume_token(21);
        arg = Src();
                          args.add(arg);
      }
      break;
    default:
      jj_la1[12] = jj_gen;
      ;
    }
    jj_consume_token(22);
    {if (true) return args;}
    throw new Error("Missing return statement in function");
  }

// Src -> Id | Temp | IntLit | BoolLit | StrLit
//
  static final public IR1.Src Src() throws ParseException {
  IR1.Src src;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case Id:
      src = Id();
      break;
    case Temp:
      src = Temp();
      break;
    case IntLit:
      src = IntLit();
      break;
    case BoolLit:
      src = BoolLit();
      break;
    case StrLit:
      src = StrLit();
      break;
    default:
      jj_la1[13] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return src;}
    throw new Error("Missing return statement in function");
  }

// Addr -> [IntLit] "[" Src "]"
//
  static final public IR1.Addr Addr() throws ParseException {
  IR1.IntLit v; int offset=0; IR1.Src base;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case IntLit:
      v = IntLit();
                 offset = v.i;
      break;
    default:
      jj_la1[14] = jj_gen;
      ;
    }
    jj_consume_token(25);
    base = Src();
    jj_consume_token(26);
    {if (true) return new IR1.Addr(base, offset);}
    throw new Error("Missing return statement in function");
  }

// Dest -> Id | Temp  
//
  static final public IR1.Dest Dest() throws ParseException {
  IR1.Dest dst;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case Id:
      dst = Id();
      break;
    case Temp:
      dst = Temp();
      break;
    default:
      jj_la1[15] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return dst;}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.Temp Temp() throws ParseException {
  Token t; String s;
    t = jj_consume_token(Temp);
    s = t.image.substring(1,t.image.length());
    {if (true) return new IR1.Temp(Integer.parseInt(s));}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.Id Id() throws ParseException {
  Token t;
    t = jj_consume_token(Id);
           {if (true) return new IR1.Id(t.image);}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.Global Global() throws ParseException {
  Token t;
    t = jj_consume_token(Global);
    {if (true) return new IR1.Global(t.image.substring(1,t.image.length()));}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.IntLit IntLit() throws ParseException {
  Token t;
    t = jj_consume_token(IntLit);
               {if (true) return new IR1.IntLit(Integer.parseInt(t.image));}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.BoolLit BoolLit() throws ParseException {
  Token t;
    t = jj_consume_token(BoolLit);
                {if (true) return new IR1.BoolLit(Boolean.parseBoolean(t.image));}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.StrLit StrLit() throws ParseException {
  Token t;
    t = jj_consume_token(StrLit);
    {if (true) return new IR1.StrLit(t.image.substring(1,t.image.length()-1));}
    throw new Error("Missing return statement in function");
  }

// Operators
  static final public IR1.BOP BOP() throws ParseException {
  IR1.BOP op=null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 27:
    case 28:
    case 29:
    case 30:
    case 31:
    case 32:
      op = AOP();
      break;
    case 33:
    case 34:
    case 35:
    case 36:
    case 37:
    case 38:
      op = ROP();
      break;
    default:
      jj_la1[16] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return op;}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.AOP AOP() throws ParseException {
  IR1.AOP op=null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 27:
      jj_consume_token(27);
           op = IR1.AOP.ADD;
      break;
    case 28:
      jj_consume_token(28);
                                        op = IR1.AOP.SUB;
      break;
    case 29:
      jj_consume_token(29);
           op = IR1.AOP.MUL;
      break;
    case 30:
      jj_consume_token(30);
                                        op = IR1.AOP.DIV;
      break;
    case 31:
      jj_consume_token(31);
           op = IR1.AOP.AND;
      break;
    case 32:
      jj_consume_token(32);
                                        op = IR1.AOP.OR;
      break;
    default:
      jj_la1[17] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return op;}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.ROP ROP() throws ParseException {
  IR1.ROP op=null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 33:
      jj_consume_token(33);
           op = IR1.ROP.EQ;
      break;
    case 34:
      jj_consume_token(34);
                                        op = IR1.ROP.NE;
      break;
    case 35:
      jj_consume_token(35);
           op = IR1.ROP.LT;
      break;
    case 36:
      jj_consume_token(36);
                                        op = IR1.ROP.LE;
      break;
    case 37:
      jj_consume_token(37);
           op = IR1.ROP.GT;
      break;
    case 38:
      jj_consume_token(38);
                                        op = IR1.ROP.GE;
      break;
    default:
      jj_la1[18] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return op;}
    throw new Error("Missing return statement in function");
  }

  static final public IR1.UOP UOP() throws ParseException {
  IR1.UOP op=null;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 28:
      jj_consume_token(28);
           op = IR1.UOP.NEG;
      break;
    case 39:
      jj_consume_token(39);
                                        op = IR1.UOP.NOT;
      break;
    default:
      jj_la1[19] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return op;}
    throw new Error("Missing return statement in function");
  }

  static private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  static private boolean jj_2_2(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_2(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  static private boolean jj_3R_17() {
    if (jj_scan_token(Id)) return true;
    return false;
  }

  static private boolean jj_3R_10() {
    if (jj_3R_18()) return true;
    return false;
  }

  static private boolean jj_3R_14() {
    if (jj_3R_11()) return true;
    return false;
  }

  static private boolean jj_3R_18() {
    if (jj_scan_token(Temp)) return true;
    return false;
  }

  static private boolean jj_3R_9() {
    if (jj_3R_17()) return true;
    return false;
  }

  static private boolean jj_3R_6() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_9()) {
    jj_scanpos = xsp;
    if (jj_3R_10()) return true;
    }
    return false;
  }

  static private boolean jj_3R_13() {
    if (jj_3R_18()) return true;
    return false;
  }

  static private boolean jj_3R_20() {
    if (jj_scan_token(StrLit)) return true;
    return false;
  }

  static private boolean jj_3_1() {
    if (jj_3R_5()) return true;
    return false;
  }

  static private boolean jj_3R_7() {
    if (jj_3R_11()) return true;
    return false;
  }

  static private boolean jj_3R_16() {
    if (jj_3R_20()) return true;
    return false;
  }

  static private boolean jj_3_2() {
    if (jj_3R_6()) return true;
    if (jj_scan_token(23)) return true;
    return false;
  }

  static private boolean jj_3R_5() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_7()) jj_scanpos = xsp;
    if (jj_scan_token(25)) return true;
    if (jj_3R_8()) return true;
    return false;
  }

  static private boolean jj_3R_19() {
    if (jj_scan_token(BoolLit)) return true;
    return false;
  }

  static private boolean jj_3R_12() {
    if (jj_3R_17()) return true;
    return false;
  }

  static private boolean jj_3R_8() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_12()) {
    jj_scanpos = xsp;
    if (jj_3R_13()) {
    jj_scanpos = xsp;
    if (jj_3R_14()) {
    jj_scanpos = xsp;
    if (jj_3R_15()) {
    jj_scanpos = xsp;
    if (jj_3R_16()) return true;
    }
    }
    }
    }
    return false;
  }

  static private boolean jj_3R_11() {
    if (jj_scan_token(IntLit)) return true;
    return false;
  }

  static private boolean jj_3R_15() {
    if (jj_3R_19()) return true;
    return false;
  }

  static private boolean jj_initialized_once = false;
  /** Generated Token Manager. */
  static public ir1ParserTokenManager token_source;
  static SimpleCharStream jj_input_stream;
  /** Current token. */
  static public Token token;
  /** Next token. */
  static public Token jj_nt;
  static private int jj_ntk;
  static private Token jj_scanpos, jj_lastpos;
  static private int jj_la;
  static private int jj_gen;
  static final private int[] jj_la1 = new int[20];
  static private int[] jj_la1_0;
  static private int[] jj_la1_1;
  static {
      jj_la1_init_0();
      jj_la1_init_1();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {0x30000,0x30000,0x100000,0x202c9e0,0x202c9e0,0x200000,0x8000,0xf8000000,0x1000f880,0xf800,0x20089e0,0x200000,0xf800,0xf800,0x800,0xc000,0xf8000000,0xf8000000,0x0,0x10000000,};
   }
   private static void jj_la1_init_1() {
      jj_la1_1 = new int[] {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x7f,0x80,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x7f,0x1,0x7e,0x80,};
   }
  static final private JJCalls[] jj_2_rtns = new JJCalls[2];
  static private boolean jj_rescan = false;
  static private int jj_gc = 0;

  /** Constructor with InputStream. */
  public ir1Parser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public ir1Parser(java.io.InputStream stream, String encoding) {
    if (jj_initialized_once) {
      System.out.println("ERROR: Second call to constructor of static parser.  ");
      System.out.println("       You must either use ReInit() or set the JavaCC option STATIC to false");
      System.out.println("       during parser generation.");
      throw new Error();
    }
    jj_initialized_once = true;
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new ir1ParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 20; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  static public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  static public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 20; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public ir1Parser(java.io.Reader stream) {
    if (jj_initialized_once) {
      System.out.println("ERROR: Second call to constructor of static parser. ");
      System.out.println("       You must either use ReInit() or set the JavaCC option STATIC to false");
      System.out.println("       during parser generation.");
      throw new Error();
    }
    jj_initialized_once = true;
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ir1ParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 20; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  static public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 20; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public ir1Parser(ir1ParserTokenManager tm) {
    if (jj_initialized_once) {
      System.out.println("ERROR: Second call to constructor of static parser. ");
      System.out.println("       You must either use ReInit() or set the JavaCC option STATIC to false");
      System.out.println("       during parser generation.");
      throw new Error();
    }
    jj_initialized_once = true;
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 20; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(ir1ParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 20; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  static private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  static final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  static private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  static final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  static final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  static private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  static private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  static private int[] jj_expentry;
  static private int jj_kind = -1;
  static private int[] jj_lasttokens = new int[100];
  static private int jj_endpos;

  static private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  static public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[40];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 20; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
          if ((jj_la1_1[i] & (1<<j)) != 0) {
            la1tokens[32+j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 40; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  static final public void enable_tracing() {
  }

  /** Disable tracing. */
  static final public void disable_tracing() {
  }

  static private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 2; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
            case 1: jj_3_2(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  static private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}
