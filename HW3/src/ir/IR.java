// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
// 
// Three-address IR definitions. 
// (Some nodes are not strictly 3-address.)
//
// Based on an earlier version by Andrew Tolmach. - Jingke Li
//
// Version 7. (Last modified on 1/8/15.)   
//
package ir;
import java.io.*;
import java.util.*;

public class IR {

  public static class IRException extends Exception {
    IRException(String text) {
      super(text);
    }
  }

  public static final IntLit ZERO = new IntLit(0);
  public static final BoolLit TRUE = new BoolLit(true);
  public static final BoolLit FALSE = new BoolLit(false);

  public static boolean indexed = false;
  public static int linenum = 0;
  static String line(boolean count, String s) {
    String idx = (indexed && count) ? (linenum++) + ". " 
                   + (linenum<11 ? " " : "") : "";
    return idx + s;
  }

  // Types

  public static enum Type {
    BOOL(":B",1,FALSE), INT(":I",4,ZERO), PTR(":P",8,ZERO);
    final String name;
    public final int size;
    public final Src defaultValue;
    Type(String s, int i, Src v) { name=s; size=i; defaultValue=v; }
    public String toString() { return name; }
  }

  // Program
  
  public static class Program {
    public final Data[] data;
    public final Func[] funcs;

    public Program(Data[] d, Func[] f) { data=d; funcs=f; }
    public Program(List<Data> dl, List<Func> fl) { 
      this(dl.toArray(new Data[0]), fl.toArray(new Func[0]));
    }
    public String toIndexedString() { 
      indexed = true;
      return toString();
    }
    public String toString() { 
      String str = "# IR Program\n";
      if (data != null && data.length > 0)
	str += "\n";
      for (Data d: data)
	str += d;
      for (Func f: funcs)
	str += "\n" + f;
      return str;
    }
  }

  // Global data records

  public static class Data {
    public final Global name;
    public final int size;
    public final Global[] items;
    
    public Data(Global n, int i, Global[] l) {
      name = n; size = i; items = l;
    }
    public Data(Global n, int i, List<Global> ll) {
      this(n, i, ll.toArray(new Global[0]));
    }
    public String toString() { 
      String str = "data " + name + " (sz=" + size + "): ";
      if (items.length > 0) {
	str += items[0].toString();
	for (int i=1; i<items.length; i++)
	  str += ", " + items[i];
      }
      return str + "\n";
    }
  }

  // Functions

  public static class Func {
    public final String name;
    public final String[] params;
    public final String[] locals;
    public final Inst[] code;

    public Func(String n, String[] p, String[] l, Inst[] c) {
      name=n; params=p; locals=l; code=c; 
    }
    public Func(String n, List<String> pl, List<String> ll, List<Inst> cl) {
      this(n, pl.toArray(new String[0]), ll.toArray(new String[0]),
	   cl.toArray(new Inst[0])); 
    }
    public String toString() { 
      String header = line(false, "_" + name + " " + StringArrayToString(params)
			   + "\n")
	              + (locals.length==0? "" : 
			 line(false, StringArrayToString(locals) + "\n"));
      String body = "";
      linenum = 0;
      for (Inst s: code)
	body += s.toString();
      return header + line(false,"{\n") + body + line(false,"}\n");
    }
  }

  public static String StringArrayToString(String[] vars) {
    String s = "(";
    if (vars.length > 0) {
      s += vars[0];
      for (int i=1; i<vars.length; i++)
	s += ", " + vars[i];
    }
    return s + ")";
  }

  // Instructions

  public static abstract class Inst {
    abstract public Object accept(InstVisitor v) throws IRException;
  }

  public interface InstVisitor {
    Object visit(Binop c) throws IRException;
    Object visit(Unop c) throws IRException;
    Object visit(Move c) throws IRException;
    Object visit(Load c) throws IRException;
    Object visit(Store c) throws IRException;
    Object visit(Call c) throws IRException;
    Object visit(Return c) throws IRException;
    Object visit(CJump c) throws IRException;
    Object visit(Jump c) throws IRException;
    Object visit(LabelDec c) throws IRException;
  }

  public static class Binop extends Inst {
    public final BOP op;
    public final Dest dst;
    public final Src src1, src2;

    public Binop(BOP o, Dest d, Src s1, Src s2) { 
      op=o; dst=d; src1=s1; src2=s2; 
    }
    public String toString() { 
      return line(true, " " + dst + " = " + src1 + " " + op + " " + src2 + "\n");
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Unop extends Inst {
    public final UOP op;
    public final Dest dst;
    public final Src src;

    public Unop(UOP o, Dest d, Src s) { op=o; dst=d; src=s; }
    public String toString() { 
      return line(true, " " + dst + " = " + op + src + "\n");
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Move extends Inst {
    public final Dest dst;
    public final Src src;

    public Move(Dest d, Src s) { dst=d; src=s; }
    public String toString() { 
      return line(true, " " + dst + " = " + src + "\n"); 
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Load extends Inst {
    public final Type type;
    public final Dest dst;
    public final Addr addr;

    public Load (Type t, Dest d, Addr a) { type=t; dst=d; addr=a; }
    public String toString() { 
      return line(true, " " + dst + " = " + addr + type + "\n"); 
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }
    
  public static class Store extends Inst {
    public final Type type;
    public final Addr addr;
    public final Src src;

    public Store(Type t, Addr a, Src s) { type=t; addr=a; src=s; }
    public String toString() { 
      return line(true, " " + addr + type + " = " + src + "\n"); 
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Call extends Inst {
    public final CallTgt tgt;
    public final boolean ind;	// true if indirect
    public final Src[] args;
    public final Dest rdst;     // could be null

    public Call(CallTgt f, boolean b, Src[] a, Dest r) { 
      tgt=f; ind=b; args=a; rdst=r;
    }
    public Call(CallTgt f, boolean b, List<Src> al, Dest r) { 
      this(f, b, al.toArray(new Src[0]), r);
    }
    public Call(CallTgt f, boolean b, List<Src> al) { 
      this(f, b, al.toArray(new Src[0]), null);
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
      return line(true, retstr +  "call " + (ind ? "* " : "") +
		  tgt + arglist + "\n");
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Return extends Inst {
    public final Src val;	// could be null

    public Return() { val=null; }
    public Return(Src s) { val=s; }
    public String toString() { 
      return line(true, " return " + (val==null ? "" : val) + "\n"); 
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class CJump extends Inst {
    public final ROP op;
    public final Src src1, src2;
    public final Label lab;

    public CJump(ROP o, Src s1, Src s2, Label l) { 
      op=o; src1=s1; src2=s2; lab=l; 
    }
    public String toString() { 
      return line(true, " if " + src1 + " " + op + " " + src2 + 
	" goto " + lab + "\n");
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Jump extends Inst {
    public final Label lab;

    public Jump(Label l) { lab=l; }
    public String toString() { 
      return line(true, " goto " + lab + "\n"); 
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class LabelDec extends Inst { 
    public final Label lab;

    public LabelDec(Label l) { lab=l; }

    public String toString() { 
	return line(true, lab + ":\n"); 
    }
    public Object accept(InstVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  // Label

  public static class Label {
    static int labelnum=0;
    public String name;

    public Label() { name = "L" + labelnum++; }
    public Label(String s) { name = s; }
    public void set(String s) { name = s; }
    public String toString() { return name; }
  }

  // Address

  public static class Addr {   // Memory at base + offset
    public final Src base;  
    public final int offset;

    public Addr(Src b) { base=b; offset=0; }
    public Addr(Src b, int o) { base=b; offset=o; }
    public String toString() {
      return "" + ((offset == 0) ? "" : offset) + "[" + base + "]";
    }
  }

  // Operands

  public interface Src {
    Object accept(SrcVisitor v) throws IRException;
  }

  public interface SrcVisitor {
    Object visit(Id rand) throws IRException;
    Object visit(Temp rand) throws IRException;
    Object visit(Global rand) throws IRException;
    Object visit(IntLit rand) throws IRException;
    Object visit(BoolLit rand) throws IRException;
    Object visit(StrLit rand) throws IRException;
  }

  public interface Dest {
    Object accept(DestVisitor v) throws IRException;
  }

  public interface DestVisitor {
    Object visit(Id rand) throws IRException;
    Object visit(Temp rand) throws IRException;
      //    Object visit(Global rand) throws IRException;
  }

  public interface CallTgt {
  }

  public interface Reg {
  }

  public static class Id implements Reg, Src, Dest, CallTgt  {
    public final String name;

    public Id(String s) { name=s; }
    public String toString() { return name; }
    public boolean equals(Object l) {
      return (l instanceof Id && (((Id) l).name.equals(name)));
    }
    public int hashCode() {  
      return name.hashCode(); 
    }
    public Object accept(SrcVisitor v) throws IRException {
      return v.visit(this);
    }
    public Object accept(DestVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Temp implements Reg, Src, Dest, CallTgt  {
    private static int cnt=0;
    public final int num;

    public Temp(int n) { num=n; }
    public Temp() { num = ++Temp.cnt; }
    public static void reset() { Temp.cnt = 0; }
    public static int getcnt() { return Temp.cnt; }
    public String toString() { return "t" + num; }
    public boolean equals(Object l) {
      return (l instanceof Temp && (((Temp) l).num == num));
    }
    public int hashCode() {  
      return num; 
    }
    public Object accept(SrcVisitor v) throws IRException {
      return v.visit(this);
    }
    public Object accept(DestVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class Global implements Src, CallTgt {
    public final String name;

    public Global(String s) { name = s; }
    public String toString() { return "_" + name; }  // should be environment dependent
    public Object accept(SrcVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class IntLit implements Src {
    public final int i;

    public IntLit(int v) { i=v; }
    public String toString() { return i + ""; }
    public Object accept(SrcVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class BoolLit implements Src {
    public final boolean b;

    public BoolLit(boolean v) { b=v; }
    public String toString() { return b + ""; }
    public Object accept(SrcVisitor v) throws IRException {
      return v.visit(this);
    }
  }

  public static class StrLit implements Src {
    public final String s;

    public StrLit(String v) { s=v; }
    public String toString() { return "\"" + s + "\""; }
    public Object accept(SrcVisitor v) throws IRException {
      return v.visit(this);
    }
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
