// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// IR1 definition. (For CS322, Jingke Li)
//
//
package ir1;
import java.io.*;
import java.util.*;

public class IR1 {
  public static final BoolLit TRUE = new BoolLit(true);
  public static final BoolLit FALSE = new BoolLit(false);

  // Program -> {Func}
  //
  public static class Program {
    public final Func[] funcs;

    Program(Func[] f) { funcs=f; }
    Program(List<Func> fl) { this(fl.toArray(new Func[0])); }
    public String toString() { 
      String str = "# IR1 Program\n";
      for (Func f: funcs)
	str += "\n" + f;
      return str;
    }
  }

  // Func -> <global> VarList [VarList] {Inst}
  //
  public static class Func {
    public final String name;
    public final String[] params;
    public final String[] locals;
    public final Inst[] code;

    Func(String n, String[] p, String[] l, Inst[] c) {
      name=n; params=p; locals=l; code=c; 
    }
    Func(String n, List<String> pl, List<String> ll, List<Inst> cl) {
      this(n, pl.toArray(new String[0]), ll.toArray(new String[0]),
	   cl.toArray(new Inst[0])); 
    }
    public String toString() { 
      String header = "_" + name + " " + StringArrayToString(params) + "\n" +
  	                (locals.length==0? "" : StringArrayToString(locals) + "\n");
      String body = "";
      for (Inst s: code)
	body += s.toString();
      return header + "{\n" + body + "}\n";
    }
  }

  // VarList -> "(" [String {"," String}] ")"
  //
  static String StringArrayToString(String[] vars) {
    String s = "(";
    if (vars.length > 0) {
      s += vars[0];
      for (int i=1; i<vars.length; i++)
	s += ", " + vars[i];
    }
    return s + ")";
  }

  // Instructions

  public static abstract class Inst {}

  // Inst -> Dest "=" Src BOP Src
  //
  public static class Binop extends Inst {
    public final BOP op;
    public final Dest dst;
    public final Src src1, src2;

    Binop(BOP o, Dest d, Src s1, Src s2) { 
      op=o; dst=d; src1=s1; src2=s2; 
    }
    public String toString() { 
      return " " + dst + " = " + src1 + " " + op + " " + src2 + "\n";
    }
  }

  // Inst -> Dest "=" UOP Src
  //
  public static class Unop extends Inst {
    public final UOP op;
    public final Dest dst;
    public final Src src;

    Unop(UOP o, Dest d, Src s) { op=o; dst=d; src=s; }
    public String toString() { 
      return " " + dst + " = " + op + src + "\n";
    }
  }

  // Inst -> Dest "=" Src
  //
  public static class Move extends Inst {
    public final Dest dst;
    public final Src src;

    Move(Dest d, Src s) { dst=d; src=s; }
    public String toString() { 
      return " " + dst + " = " + src + "\n"; 
    }
  }

  // Inst -> Dest "=" Addr
  //
  public static class Load extends Inst {
    public final Dest dst;
    public final Addr addr;

    Load(Dest d, Addr a) { dst=d; addr=a; }
    public String toString() { 
      return " " + dst + " = " + addr + "\n"; 
    }
  }
    
  // Inst -> Addr "=" Src
  //
  public static class Store extends Inst {
    public final Addr addr;
    public final Src src;

    Store(Addr a, Src s) { addr=a; src=s; }
    public String toString() { 
      return " " + addr + " = " + src + "\n"; 
    }
  }

  // Inst -> [Dest "="] "call" <Global> "(" [Src {"," Src}] ")"
  //
  public static class Call extends Inst {
    public final String name;
    public final Dest rdst;    // could be null
    public final Src[] args;

    Call(String n, Src[] a, Dest r) { 
      name=n; args=a; rdst=r;
    }
    Call(String n, List<Src> al, Dest r) { 
      this(n, al.toArray(new Src[0]), r);
    }
    Call(String n, List<Src> al) { 
      this(n, al.toArray(new Src[0]), null);
    }
    public String toString() { 
      String arglist = "(";
      if (args.length > 0) {
	arglist += args[0];
	for (int i=1; i<args.length; i++)
	  arglist += ", " + args[i];
      }
      arglist +=  ")";
      String retstr = (rdst==null) ? " " : " " + rdst + " = ";
      return retstr +  "call _" + name + arglist + "\n";
    }
  }

  // Inst -> "return" [Src]
  //
  public static class Return extends Inst {
    public final Src val;	// could be null

    Return() { val=null; }
    Return(Src s) { val=s; }
    public String toString() { 
      return " return " + (val==null ? "" : val) + "\n"; 
    }
  }

  // Inst -> "if" Src ROP Src "goto" Label
  //
  public static class CJump extends Inst {
    public final ROP op;
    public final Src src1, src2;
    public final Label lab;

    CJump(ROP o, Src s1, Src s2, Label l) { 
      op=o; src1=s1; src2=s2; lab=l; 
    }
    public String toString() { 
      return " if " + src1 + " " + op + " " + src2 + 
	" goto " + lab + "\n";
    }
  }

  // Inst -> "goto" Label
  //
  public static class Jump extends Inst {
    public final Label lab;

    Jump(Label l) { lab=l; }
    public String toString() { 
      return " goto " + lab + "\n"; 
    }
  }

  // Inst -> Label ":"
  //
  public static class LabelDec extends Inst { 
    public final String name;

    LabelDec(String s) { name=s; }
    public String toString() { 
      return name + ":\n"; 
    }
  }

  // Label -> 
  //
  public static class Label {
    static int labelnum=0;
    public String name;

    Label() { name = "L" + labelnum++; }
    Label(String s) { name = s; }
    public void set(String s) { name = s; }
    public String toString() { return name; }
  }

  // Addr -> [<IntLit>] "[" Src "]"
  //
  public static class Addr {
    public final Src base;  
    public final int offset;

    Addr(Src b) { base=b; offset=0; }
    Addr(Src b, int o) { base=b; offset=o; }
    public String toString() {
      return "" + ((offset == 0) ? "" : offset) + "[" + base + "]";
    }
  }

  // Operands

  // Src -> <Id> | <Temp> | <IntLit> | <BoolLit> | <StrLit> 
  //
  public interface Src {}

  // Dest -> <Id> | <Temp> 
  //
  public interface Dest {}

  public static class Global {
    public final String name;

    Global(String s) { name = s; }
    public String toString() { return "_" + name; }
  }

  public static class Id implements Src, Dest  {
    public final String name;

    Id(String s) { name=s; }
    public String toString() { return name; }
  }

  public static class Temp implements Src, Dest  {
    private static int cnt=0;
    public final int num;

    Temp() { num = ++Temp.cnt; }
    Temp(int n) { num=n; }
    public String toString() { return "t" + num; }
  }

  public static class IntLit implements Src {
    public final int i;

    IntLit(int v) { i=v; }
    public String toString() { return i + ""; }
  }

  public static class BoolLit implements Src {
    public final boolean b;

    BoolLit(boolean v) { b=v; }
    public String toString() { return b + ""; }
  }

  public static class StrLit implements Src {
    public final String s;

    StrLit(String v) { s=v; }
    public String toString() { return "\"" + s + "\""; }
  }

  // Operators

  public static interface BOP {}

  public static enum AOP implements BOP {
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), AND("&&"), OR("||");
    final String name;

    AOP(String n) { name = n; }
    public String toString() { return name; }
  }

  public static enum ROP implements BOP {
    EQ("=="), NE("!="), LT("<"), LE("<="), GT(">"), GE(">=");
    final String name;

    ROP(String n) { name = n; }
    public String toString() { return name; }
  }

  public static enum UOP {
    NEG("-"), NOT("!");
    final String name;

    UOP(String n) { name = n; }
    public String toString() { return name; }
  }

}
