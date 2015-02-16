// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// EL1 AST specification. (For CS322, Jingke Li)
//
//
public class EL1 {

  public abstract static class Exp {}

  // Exp -> Exp + Exp
  //
  public static class Plus extends Exp {
    final Exp e1;
    final Exp e2;
    Plus(Exp e1, Exp e2) { this.e1=e1; this.e2=e2; }
    public String toString() { return e1 + " + " + e2; }
  }

  // Exp -> let var = Exp in Exp end
  //
  public static class Let extends Exp {
    final Var var;
    final Exp val;
    final Exp e;
    Let(Var var, Exp val, Exp e) { 
      this.var=var; this.val=val; this.e=e; 
    }
    public String toString() { 
      return "let " + var + " = " + val + " in " + e + " end";
    }
  }

  // Exp -> fn var => Exp
  //
  public static class Func extends Exp {
    final Var var;
    final Exp body;
    Func(Var var, Exp body) { this.var=var; this.body=body; }
    public String toString() { 
      return "fn " + var + " => " + body;
    }
  }

  // Exp -> Exp Exp
  //
  public static class Call extends Exp {
    final Exp fn;
    final Exp arg;
    Call(Exp fn, Exp arg) { this.fn=fn; this.arg=arg; }
    public String toString() { 
      return (fn instanceof Var)? fn + " " + arg : "(" + fn + ") " + arg; 
    }
  }

  // Exp -> var
  //
  public static class Var extends Exp {
    final String s;
    Var(String s) { this.s=s; }
    public String toString() { return s; }
  }

  // Exp -> num
  //
  public static class Num extends Exp {
    final int i;	
    Num(int ai) { i=ai; }
    public String toString() { return i + ""; }
  }

}
