// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// IR0 code generator. (For CS322)
//
//  - short-circuit semantics for Boolean expressions
//
//
import java.lang.invoke.SwitchPoint;
import java.util.*;
import java.io.*;
import ast0.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import ir0.*;

class IR0GenOpt {

  static class GenException extends Exception {
    public GenException(String msg) { super(msg); }
  }

  // For returning <src,code> pair from gen routines
  //
  static class CodePack {
    IR0.Src src;
    List<IR0.Inst> code;
    CodePack(IR0.Src src, List<IR0.Inst> code) { 
      this.src=src; this.code=code; 
    }
    CodePack(IR0.Src src) { 
      this.src=src; code=new ArrayList<IR0.Inst>(); 
    }
  } 

  // For returning <addr,code> pair from genAddr routines
  //
  static class AddrPack {
    IR0.Addr addr;
    List<IR0.Inst> code;
    AddrPack(IR0.Addr addr, List<IR0.Inst> code) { 
      this.addr=addr; this.code=code; 
    }
    AddrPack(IR0.Addr addr) { 
      this.addr=addr; code=new ArrayList<IR0.Inst>(); 
    }
  }

  // The main routine
  //
  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
      FileInputStream stream = new FileInputStream(args[0]);
      Ast0.Program p = new ast0Parser(stream).Program();
      stream.close();
      IR0.Program ir0 = IR0GenOpt.gen(p);
      System.out.print(ir0.toString());
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  // Ast0.Program ---
  // Ast0.Stmt[] stmts;
  //
  // AG:
  //   code: stmts.c  -- append all individual stmt.c

  public static IR0.Program gen(Ast0.Program n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    for (Ast0.Stmt s: n.stmts) {
      code.addAll(gen(Crawler(s)));
      //code.addAll(gen(s));
    }
    return new IR0.Program(code);
  }

  public static Ast0.Stmt Crawler(Ast0.Stmt cur) throws Exception {
    if (cur == null) return cur;//throw new Exception("Null Statement");

    if (cur instanceof Ast0.Assign) {
      Ast0.Assign temp = (Ast0.Assign) cur;

      return new Ast0.Assign(CodeFolder(temp.lhs), CodeFolder(temp.rhs));
    } else if (cur instanceof  Ast0.If) {
      Ast0.If temp = (Ast0.If) cur;

      return new Ast0.If(CodeFolder(temp.cond), Crawler(temp.s1), Crawler(temp.s2));
    } else if (cur instanceof Ast0.While) {
      Ast0.While temp = (Ast0.While) cur;

      return new Ast0.While(CodeFolder(temp.cond), Crawler(temp.s));
    } else if (cur instanceof Ast0.Print) {
      Ast0.Print temp = (Ast0.Print) cur;

      return new Ast0.Print(CodeFolder(temp.arg));
    }

    throw new Exception("Invalid Statement Type: " + cur.getClass().getName());
  }

  public static Ast0.Exp CodeFolder (Ast0.Exp cur) throws Exception{
    if (cur == null) return cur;//throw new Exception("Null Expression");

    if (cur instanceof Ast0.IntLit || cur instanceof Ast0.BoolLit)
      return cur;
    else if (cur instanceof Ast0.Binop) {
      Ast0.Binop temp = (Ast0.Binop) cur;

      Ast0.Exp e1 = CodeFolder(temp.e1);
      Ast0.Exp e2 = CodeFolder(temp.e2);

      if (!((e1 instanceof Ast0.IntLit && e2 instanceof Ast0.IntLit) || e1 instanceof Ast0.BoolLit || e2 instanceof Ast0.BoolLit))
        return new Ast0.Binop(temp.op, temp.e1, temp.e2);

      if (temp.op == Ast0.BOP.ADD) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.IntLit(i1.i + i2.i);
      } else if (temp.op == Ast0.BOP.SUB) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.IntLit(i1.i - i2.i);
      } else if (temp.op == Ast0.BOP.MUL) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.IntLit(i1.i * i2.i);
      } else if (temp.op == Ast0.BOP.DIV) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.IntLit(i1.i / i2.i);
      } else if (temp.op == Ast0.BOP.LT) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.BoolLit(i1.i < i2.i);
      } else if (temp.op == Ast0.BOP.LE) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.BoolLit(i1.i <= i2.i);
      } else if (temp.op == Ast0.BOP.GT) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.BoolLit(i1.i > i2.i);
      } else if (temp.op == Ast0.BOP.GE) {
        Ast0.IntLit i1 = (Ast0.IntLit)e1;
        Ast0.IntLit i2 = (Ast0.IntLit)e2;

        return new Ast0.BoolLit(i1.i >= i2.i);
      } else if (temp.op == Ast0.BOP.AND) {
        if (e1 instanceof Ast0.BoolLit && e2 instanceof Ast0.BoolLit) {
          Ast0.BoolLit i1 = (Ast0.BoolLit) e1;
          Ast0.BoolLit i2 = (Ast0.BoolLit) e2;

          return new Ast0.BoolLit(i1.b && i2.b);
        } else if ((e1 instanceof Ast0.BoolLit && !((Ast0.BoolLit)e1).b)
                || (e2 instanceof Ast0.BoolLit && !((Ast0.BoolLit)e2).b))
          return new Ast0.BoolLit(false);
        else
          return new Ast0.Binop(temp.op, temp.e1, temp.e2);
      } else if (temp.op == Ast0.BOP.OR) {
        if (e1 instanceof Ast0.BoolLit && e2 instanceof Ast0.BoolLit) {
          Ast0.BoolLit i1 = (Ast0.BoolLit)e1;
          Ast0.BoolLit i2 = (Ast0.BoolLit)e2;

          return new Ast0.BoolLit(i1.b || i2.b);
        } else if ((e1 instanceof Ast0.BoolLit && ((Ast0.BoolLit)e1).b)
                || (e2 instanceof Ast0.BoolLit && ((Ast0.BoolLit)e2).b))
          return new Ast0.BoolLit(true);
         else if (e1 instanceof Ast0.BoolLit && !((Ast0.BoolLit)e1).b)
          return e2;
         else if (e2 instanceof Ast0.BoolLit && !((Ast0.BoolLit)e2).b)
          return e1;
         else
          return new Ast0.Binop(temp.op, temp.e1, temp.e2);
      } else if (temp.op == Ast0.BOP.EQ) {
        Ast0.IntLit i1;
        Ast0.IntLit i2;

        if (e1 instanceof Ast0.BoolLit)
          i1 = new Ast0.IntLit(((Ast0.BoolLit)e1).b ? 1 : 0);
        else
          i1 = (Ast0.IntLit)e1;

        if (e2 instanceof Ast0.BoolLit)
          i2 = new Ast0.IntLit(((Ast0.BoolLit)e2).b ? 1 : 0);
        else
          i2 = (Ast0.IntLit)e2;

        return new Ast0.BoolLit(i1.i == i2.i);
      } else if (temp.op == Ast0.BOP.NE) {
        Ast0.IntLit i1;
        Ast0.IntLit i2;

        if (e1 instanceof Ast0.BoolLit)
          i1 = new Ast0.IntLit(((Ast0.BoolLit)e1).b ? 1 : 0);
        else
          i1 = (Ast0.IntLit)e1;

        if (e2 instanceof Ast0.BoolLit)
          i2 = new Ast0.IntLit(((Ast0.BoolLit)e2).b ? 1 : 0);
        else
          i2 = (Ast0.IntLit)e2;

        return new Ast0.BoolLit(i1.i != i2.i);
      }
    } else if (cur instanceof Ast0.Unop) {
      Ast0.Unop temp = (Ast0.Unop) cur;

      Ast0.Exp e = CodeFolder(temp.e);

      if (!(e instanceof Ast0.BoolLit || e instanceof Ast0.IntLit))
        return new Ast0.Unop(temp.op, temp.e);

      if (temp.op == Ast0.UOP.NOT) {
        Ast0.BoolLit b = (Ast0.BoolLit)e;

        return new Ast0.BoolLit(!b.b);
      } else if (temp.op == Ast0.UOP.NEG) {
        Ast0.IntLit i = (Ast0.IntLit)e;

        return new Ast0.IntLit(-i.i);
      }
    }  else if (cur instanceof Ast0.NewArray) {
      Ast0.NewArray temp = (Ast0.NewArray) cur;

      return new Ast0.NewArray(temp.len);
    }  else if (cur instanceof Ast0.ArrayElm) {
      Ast0.ArrayElm temp = (Ast0.ArrayElm) cur;
      Ast0.Exp ar = CodeFolder(temp.ar);
      Ast0.Exp idx =  CodeFolder(temp.idx);
      //Todo: Array folding, that's it?
      return new Ast0.ArrayElm(ar,idx);
    }  else if (cur instanceof Ast0.Id) {
      Ast0.Id temp = (Ast0.Id) cur;

      return new Ast0.Id(temp.nm);
    }

    throw new Exception("Invalid Expression Type: " + cur.getClass().getTypeName());
  }

  // STATEMENTS

  static List<IR0.Inst> gen(Ast0.Stmt n) throws Exception {
    if (n instanceof Ast0.Assign)      return gen((Ast0.Assign) n);
    else if (n instanceof Ast0.If)     return gen((Ast0.If) n);
    else if (n instanceof Ast0.While)  return gen((Ast0.While) n);
    else if (n instanceof Ast0.Print)  return gen((Ast0.Print) n);
    throw new GenException("Unknown Ast0 Stmt: " + n);
  }

  // Ast0.Assign ---
  // Ast0.Exp lhs, rhs;
  //
  // AG:
  //   code: rhs.c + lhs.c + ( "lhs.l = rhs.v"     # if lhs is id
  //                         | "[lhs.l] = rhs.v" ) # otherwise

  static List<IR0.Inst> gen(Ast0.Assign n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    CodePack rhs = gen(n.rhs);
    code.addAll(rhs.code);
    if (n.lhs instanceof Ast0.Id) {
      IR0.Dest lhs = new IR0.Id(((Ast0.Id) n.lhs).nm);
      code.add(new IR0.Move(lhs, rhs.src));
    } else if (n.lhs instanceof Ast0.ArrayElm) {
      AddrPack p = genAddr((Ast0.ArrayElm) n.lhs);
      code.addAll(p.code);
      code.add(new IR0.Store(p.addr, rhs.src));
    } else {
      throw new GenException("Wrong Ast0 lhs Exp: " + n.lhs);
    }
    return code;
  }

  // Ast0.If ---
  // Ast0.Exp cond;
  // Ast0.Stmt s1, s2;
  //
  // AG:
  //   newLabel: L1[,L2]
  //   code: cond.c 
  //         + "if cond.v == false goto L1" 
  //         + s1.c 
  //         [+ "goto L2"] 
  //         + "L1:" 
  //         [+ s2.c]
  //         [+ "L2:"]

  static List<IR0.Inst> gen(Ast0.If n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    IR0.Label L1 = new IR0.Label();

    if (n.cond instanceof Ast0.BoolLit) {
      Ast0.BoolLit temp = (Ast0.BoolLit) n.cond;
      if (temp.b)
        code.addAll(gen(n.s1));
      else if (!temp.b && n.s2 != null)
        code.addAll(gen(n.s2));

      return code;
    }

    if (n.cond instanceof Ast0.Binop && isROP(((Ast0.Binop)n.cond).op)) {
      Ast0.BOP op = ((Ast0.Binop)n.cond).op;
      Ast0.Binop temp = (Ast0.Binop)n.cond;
      CodePack l = gen(temp.e1);
      CodePack r = gen(temp.e2);
      code.addAll(l.code);
      code.addAll(r.code);

      if (op == Ast0.BOP.LT)
        code.add(new IR0.CJump(IR0.ROP.GE, l.src, r.src, L1));
      else if (op == Ast0.BOP.LE)
        code.add(new IR0.CJump(IR0.ROP.GT, l.src, r.src, L1));
      else if (op == Ast0.BOP.GT)
        code.add(new IR0.CJump(IR0.ROP.LE, l.src, r.src, L1));
      else if (op == Ast0.BOP.GE)
        code.add(new IR0.CJump(IR0.ROP.LT, l.src, r.src, L1));
      else if (op == Ast0.BOP.EQ)
        code.add(new IR0.CJump(IR0.ROP.NE, l.src, r.src, L1));
      else if (op == Ast0.BOP.NE)
        code.add(new IR0.CJump(IR0.ROP.EQ, l.src, r.src, L1));

    } else {
      CodePack p = gen(n.cond);
      code.addAll(p.code);
      code.add(new IR0.CJump(IR0.ROP.EQ, p.src, IR0.FALSE, L1));
    }

    code.addAll(gen(n.s1));
    if (n.s2 == null) {
      code.add(new IR0.LabelDec(L1));
    } else {
      IR0.Label L2 = new IR0.Label();
      code.add(new IR0.Jump(L2));
      code.add(new IR0.LabelDec(L1));
      code.addAll(gen(n.s2));
      code.add(new IR0.LabelDec(L2));
    }
    return code;
  }

  // Ast0.While ---
  // Ast0.Exp cond;
  // Ast0.Stmt s;
  //
  // AG:
  //   newLabel: L1,L2
  //   code: "L1:" 
  //         + cond.c 
  //         + "if cond.v == false goto L2" 
  //         + s.c 
  //         + "goto L1" 
  //         + "L2:"

  static List<IR0.Inst> gen(Ast0.While n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    IR0.Label L1 = new IR0.Label();
    IR0.Label L2 = new IR0.Label();


    if (n.cond instanceof Ast0.BoolLit) {
      Ast0.BoolLit temp = (Ast0.BoolLit) n.cond;
      if (temp.b)
        code.addAll(gen(n.s));

      return code;
    }

    code.add(new IR0.LabelDec(L1));

    if (n.cond instanceof Ast0.Binop && isROP(((Ast0.Binop)n.cond).op)) {
      Ast0.BOP op = ((Ast0.Binop)n.cond).op;
      Ast0.Binop temp = (Ast0.Binop)n.cond;
      CodePack l = gen(temp.e1);
      CodePack r = gen(temp.e2);
      code.addAll(l.code);
      code.addAll(r.code);

      if (op == Ast0.BOP.LT)
        code.add(new IR0.CJump(IR0.ROP.GE, l.src, r.src, L2));
      else if (op == Ast0.BOP.LE)
        code.add(new IR0.CJump(IR0.ROP.GT, l.src, r.src, L2));
      else if (op == Ast0.BOP.GT)
        code.add(new IR0.CJump(IR0.ROP.LE, l.src, r.src, L2));
      else if (op == Ast0.BOP.GE)
        code.add(new IR0.CJump(IR0.ROP.LT, l.src, r.src, L2));
      else if (op == Ast0.BOP.EQ)
        code.add(new IR0.CJump(IR0.ROP.NE, l.src, r.src, L2));
      else if (op == Ast0.BOP.NE)
        code.add(new IR0.CJump(IR0.ROP.EQ, l.src, r.src, L2));
    } else {
      CodePack p = gen(n.cond);
      code.addAll(p.code);
      code.add(new IR0.CJump(IR0.ROP.EQ, p.src, IR0.FALSE, L2));
    }
    code.addAll(gen(n.s));
    code.add(new IR0.Jump(L1));
    code.add(new IR0.LabelDec(L2));
    return code;
  }
  
  // Ast0.Print ---
  // Ast0.PrArg [arg];
  //
  // AG:
  //   code: arg.c + "print (arg.v)"  if arg is Exp
  //     or  "print ()"               if arg==null

  static List<IR0.Inst> gen(Ast0.Print n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    if (n.arg == null) {
      code.add(new IR0.Print());
    } else {
      CodePack p = gen((Ast0.Exp) n.arg);
      code.addAll(p.code);
      code.add(new IR0.Print(p.src));
    }
    return code;
  }

  // EXPRESSIONS

  static CodePack gen(Ast0.Exp n) throws Exception {
    if (n instanceof Ast0.Binop)    return gen((Ast0.Binop) n);
    if (n instanceof Ast0.Unop)     return gen((Ast0.Unop) n);
    if (n instanceof Ast0.NewArray) return gen((Ast0.NewArray) n);
    if (n instanceof Ast0.ArrayElm) return gen((Ast0.ArrayElm) n);
    if (n instanceof Ast0.Id)	    return gen((Ast0.Id) n);
    if (n instanceof Ast0.IntLit)   return gen((Ast0.IntLit) n);
    if (n instanceof Ast0.BoolLit)  return gen((Ast0.BoolLit) n);
    throw new GenException("Unknown Exp node: " + n);
  }

  // Ast0.Binop

  static CodePack gen(Ast0.Binop n) throws Exception {
    if (isAOP(n.op))      return genAOP(n);
    else if (isROP(n.op)) return genROP(n);
    else                  return genLOP(n);
  }

  // Ast0.Binop --- arithmetic op case
  // Ast0.BOP op;
  // Ast0.Exp e1,e2;
  //
  // AG:
  //   newTemp: t
  //   code: e1.c + e2.c
  //         + "t = e1.v op e2.v"

  static CodePack genAOP(Ast0.Binop n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    CodePack l = gen(n.e1);
    CodePack r = gen(n.e2);
    IR0.Temp t = new IR0.Temp();
    code.addAll(l.code);
    code.addAll(r.code);
    code.add(new IR0.Binop(gen(n.op), t, l.src, r.src));
    return new CodePack(t, code);
  }

  // Ast0.Binop --- logical op case
  // Ast0.BOP op;
  // Ast0.Exp e1,e2;
  //
  // AG:
  //   newTemp: t
  //   newLabel: L
  //   let val=true if op==OR
  //   let val=false if op==AND
  //   code: "t = val"		       
  //         + e1.c 		     
  //         + "if e1.v==val goto L"     
  //         + e2.c			       
  //         + "if e2.v==val goto L"     
  //         + "t = !val"		       
  //         + "L:"                      

  static CodePack genLOP(Ast0.Binop n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    IR0.BoolLit val = (n.op==Ast0.BOP.OR) ? IR0.TRUE : IR0.FALSE;
    IR0.BoolLit nval = (n.op==Ast0.BOP.OR) ? IR0.FALSE : IR0.TRUE;
    IR0.Temp t = new IR0.Temp();
    IR0.Label L = new IR0.Label();
    code.add(new IR0.Move(t, val));
    CodePack l = gen(n.e1);
    code.addAll(l.code);
    code.add(new IR0.CJump(IR0.ROP.EQ, l.src, val, L));
    CodePack r = gen(n.e2);
    code.addAll(r.code);
    code.add(new IR0.CJump(IR0.ROP.EQ, r.src, val, L));
    code.add(new IR0.Move(t, nval));
    code.add(new IR0.LabelDec(L));
    return new CodePack(t, code);
  }

  // Ast0.Binop --- relational op case
  // Ast0.BOP op;
  // Ast0.Exp e1,e2;
  //
  // AG:
  //   newTemp: t
  //   newLabel: L
  //   code: e1.c + e2.c
  //         + "t = true"		       
  //         + "if e1.v op e2.v goto L"     
  //         + "t = false"		       
  //         + "L:"                      

  static CodePack genROP(Ast0.Binop n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    CodePack l = gen(n.e1);
    CodePack r = gen(n.e2);
    IR0.Temp t = new IR0.Temp();
    IR0.Label L = new IR0.Label();
    code.addAll(l.code);
    code.addAll(r.code);
    code.add(new IR0.Move(t, IR0.TRUE));
    code.add(new IR0.CJump((IR0.ROP)gen(n.op), l.src, r.src, L));
    code.add(new IR0.Move(t, IR0.FALSE));
    code.add(new IR0.LabelDec(L));
    return new CodePack(t, code);
  }

  // Ast0.Unop ---
  // Ast0.UOP op;
  // Ast0.Exp e;
  //
  // AG:
  //   newTemp: t
  //   code: e.c + "t = op e.v"

  static CodePack gen(Ast0.Unop n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    CodePack p = gen(n.e);
    code.addAll(p.code);
    IR0.UOP op = (n.op == Ast0.UOP.NEG) ? IR0.UOP.NEG : IR0.UOP.NOT;
    IR0.Temp t = new IR0.Temp();
    code.add(new IR0.Unop(op, t, p.src));
    return new CodePack(t, code);
  }
  
  // Ast0.NewArray ---
  // int len;
  // 
  // AG:
  //   newTemp: t
  //   code: "t = malloc (len * 4)"

  static CodePack gen(Ast0.NewArray n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    IR0.IntLit arg = new IR0.IntLit(n.len * 4);
    IR0.Temp t = new IR0.Temp();
    code.add(new IR0.Malloc(t, arg));
    return new CodePack(t, code);
  }

  // Ast0.ArrayElm ---
  // Ast0.Exp ar, idx;
  //
  // AG:
  //   newTemp: t1,t2,t3
  //   code: ar.c + idx.c
  //         + "t1 = idx.v * 4"
  //         + "t2 = ar.v + t1"
  //         + "t3 = [t2]"

  static CodePack gen(Ast0.ArrayElm n) throws Exception {
    AddrPack p = genAddr(n);
    IR0.Temp t = new IR0.Temp();
    p.code.add(new IR0.Load(t, p.addr));
    return new CodePack(t, p.code);
  }
  
  static AddrPack genAddr(Ast0.ArrayElm n) throws Exception {
    List<IR0.Inst> code = new ArrayList<IR0.Inst>();
    if (n.idx instanceof Ast0.IntLit) {
      CodePack ar = gen(n.ar);
      code.addAll(ar.code);

      return new AddrPack(new IR0.Addr(ar.src, ((Ast0.IntLit)n.idx).i * 4), code);
    }
    CodePack ar = gen(n.ar);
    CodePack idx = gen(n.idx);
    code.addAll(ar.code);
    code.addAll(idx.code);
    IR0.Temp t1 = new IR0.Temp();
    IR0.Temp t2 = new IR0.Temp();
    IR0.IntLit intSz = new IR0.IntLit(4);
    code.add(new IR0.Binop(IR0.AOP.MUL, t1, idx.src, intSz));
    code.add(new IR0.Binop(IR0.AOP.ADD, t2, ar.src, t1));
    return new AddrPack(new IR0.Addr(t2, 0), code);
  }

  // Ast0.Id ---
  // String nm;

  static CodePack gen(Ast0.Id n) throws Exception {
    return new CodePack(new IR0.Id(n.nm));
  }

  // Ast0.IntLit ---
  // int i;

  static CodePack gen(Ast0.IntLit n) throws Exception {
    return  new CodePack(new IR0.IntLit(n.i));
  }

  // Ast0.BoolLit ---
  // boolean b;

  static CodePack gen(Ast0.BoolLit n) throws Exception {
    return  new CodePack(n.b ? IR0.TRUE : IR0.FALSE);
  }

  // OPERATORS

  static IR0.BOP gen(Ast0.BOP op) {
    IR0.BOP irOp = null;
    switch (op) {
    case ADD: irOp = IR0.AOP.ADD; break;
    case SUB: irOp = IR0.AOP.SUB; break;
    case MUL: irOp = IR0.AOP.MUL; break;
    case DIV: irOp = IR0.AOP.DIV; break;
    case AND: irOp = IR0.AOP.AND; break;
    case OR:  irOp = IR0.AOP.OR;  break;
    case EQ:  irOp = IR0.ROP.EQ;  break;
    case NE:  irOp = IR0.ROP.NE;  break;
    case LT:  irOp = IR0.ROP.LT;  break;
    case LE:  irOp = IR0.ROP.LE;  break;
    case GT:  irOp = IR0.ROP.GT;  break;
    case GE:  irOp = IR0.ROP.GE;  break;
    }
    return irOp;
  }
   
  static boolean isAOP(Ast0.BOP op) {
    switch (op) {
    case ADD:  return true; 
    case SUB:  return true; 
    case MUL:  return true; 
    case DIV:  return true; 
    }
    return false;
  }
   
  static boolean isROP(Ast0.BOP op) {
    switch (op) {
    case EQ:  return true; 
    case NE:  return true; 
    case LT:  return true; 
    case LE:  return true; 
    case GT:  return true; 
    case GE:  return true; 
    }
    return false;
  }
   
}
