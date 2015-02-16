// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// IR0 definition. (For CS322 Lab4, Jingke Li)
//
//
package ir0;
import java.io.*;
import java.util.*;

public class IR0 {
  public static final BoolLit TRUE = new BoolLit(true);
  public static final BoolLit FALSE = new BoolLit(false);

  // Program -> {Inst}
  //
  public static class Program {
    public final Inst[] insts;

    public Program(Inst[] sa) { insts=sa; }
    public Program(List<Inst> sl) { 
      this(sl.toArray(new Inst[0]));
    }
    public String toString() { 
      String str = "# IR0 Program\n";
      for (Inst s: insts)
	str += s;
      return str;
    }
  }

  // Instructions

  public static abstract class Inst {}

  // Inst -> Dest "=" Src BOP Src
  //
  public static class Binop extends Inst {
    public final BOP op;
    public final Dest dst;
    public final Src src1, src2;

    public Binop(BOP o, Dest d, Src s1, Src s2) { 
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

    public Unop(UOP o, Dest d, Src s) { op=o; dst=d; src=s; }

    public String toString() { 
      return " " + dst + " = " + op + src + "\n";
    }
  }

  // Inst -> Dest "=" Src
  //
  public static class Move extends Inst {
    public final Dest dst;
    public final Src src;

    public Move(Dest d, Src s) { dst=d; src=s; }

    public String toString() { 
      return " " + dst + " = " + src + "\n"; 
    }
  }

  // Inst -> Dest "=" "malloc" "(" Src ")"
  //
  public static class Malloc extends Inst {
    public final Dest rdst;
    public final Src arg;

    public Malloc(Dest r, Src a) { rdst=r; arg=a; }
 
    public String toString() { 
      return " " + rdst +  " = malloc (" + arg + ")\n";
    }
  }

  // Inst -> Dest "=" Addr
  //
  public static class Load extends Inst {
    public final Dest dst;
    public final Addr addr;

    public Load(Dest d, Addr a) { dst=d; addr=a; }

    public String toString() { 
      return " " + dst + " = " + addr + "\n"; 
    }
  }
    
  // Inst -> Addr "=" Src
  //
  public static class Store extends Inst {
    public final Addr addr;
    public final Src src;

    public Store(Addr a, Src s) { addr=a; src=s; }

    public String toString() { 
      return " " + addr + " = " + src + "\n"; 
    }
  }

  // Inst -> "print" "(" [Src] ")"
  //
  public static class Print extends Inst {
    public final Src arg;

    public Print() { arg=null; }
    public Print(Src a) { arg=a; }

    public String toString() { 
      return " print (" + ((arg==null) ? "" : arg) + ")\n";
    }
  }

  // Inst -> "if" Src ROP Src "goto" Label
  //
  public static class CJump extends Inst {
    public final ROP op;
    public final Src src1, src2;
    public final Label lab;

    public CJump(ROP o, Src s1, Src s2, Label l) { 
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

    public Jump(Label l) { lab=l; }

    public String toString() { 
      return " goto " + lab + "\n"; 
    }
  }

  // Inst -> Label ":"
  //
  public static class LabelDec extends Inst { 
    public final Label lab;

    public LabelDec(Label l) { lab=l; }

    public String toString() { 
      return lab + ":\n"; 
    }
  }

  // Label -> 
  //
  public static class Label {
    static int labelnum=0;
    public String name;

    public Label() { name = "L" + labelnum++; }
    public Label(String s) { name = s; }
    public void set(String s) { name = s; }
    public String toString() { return name; }
  }

  // Addr -> [<IntLit>] "[" Src "]"
  //
  public static class Addr {
    public final Src base;  
    public final int offset;

    public Addr(Src b) { base=b; offset=0; }
    public Addr(Src b, int o) { base=b; offset=o; }
    public String toString() {
      return "" + ((offset == 0) ? "" : offset) + "[" + base + "]";
    }
  }

  // Operands

  // Src -> <Id> | <Temp> | <IntLit> | <BoolLit>
  //
  public interface Src {}

  // Dest -> <Id> | <Temp> 
  //
  public interface Dest {}

  public static class Id implements Src, Dest  {
    public final String name;

    public Id(String s) { name=s; }
    public String toString() { return name; }
  }

  public static class Temp implements Src, Dest  {
    private static int cnt=0;
    public final int num;

    public Temp() { num = ++Temp.cnt; }
    public Temp(int n) { num=n; }
    public String toString() { return "t" + num; }
  }

  public static class IntLit implements Src {
    public final int i;

    public IntLit(int v) { i=v; }
    public String toString() { return i + ""; }
  }

  public static class BoolLit implements Src {
    public final boolean b;

    public BoolLit(boolean v) { b=v; }
    public String toString() { return b + ""; }
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
