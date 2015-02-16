// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// AST Definition. (For CS322, Jingke Li)
//
package ast;
import java.util.*;

public class Ast {
  static int tab=0;	// indentation for printing AST.

  public abstract static class Node {
    String tab() {
      String str = "";
      for (int i = 0; i < Ast.tab; i++)
	str += " ";
      return str;
    }
  }

  // Define constant nodes for classes with no fields
  // --- to avoid unnecessary object allocation.
  //
  public static final IntType IntType = new IntType();
  public static final BoolType BoolType = new BoolType();
  public static final This This = new This();

  // Program Node -------------------------------------------------------

  public static class Program extends Node {
    public final ClassDecl[] classes;

    public Program(ClassDecl[] ca) { classes=ca; }
    public Program(List<ClassDecl> cl) { 
      this(cl.toArray(new ClassDecl[0]));
    }
    public String toString() { 
      String str = "# AST Program\n";
      for (ClassDecl c: classes) 
	str += c;
      return str;
    }
  }   

  // Declarations -------------------------------------------------------

  public static class ClassDecl extends Node {
    public final String nm;	       // class name
    public final String pnm;	       // parent class name (could be null)
    public final VarDecl[] flds;       // fiels
    public final MethodDecl[] mthds;   // methods

    public ClassDecl(String c, String p, VarDecl[] va, MethodDecl[] ma) {
      nm=c; pnm=p; flds=va; mthds=ma;
    }
    public ClassDecl(String c, String p, List<VarDecl> vl, List<MethodDecl> ml) {
      this(c, p, vl.toArray(new VarDecl[0]), ml.toArray(new MethodDecl[0]));
    }
    public String toString() { 
      String str = "ClassDecl " + nm + " " + (pnm==null ? "" : pnm) + "\n"; 
      Ast.tab = 2; 
      for (VarDecl v: flds) 
	str += v;
      for (MethodDecl m: mthds) 
	str += m;
      return str;
    }
  }

  public static class MethodDecl extends Node {
    public final Type t;	    // return type (could be null)
    public final String nm;	    // method name
    public final Param[] params;    // param parameters
    public final VarDecl[] vars;    // local variables
    public final Stmt[] stmts;	    // method body

    public MethodDecl(Type rt, String m, Param[] fa, VarDecl[] va, Stmt[] sa) {
      t=rt; nm=m; params=fa; vars=va; stmts=sa;
    }
    public MethodDecl(Type rt, String m, List<Param> fl, List<VarDecl> vl, List<Stmt> sl) {
      this(rt, m, fl.toArray(new Param[0]), 
	   vl.toArray(new VarDecl[0]), sl.toArray(new Stmt[0]));
    }
    public String toString() { 
      String str = "  MethodDecl " + (t==null ? "void" : t) + " " + nm + " ("; 
      for (Param f: params) 
	str += f + " ";
      str += ")\n";
      Ast.tab = 3; 
      for (VarDecl v: vars) 
	str += v;
      for (Stmt s: stmts) 
	str += s;
      return str;
    }
  }

  public static class VarDecl extends Node {
    public final Type t;     // variable type
    public final String nm;  // variable name
    public final Exp init;   // init expr (could be null)

    public VarDecl(Type at, String v, Exp e) { t=at; nm=v; init=e; }

    public String toString() { 
      return tab() + "VarDecl " + t + " " + nm + " " + 
	(init==null ? "()" : init) + "\n"; 
    }
  }

  public static class Param extends Node {
    public final Type t;     // parameter type
    public final String nm;  // parameter name

    public Param(Type at, String v) { t=at; nm=v; }

    public String toString() { 
      return "(Param " + t + " " + nm + ")"; 
    }
  }

  // Types --------------------------------------------------------------

  public static abstract class Type extends Node {}

  public static class IntType extends Type {
    public String toString() { return "IntType"; }
  }

  public static class BoolType extends Type {
    public String toString() { return "BoolType"; }
  }

  public static class ArrayType extends Type {
    public final Type et;  // array element type

    public ArrayType(Type t) { et=t; }

    public String toString() { 
      return "(ArrayType " + et + ")"; 
    }
  }

  public static class ObjType extends Type {
    public final String nm;  // object's class name

    public ObjType(String i) { nm=i; }

    public String toString() { 
      return "(ObjType " + nm + ")"; 
    }
  }

  // Statements ---------------------------------------------------------

  public static abstract class Stmt extends Node {}

  public static class Block extends Stmt {
    public final Stmt[] stmts;

    public Block(Stmt[] sa) { stmts=sa; }
    public Block(List<Stmt> sl) { 
      this(sl.toArray(new Stmt[0])); 
    }
    public String toString() { 
      String s = "";
      if (stmts!=null) {
	s = tab() + "{\n";
	Ast.tab++; 
	for (Stmt st: stmts) 
	  s += st;
	Ast.tab--;
	s += tab() + "}\n"; 
      }
      return s;
    }
  }

  public static class Assign extends Stmt {
    public final Exp lhs;  
    public final Exp rhs;  

    public Assign(Exp e1, Exp e2) { lhs=e1; rhs=e2; }

    public String toString() { 
      return tab() + "Assign " + lhs + " " + rhs + "\n"; 
    }
  }

  public static class CallStmt extends Stmt {
    public final Exp obj;     // class object
    public final String nm;   // method name
    public final Exp[] args;  // arguments

    public CallStmt(Exp e, String s, Exp[] ea) { 
      obj=e; nm=s; args=ea; 
    }
    public CallStmt(Exp e, String s, List<Exp> el) { 
      this(e, s, el.toArray(new Exp[0])); 
    }
    public String toString() { 
      String s = tab() + "CallStmt " + obj + " " + nm + " ("; 
      for (Exp e: args) 
	s += e + " "; 
      s += ")\n"; 
      return s;
    }
  }

  public static class If extends Stmt {
    public final Exp cond;  
    public final Stmt s1;   // then clause
    public final Stmt s2;   // else clause (could be null)

    public If(Exp e, Stmt as1, Stmt as2) { cond=e; s1=as1; s2=as2; }

    public String toString() { 
      String str = tab() + "If " + cond + "\n"; 
      Ast.tab++; 
      str += s1; 
      Ast.tab--;
      if (s2 != null) {
	str += tab() + "Else\n";
	Ast.tab++; 
	str += s2; 
	Ast.tab--;
      }
      return str;
    }
  }

  public static class While extends Stmt {
    public final Exp cond;
    public final Stmt s;

    public While(Exp e, Stmt as) { cond=e; s=as; }

    public String toString() { 
      String str = tab() + "While " + cond + "\n";
      Ast.tab++; 
      str += s; 
      Ast.tab--;
      return str;
    }
  }   

  public static class Print extends Stmt {
    public final PrArg arg;  // (could be null)

    public Print(PrArg e) { arg=e; }

    public String toString() { 
      return tab() + "Print " + (arg==null ? "()" : arg) + "\n"; 
    }
  }

  public static class Return extends Stmt {
    public final Exp val;  // (could be null)

    public Return(Exp e) { val=e; }

    public String toString() { 
      return tab() + "Return " + (val==null ? "()" : val) + "\n"; 
    }
  }

  // Print statement's arguements ---------------------------------------

  public interface PrArg {}

  // Expressions --------------------------------------------------------

  public static abstract class Exp extends Node implements PrArg {}

  public static class Binop extends Exp {
    public final BOP op;
    public final Exp e1;
    public final Exp e2;

    public Binop(BOP o, Exp ae1, Exp ae2) { op=o; e1=ae1; e2=ae2; }

    public String toString() { 
      return "(Binop " + op + " " + e1 + " " + e2 + ")";
    }
  }

  public static class Unop extends Exp {
    public final UOP op;
    public final Exp e;

    public Unop(UOP o, Exp ae) { op=o; e=ae; }

    public String toString() { 
      return "(Unop " + op + " " + e + ")";
    }
  }

  public static class Call extends Exp {
    public final Exp obj;     // class object
    public final String nm;   // method name
    public final Exp[] args;  // arguments

    public Call(Exp e, String s, Exp[] ea) { 
      obj=e; nm=s; args=ea; 
    }
    public Call(Exp e, String s, List<Exp> el) { 
      this(e, s, el.toArray(new Exp[0])); 
    }
    public String toString() { 
      String str ="(Call " + obj + " " + nm + " ("; 
      for (Exp e: args) 
	str += e + " "; 
      str += "))"; 
      return str; 
    }
  }

  public static class NewArray extends Exp {
    public final Type et;  // element type
    public final int len;  // array length

    public NewArray(Type t, int i) { et=t; len=i; }

    public String toString() { 
      return "(NewArray " + et + " " + len + ")";
    }
  }

  public static class ArrayElm extends Exp {
    public final Exp ar;   // array object
    public final Exp idx;  // element's index

    public ArrayElm(Exp e1, Exp e2) { ar=e1; idx=e2; }

    public String toString() { 
      return "(ArrayElm " + ar + " " + idx +")";
    }
  }

  public static class NewObj extends Exp {
    public final String nm;   // class name

    public NewObj(String s) { nm=s; }
    public String toString() { 
      return "(NewObj " + nm + ")"; 
    }
  }

  public static class Field extends Exp {
    public final Exp obj;    // class object
    public final String nm;  // field name

    public Field(Exp e, String s) { obj=e; nm=s; }

    public String toString() { 
      return "(Field " + obj + " " +  nm + ") ";
    }
  }

  public static class Id extends Exp {
    public final String nm;  // id name

    public Id(String s) { nm=s; }
    public String toString() { return nm; }
  }

  public static class This extends Exp {
    public String toString() { return "This"; }
  }

  public static class IntLit extends Exp {
    public final int i; 
    
    public IntLit(int ai) { i=ai; }
    public String toString() { return i + ""; }
  }

  public static class BoolLit extends Exp {
    public final boolean b;	

    public BoolLit(boolean ab) { b=ab; }
    public String toString() { return b + ""; }
  }

  // String literal is not an expression
  public static class StrLit extends Node implements PrArg {
    public final String s;	

    public StrLit(String as) { s=as; }
    public String toString() { return "\"" + s + "\""; }
  }

  // Operators ----------------------------------------------------------

  public static enum BOP {
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), AND("&&"), OR("||"),
    EQ("=="), NE("!="), LT("<"), LE("<="), GT(">"), GE(">=");
    private String name;

    BOP(String n) { name = n; }
    public String toString() { return name; }
  }

  public static enum UOP {
    NEG("-"), NOT("!");
    private String name;

    UOP(String n) { name = n; }
    public String toString() { return name; }
  }

}
