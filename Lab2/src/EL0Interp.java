// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// An EL0 interpreter. (For CS322, Jingke Li)
//
//   Prog -> Exp
//   Exp  -> Exp + Exp
//   Exp  -> let var = Exp in Exp end
//   Exp  -> var
//   Exp  -> num
//
//
import java.io.*;

public class EL0Interp {

  // An environment for variables
  //
  static class Env {
    String var;
    int val;
    Env rest;

    Env() { 
      var=""; val=0; rest=null; 
    }
    Env(String var, int val, Env rest) { 
      this.var=var; this.val=val; this.rest=rest; 
    }

    Env extend(String var, int val) {
      return new Env(var, val, this);
    }

    int lookup(String var) throws Exception {
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
      EL0.Exp prog = new EL0Parser(stream).Program();
      stream.close();
      int val = eval(prog, new Env());
      System.out.println(val);
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  // Dispatch eval to a specific Exp node
  //
  static int eval(EL0.Exp n, Env env) throws Exception {
    if (n instanceof EL0.Plus) return eval(((EL0.Plus) n), env);
    if (n instanceof EL0.Let)  return eval(((EL0.Let) n), env);
    if (n instanceof EL0.Var)  return eval(((EL0.Var) n), env);
    if (n instanceof EL0.Num)  return eval((EL0.Num) n);
    throw new Exception("Unknown Exp: " + n);
  }
    
  // Plus ---
  //  Exp e1;
  //  Exp e2;
  //
  // Semantic actions:
  //  Exp -> Exp1 + Exp2 {Exp.val = Exp1.val + Exp2.val;}
  //
  static int eval(EL0.Plus n, Env env) throws Exception {
    int val1 = eval(n.e1, env);
    int val2 = eval(n.e2, env);
    return val1 + val2;
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
  static int eval(EL0.Let n, Env env) throws Exception {
    int val = eval(n.val, env);
    Env newenv = env.extend(n.var.s, val);
    return eval(n.e, newenv);
  }

  // Var ---
  //  String s;
  //
  // Semantic actions:
  //  Exp -> var {Exp.val = lookup(env, var.id);}
  //
  static int eval(EL0.Var n, Env env) throws Exception {
    return env.lookup(n.s);
  }
  
  // Num ---
  //  int i;
  //
  // Semantic actions:
  //  Exp -> num {Exp.val = num.val;}
  //
  static int eval(EL0.Num n) throws Exception {
    return n.i;
  }

}
