// 
// A starting version of IR1 interpreter. (For CS322 W15 Assignment 1)
//
//
import java.util.*;
import java.io.*;

import com.sun.org.apache.xpath.internal.operations.Bool;
import ir1.*;

public class IR1Interp {



  static class IntException extends Exception {
    public IntException(String msg) { super(msg); }
  }

  //-----------------------------------------------------------------
  // Value representation
  //-----------------------------------------------------------------
  //
  abstract static class Val {}

  // Integer values
  //
  static class IntVal extends Val {
    int i;
    IntVal(int i) { this.i = i; }
    public String toString() { return "" + i; }
  }

  // Boolean values
  //
  static class BoolVal extends Val {
    boolean b;
    BoolVal(boolean b) { this.b = b; }
    public String toString() { return "" + b; }
  }

  // String values
  //
  static class StrVal extends Val {
    String s;
    StrVal(String s) { this.s = s; }
    public String toString() { return s; }
  }

  // A special "undefined" value
  //
  static class UndVal extends Val {
    public String toString() { return "UndVal"; }
  }

  //-----------------------------------------------------------------
  // Environment representation
  //-----------------------------------------------------------------
  //
  // Think of how to organize environments.
  // 
  // The following environments are shown in the lecture for use in 
  // an IR0 interpreter:
  //
  //   HashMap<String,Integer> labelMap;  // label table
  //   HashMap<Integer,Val> tempMap;	  // temp table
  //   HashMap<String,Val> varMap;	  // var table
  // 
  // For IR1, they need to be managed at per function level.
  // 

  static class EnvMgr {
    public static class FuncEnv {
      public HashMap<String,Integer> labelMap;  // label table
      public HashMap<Integer,Val> tempMap;	  // temp table
      public HashMap<String,Val> varMap;	  // var table

      FuncEnv() {
        labelMap = new HashMap<String, Integer>();
        tempMap = new HashMap<Integer, Val>();
        varMap = new HashMap<String, Val>();
      }
    }

    private static HashMap<String, FuncEnv> envDict;
    public static String currentEnv;


    static {
      envDict = new HashMap<String, FuncEnv>();
      currentEnv = "main";
    }

    /**
     * This method will automatically create an env
     * if it doesn't already exhist.
     * @param funcName The name of the function to associate the env with
     * @return The env
     */
    public static FuncEnv get(String funcName) {
      if (!envDict.containsKey(funcName)) {
        envDict.put(funcName, new FuncEnv());
      }

      return envDict.get(funcName);
    }

    /**
     * Retrives the funcEnv at the currentEnv
     * @return the env
     */
    public static FuncEnv get() {
      return get(currentEnv);
    }


  }

  //-----------------------------------------------------------------
  // Global variables and constants
  //-----------------------------------------------------------------
  //
  // These variables and constants are for your reference only.
  // You may decide to use all of them, some of these, or not at all.
  //

  // Function lookup table
  // - maps function names to their AST nodes

  static HashMap<String, IR1.Func> funcMap; 	

  // Heap memory
  // - for handling 'malloc'ed data
  // - you need to define alloc and access methods for it

  static ArrayList<Val> heap;		

  // Return value
  // - for passing return value from callee to caller

  static Val retVal;

  // Execution status
  // - tells whether to continue with the nest inst, to jump to
  //   a new target inst, or to return to the caller

  static final int CONTINUE = 0;
  static final int RETURN = -1;	

  //-----------------------------------------------------------------
  // The main method
  //-----------------------------------------------------------------
  //
  // 1. Open an IR1 program file. 
  // 2. Call the IR1 AST parser to read in the program and 
  //    convert it to an AST (rooted at an IR1.Program node).
  // 3. Invoke the interpretation process on the root node.

  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
      //String workingDir = System.getProperty("user.dir");
      //String filePath = "src/tst/test09.ir";
      //System.out.println("WorkingDir: \"" + workingDir + "\"");
      //System.out.println("FilePath: \"" + filePath + "\"");
      //System.out.println("Change path in code");

      FileInputStream stream = new FileInputStream(args[0]);
      IR1.Program p = new ir1Parser(stream).Program();
      stream.close();

      IR1Interp.execute(p);
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  //-----------------------------------------------------------------
  // Top-level IR nodes
  //-----------------------------------------------------------------
  //

  // Program ---
  //  Func[] funcs;
  //
  // 1. Establish the function lookup map
  // 2. Lookup 'main' in funcMap, and 
  // 3. start interpreting from main's AST node

  public static void execute(IR1.Program n) throws Exception { 
    funcMap = new HashMap<String,IR1.Func>();
    heap = new ArrayList<Val>();
    //heap.
    //storage = new ArrayList<Val>();
    retVal = null;//Val.Undefined;
    for (IR1.Func f: n.funcs)
      funcMap.put(f.name, f);
    execute(funcMap.get("main"));
  }

  // Func ---
  //  String name;
  //  Var[] params;
  //  Var[] locals;
  //  Inst[] code;
  //
  // 1. Collect label decls information and store them in
  //    a label-lookup table for later use.
  // 2. Execute the fetch-and-execute loop.

  static void execute(IR1.Func n) throws Exception {

    EnvMgr.currentEnv = n.name;
    EnvMgr.FuncEnv mainEnv = EnvMgr.get();

    for (int i = 0;i < n.code.length; i++) {
      IR1.Inst tempInst = n.code[i];
      if (tempInst instanceof IR1.LabelDec) {

        mainEnv.labelMap.put(((IR1.LabelDec) tempInst).name, i);
      }
    }


    // The fetch-and-execute loop
    int idx = 0;
    while (idx < n.code.length) {
      int next = execute(n.code[idx]);
      if (next == CONTINUE)
	idx++; 
      else if (next == RETURN)
        break;
      else
	idx = next;
    }
  }

  // Dispatch execution to an individual Inst node.

  static int execute(IR1.Inst n) throws Exception {
    if (n instanceof IR1.Binop)    return execute((IR1.Binop) n);
    if (n instanceof IR1.Unop) 	   return execute((IR1.Unop) n);
    if (n instanceof IR1.Move) 	   return execute((IR1.Move) n);
    if (n instanceof IR1.Load) 	   return execute((IR1.Load) n);
    if (n instanceof IR1.Store)    return execute((IR1.Store) n);
    if (n instanceof IR1.Jump) 	   return execute((IR1.Jump) n);
    if (n instanceof IR1.CJump)    return execute((IR1.CJump) n);
    if (n instanceof IR1.Call)     return execute((IR1.Call) n);
    if (n instanceof IR1.Return)   return execute((IR1.Return) n);
    if (n instanceof IR1.LabelDec) return CONTINUE;
    throw new IntException("Unknown Inst: " + n);
  }

  //-----------------------------------------------------------------
  // Execution routines for individual Inst nodes
  //-----------------------------------------------------------------
  //
  // - Each execute() routine returns CONTINUE, RETURN, or a new idx 
  //   (target of jump).
  //

  // Binop ---
  //  BOP op;
  //  Dest dst;
  //  Src src1, src2;

  static int execute(IR1.Binop n) throws Exception {
    Val val1 = evaluate(n.src1);
    Val val2 = evaluate(n.src2);

    Val res = binOpEval(n.op, val1, val2);
    //Dest would normaly be used  (as far As I understand right now)
    //To check to see if the variable we're writing to actually exhists.
    //However since the hashmaps will create a key/value pari if it doesn't
    //exhist, and happly write to it reguardless, this is totaly unnessesary.
    setVarOrTemp(n.dst, res);

    return CONTINUE;
  }

  // Unop ---
  //  UOP op;
  //  Dest dst;
  //  Src src;

  static int execute(IR1.Unop n) throws Exception {
    Val val = evaluate(n.src);
    Val res;
    if (n.op == IR1.UOP.NEG)
      res = new IntVal(-((IntVal) val).i);
    else if (n.op == IR1.UOP.NOT)
      res = new BoolVal(!((BoolVal) val).b);
    else
      throw new IntException("Wrong op in Unop inst: " + n.op);

    setVarOrTemp(n.dst, res);

    return CONTINUE;  
  }

  static Val binOpEval(IR1.BOP n, Val val1, Val val2) throws Exception {
    //Val dest = evaluate(n.dst);
    Val res;

    int v1 = -1;
    int v2 = -1;

    if (val1 instanceof IntVal)
      v1 = ((IntVal) val1).i;
    else if (val1 instanceof BoolVal)
      v1 = ((BoolVal) val1).b ? 1 : 0;

    if (val2 instanceof IntVal)
      v2 = ((IntVal) val2).i;
    else if (val1 instanceof BoolVal)
      v2 = ((BoolVal) val2).b ? 1 : 0;

    if (n == IR1.AOP.ADD)
      res = new IntVal(v1 + v2);
    else if (n == IR1.AOP.AND) {
      boolean b1 = ((BoolVal) val1).b;
      boolean b2 = ((BoolVal) val2).b;

      res = new BoolVal(b1 && b2);
    } else if (n == IR1.AOP.DIV)
      res = new IntVal(v1 / v2);
    else if (n == IR1.AOP.MUL)
      res = new IntVal(v1 * v2);
    else if (n == IR1.AOP.OR) {
      boolean b1 = ((BoolVal) val1).b;
      boolean b2 = ((BoolVal) val2).b;

      res = new BoolVal(b1 || b2);
    } else if (n == IR1.AOP.SUB)
      res = new IntVal(v1 - v2);
    else if (n == IR1.ROP.EQ)
      res = new BoolVal(v1 == v2);
    else if (n == IR1.ROP.GE)
      res = new BoolVal(v1 >= v2);
    else if (n == IR1.ROP.GT)
      res = new BoolVal(v1 > v2);
    else if (n == IR1.ROP.LE)
      res = new BoolVal(v1 <= v2);
    else if (n == IR1.ROP.LT)
      res = new BoolVal(v1 < v2);
    else if (n == IR1.ROP.NE)
      res = new BoolVal(v1 != v2);
    else
      throw new Exception("Wrong BinOp Type");

    return res;
  }

  static void setVarOrTemp(IR1.Dest dst, Val res) throws  Exception {
    if (dst instanceof IR1.Id) {
      IR1.Id temp = (IR1.Id) dst;
      EnvMgr.get().varMap.put(temp.name, res);
    } else if (dst instanceof  IR1.Temp) {
      IR1.Temp temp = (IR1.Temp) dst;
      EnvMgr.get().tempMap.put(temp.num, res);
    } else {
      if (dst == null) return;
      throw new Exception("Wrong Val Type in UnOp");
    }
  }

  // Move ---
  //  Dest dst;
  //  Src src;

  static int execute(IR1.Move n) throws Exception {
    Val srs = evaluate(n.src);


    setVarOrTemp(n.dst, srs);

    return CONTINUE;

  }

  // Load ---  
  //  Dest dst;
  //  Addr addr;

  static int execute(IR1.Load n) throws Exception {
    int memLoc = evalute(n.addr);

    setVarOrTemp(n.dst, heap.get(memLoc));

    return CONTINUE;
  }

  // Store ---  
  //  Addr addr;
  //  Src src;

  static int execute(IR1.Store n) throws Exception {
    int memLoc = evalute(n.addr);

    heap.set(memLoc, evaluate(n.src));

    return CONTINUE;
  }

  // CJump ---
  //  ROP op;
  //  Src src1, src2;
  //  Label lab;

  static int execute(IR1.CJump n) throws Exception {
    Val val1 = evaluate(n.src1);
    Val val2 = evaluate(n.src2);

    //TODO: What do if bad types?
    BoolVal res = (BoolVal)binOpEval(n.op, val1, val2);

    if (res.b)
      return EnvMgr.get().labelMap.get(n.lab.name);
    else
      return CONTINUE;
  }	

  // Jump ---
  //  Label lab;

  static int execute(IR1.Jump n) throws Exception {
    return EnvMgr.get().labelMap.get(n.lab.name);
  }

  // Call ---
  //  String name;
  //  Src[] args;
  //  Dest rdst;

  static int execute(IR1.Call n) throws Exception {
    if (n.name.equals( "printStr") || n.name.equals("printBool") || n.name.equals("printInt")) {
      print(n);
    }else if (n.name.equals("malloc")) {

      malloc(n);
      setVarOrTemp(n.rdst, retVal);
    } else {
      IR1.Func fun = funcMap.get(n.name);

      EnvMgr.FuncEnv newEnv = EnvMgr.get(fun.name);
      for (int i = 0; i < n.args.length; i++) {
        Val temp = evaluate(n.args[i]);

        newEnv.varMap.put(fun.params[i], temp);
      }

      execute(fun);

      setVarOrTemp(n.rdst, retVal);
    }
    return CONTINUE;
  }

  static void print(IR1.Call n) throws Exception {
    for (int i = 0; i < n.args.length; i++) {
      Val temp = evaluate(n.args[i]);

      if (temp instanceof IntVal)    System.out.print(((IntVal)temp).i);
      if (temp instanceof BoolVal)    System.out.print(((BoolVal)temp).b);
      if (temp instanceof StrVal)    System.out.print(((StrVal)temp).s);

      if (i != n.args.length - 1)
        System.out.print(" ");
    }
    System.out.println();
  }

  static void malloc(IR1.Call n) throws Exception{
    Val arg = evaluate(n.args[0]);
    int size = ((IntVal)arg).i;

    int curSize = heap.size();

    for (int i = 0; i < size; i++)
      heap.add(curSize + i, null);

    retVal = new IntVal(curSize);
  }

  // Return ---  
  //  Src val;

  static int execute(IR1.Return n) throws Exception {
    if (n.val == null)
      retVal = new UndVal();
    else
      retVal = evaluate(n.val);

    return RETURN;
  }

  //-----------------------------------------------------------------
  // Evaluatation routines for address
  //-----------------------------------------------------------------
  //
  // - Returns an integer (representing index to the heap memory).
  //
  // Address ---
  //  Src base;  
  //  int offset;

  static int evalute(IR1.Addr n) throws Exception {
    int base = ((IntVal)evaluate(n.base)).i;

    return base + n.offset;
  }

  //-----------------------------------------------------------------
  // Evaluatation routines for operands
  //-----------------------------------------------------------------
  //
  // - Each evaluate() routine returns a Val object.

  static Val evaluate(IR1.Src n) throws Exception {
    Val val = null;
    if (n instanceof IR1.Temp)    val = EnvMgr.get().tempMap.get(((IR1.Temp)n).num);
    if (n instanceof IR1.Id)      val = EnvMgr.get().varMap.get(((IR1.Id)n).name);
    if (n instanceof IR1.IntLit)  val = new IntVal(((IR1.IntLit)n).i);
    if (n instanceof IR1.BoolLit) val = new BoolVal(((IR1.BoolLit)n).b);
    if (n instanceof IR1.StrLit)  val = new StrVal(((IR1.StrLit)n).s);
    return val;
  }

  static Val evaluate(IR1.Dest n) throws Exception {
    Val val = null;
     if (n instanceof IR1.Temp) val = EnvMgr.get().tempMap.get(((IR1.Temp)n).num);
     if (n instanceof IR1.Id)   val = EnvMgr.get().varMap.get(((IR1.Id)n).name);
    return val;
  }

}
