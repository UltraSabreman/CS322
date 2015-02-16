// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// An EL1 interpreter. (For CS322, Jingke Li)
//
//   Prog -> Exp
//   Exp  -> Exp + Exp
//   Exp  -> let var = Exp in Exp end
//   Exp  -> var
//   Exp  -> num
//   Exp  -> fn var => Exp
//   Exp  -> Exp Exp
//
//
import java.io.*;

public class EL1Interp {

  // Value representation
  //
  static abstract class Val {}

  // -- integer values
  //
  static class IntVal extends Val {
    int i;
    IntVal(int i) { this.i = i; }
    public String toString() { return "" + i; }
  }

  // -- function values (as closures)
  //
  static class Closure extends Val {
    EL1.Var var;
    EL1.Exp body;
    Env env;
    Closure(EL1.Var var, EL1.Exp body, Env env) { 
      this.var=var; this.body=body; this.env=env; 
    }
    public String toString() { 
      return "(fn " + var + " => " + body + "; " + env + ")"; 
    }
  }

  // An environment for variables
  //
  static class Env {
    String var;
    Val val;
    Env rest;

    Env() { 
      var=""; val=null; rest=null; 
    }
    Env(String var, Val val, Env rest) { 
      this.var=var; this.val=val; this.rest=rest; 
    }

    Env extend(String var, Val val) {
      return new Env(var, val, this);
    }

    Val lookup(String var) throws Exception {
      Env env = this;
      for (; env!=null; env=env.rest) {
	if (var.equals(env.var))
	  return env.val;
      }
      throw new Exception("Variable " + var + " not defined");
    }
  }

  // The main routine
  //
  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
      FileInputStream stream = new FileInputStream(args[0]);
      EL1.Exp prog = new EL1Parser(stream).Program();
      stream.close();
      int val = ((IntVal) eval(prog, new Env())).i;
      System.out.println(val);
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  // Dispatch eval to a specific Exp node
  //
  static Val eval(EL1.Exp n, Env env) throws Exception {
    if (n instanceof EL1.Plus) return eval(((EL1.Plus) n), env);
    if (n instanceof EL1.Let)  return eval(((EL1.Let) n), env);
    if (n instanceof EL1.Func) return eval(((EL1.Func) n), env);
    if (n instanceof EL1.Call) return eval(((EL1.Call) n), env);
    if (n instanceof EL1.Var)  return eval(((EL1.Var) n), env);
    if (n instanceof EL1.Num)  return eval((EL1.Num) n);
    throw new Exception("Unknown Exp: " + n);
  }
    
  // Plus ---
  //  Exp e1;
  //  Exp e2;
  //
  // Semantic actions:
  //  Exp -> Exp1 + Exp2 {Exp.val = Exp1.val + Exp2.val;}
  //
  static Val eval(EL1.Plus n, Env env) throws Exception {
    int val1 = ((IntVal) eval(n.e1, env)).i;
    int val2 = ((IntVal) eval(n.e2, env)).i;
    return new IntVal(val1 + val2);
  }	
  
  // Let ---
  //  Var var;
  //  Exp val;
  //  Exp e;
  //
  // Semantic actions:
  //  Exp -> let var = Exp1 {env = extend(env, var.id, Exp1.val);}
  //         in Exp2 end {env = retract(env, var.id); 
  //                      Exp.val = Exp2.val;}
  //
  static Val eval(EL1.Let n, Env env) throws Exception {
    Val val = eval(n.val, env);
    Env newenv = env.extend(n.var.s, val);
    return eval(n.e, newenv);
  }

  // Var ---
  //  String s;
  //
  // Semantic actions:
  //  Exp -> var {Exp.val = lookup(env, var.id);}
  //
  static Val eval(EL1.Var n, Env env) throws Exception {
    return env.lookup(n.s);
  }
  
  // Num ---
  //  int i;
  //
  // Semantic actions:
  //  Exp -> num {Exp.val = num.val;}
  //
  static Val eval(EL1.Num n) throws Exception {
    return new IntVal(n.i);
  }

  // Func ---
  //  Var var;
  //  Exp body;
  //
  // Semantic actions:
  //  Exp -> fn var => Exp1 {Exp.val = closure(var.id, Exp1, env);}
  //
  static Val eval(EL1.Func n, Env env) throws Exception {
    System.out.println("-234234234-");
    Closure test = new Closure(n.var, n.body, env);
    System.out.println(test.toString());
    return test;

  }

  // Call ---
  //  Exp fn;
  //  Exp arg;
  //
  // Semantic actions:
  //  Exp -> Exp1 {c = Exp1.val;}
  //         Exp2 {actual = Exp2.val; stack.push(env);
  //               env = extend(c.env, c.formal, actual);
  //               Exp.val = c.body.val; env = stack.pop();}
  //
  static Val eval(EL1.Call n, Env env) throws Exception {
    //Do this in an IDE

    Val argValue = eval((EL1.Num)n.arg);
    Closure functionClosure= (Closure)env.lookup(((EL1.Var)n.fn).s);

    Env functionEnv = functionClosure.env.extend(functionClosure.var.s, argValue); //eval(new EL1.Num(5))
    

    return eval(functionClosure.body, functionEnv);
  }

}
