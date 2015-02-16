// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
// 
// IR code generator for miniJava's AST.
//
// (Starter version.)
//

import ast.Ast;
import ast.astParser;
import com.sun.org.apache.bcel.internal.classfile.Code;
import ir.IR;

import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class IRGen {

    static class GenException extends Exception {
        public GenException(String msg) {
            super(msg);
        }
    }

    //------------------------------------------------------------------------------
    // ClassInfo
    //----------
    // For keeping all useful information about a class declaration for use
    // in the codegen.

    static class ClassInfo {
        Ast.ClassDecl cdecl;    // classDecl AST
        ClassInfo parent;        // pointer to parent
        List<String> vtable;    // method-label table
        List<Ast.VarDecl> fdecls;   // field decls (incl. inherited ones)
        List<Integer> offsets;      // field offsets
        int objSize;        // object size

        // Constructor -- clone a parent's record
        //
        ClassInfo(Ast.ClassDecl cdecl, ClassInfo parent) {
            this.cdecl = cdecl;
            this.parent = parent;
            this.vtable = new ArrayList<String>(parent.vtable);
            this.fdecls = new ArrayList<Ast.VarDecl>(parent.fdecls);
            this.offsets = new ArrayList<Integer>(parent.offsets);
            this.objSize = parent.objSize;
        }

        // Constructor -- create a new record
        //
        ClassInfo(Ast.ClassDecl cdecl) {
            this.cdecl = cdecl;
            this.parent = null;
            this.vtable = new ArrayList<String>();
            this.fdecls = new ArrayList<Ast.VarDecl>();
            this.offsets = new ArrayList<Integer>();
            this.objSize = IR.Type.PTR.size;    // reserve space for ptr to class
        }

        // Utility Routines
        // ----------------
        // For accessing information stored in class information record
        //

        // Return the name of this class
        //
        String className() {
            return cdecl.nm;
        }

        // Find method's base class record
        //
        ClassInfo methodBaseClass(String mname) throws Exception {
            for (Ast.MethodDecl mdecl : cdecl.mthds)
                if (mdecl.nm.equals(mname))
                    return this;
            if (parent != null)
                return parent.methodBaseClass(mname);
            throw new GenException("Can't find base class for method " + mname);
        }

        // Find method's return type
        //
        Ast.Type methodType(String mname) throws Exception {
            for (Ast.MethodDecl mdecl : cdecl.mthds)
                if (mdecl.nm.equals(mname))
                    return mdecl.t;
            if (parent != null)
                return parent.methodType(mname);
            throw new GenException("Can't find MethodDecl for method " + mname);
        }

        // Return method's vtable offset
        //
        int methodOffset(String mname) {
            return vtable.indexOf(mname) * IR.Type.PTR.size;
        }

        // Find field variable's type
        //
        Ast.Type fieldType(String fname) throws Exception {
            for (Ast.VarDecl fdecl : cdecl.flds) {
                if (fdecl.nm.equals(fname))
                    return fdecl.t;
            }
            if (parent != null)
                return parent.fieldType(fname);
            throw new GenException("Can't find VarDecl for field " + fname);
        }

        // Return field variable's offset
        //
        int fieldOffset(String fname) throws Exception {
            for (int i = fdecls.size() - 1; i >= 0; i--) {
                if (fdecls.get(i).nm.equals(fname))
                    return offsets.get(i);
            }
            throw new GenException("Can't find offset for field " + fname);
        }

        public String toString() {
            return "ClassInfo: " + " " + cdecl + " " + parent + " "
                    + " " + vtable + " " + offsets + " " + objSize;
        }
    }

    //------------------------------------------------------------------------------
    // Other Supporting Data Structures
    //---------------------------------

    // CodePack
    // --------
    // For returning <type,src,code> tuple from gen() routines
    //
    static class CodePack {
        IR.Type type;
        IR.Src src;
        List<IR.Inst> code;

        CodePack(IR.Type type, IR.Src src, List<IR.Inst> code) {
            this.type = type;
            this.src = src;
            this.code = code;
        }

        CodePack(IR.Type type, IR.Src src) {
            this.type = type;
            this.src = src;
            code = new ArrayList<IR.Inst>();
        }
    }

    // AddrPack
    // --------
    // For returning <type,addr,code> tuple from genAddr routines

    static class AddrPack {
        IR.Type type;
        IR.Addr addr;
        List<IR.Inst> code;

        AddrPack(IR.Type type, IR.Addr addr, List<IR.Inst> code) {
            this.type = type;
            this.addr = addr;
            this.code = code;
        }
    }

    // Env
    // ---
    // For keeping track of local variables and parameters and for finding
    // their types.

    private static class Env extends HashMap<String, Ast.Type> {
    }


    //------------------------------------------------------------------------------
    // Global Variables
    // ----------------
    //

    // Env for ClassInfo records
    private static HashMap<String, ClassInfo> classEnv = new HashMap<String, ClassInfo>();

    // IR code representation of the current object
    private static IR.Src thisObj = new IR.Id("obj");


    //------------------------------------------------------------------------------
    // Utility routines
    // ----------------
    //

    // Sort ClassDecls based on parent-children relationship.

    private static Ast.ClassDecl[] topoSort(Ast.ClassDecl[] classes) {
        List<Ast.ClassDecl> cl = new ArrayList<Ast.ClassDecl>();
        Vector<String> done = new Vector<String>();
        int cnt = classes.length;
        while (cnt > 0) {
            for (Ast.ClassDecl cd : classes)
                if (!done.contains(cd.nm)
                        && ((cd.pnm == null) || done.contains(cd.pnm))) {
                    cl.add(cd);
                    done.add(cd.nm);
                    cnt--;
                }
        }
        return cl.toArray(new Ast.ClassDecl[0]);
    }

    // Return an object's base classInfo.
    //  (The parameter n is known to represent an object when call
    //  is made.)

    private static ClassInfo getClassInfo(Ast.Exp n, ClassInfo cinfo, Env env) throws Exception {
        Ast.Type typ = null;
        if (n instanceof Ast.This)
            return cinfo;
        if (n instanceof Ast.Id) {
            typ = env.get(((Ast.Id) n).nm);
            if (typ == null) // id is a field with a missing "this" pointer
                typ = cinfo.fieldType(((Ast.Id) n).nm);
        } else if (n instanceof Ast.Field) {
            ClassInfo base = getClassInfo(((Ast.Field) n).obj, cinfo, env);
            typ = base.fieldType(((Ast.Field) n).nm);
        } else {
            throw new GenException("Unexpected obj epxression " + n);
        }
        if (!(typ instanceof Ast.ObjType))
            throw new GenException("Expects an ObjType, got " + typ);
        return classEnv.get(((Ast.ObjType) typ).nm);
    }

    // Create ClassInfo record
    //
    // Codegen Guideline:
    // 1. If parent exists, clone parent's record; otherwise create a new one
    // 2. Walk the MethodDecl list. If a method is not in the v-table, add it in;
    // 3. Compute offset values for field variables
    // 4. Decide object's size

    private static ClassInfo createClassInfo(Ast.ClassDecl n) throws Exception {
        ClassInfo cinfo = (n.pnm != null) ? new ClassInfo(n, classEnv.get(n.pnm)) : new ClassInfo(n);

        for (Ast.MethodDecl decl: n.mthds) {
            if (!cinfo.vtable.contains(decl.nm))
                cinfo.vtable.add(decl.nm);

        }

        //#yolo
        int j = cinfo.offsets.size() - 1;
        if (j < 0) {
            cinfo.offsets.add(8);
            j = 0;
        } else
            cinfo.offsets.add(cinfo.offsets.get(j) + 8);

        for (Ast.MethodDecl decl: n.mthds) {
            if (!decl.nm.equals("main")) {
                int prevSum = cinfo.offsets.get(j);
                cinfo.offsets.add(prevSum + 8);
                j++;
            }
        }

        for (Ast.VarDecl decl: n.flds) {
            int prevSum = cinfo.offsets.get(j++);

            if (decl.t instanceof Ast.IntType)
                cinfo.offsets.add(prevSum + 4);
            else if (decl.t instanceof Ast.BoolType)
                cinfo.offsets.add(prevSum + 1);
            else if (decl.t instanceof Ast.ArrayType || decl.t instanceof Ast.ObjType)
                cinfo.offsets.add(prevSum + 8);
        }

        cinfo.objSize = 0;
        for (int i : cinfo.offsets)
            cinfo.objSize += i;


        return cinfo;
    }


    //------------------------------------------------------------------------------
    // The Main Routine
    //-----------------
    //
    public static void main(String[] args) throws Exception {
        //if (args.length == 1) {
        String path = "src/tst/test07.ast";
        FileInputStream stream = new FileInputStream(path);//args[0]);
        Ast.Program p = new astParser(stream).Program();
        stream.close();
        IR.Program ir = gen(p);
        System.out.print(ir.toString());
        //} else {
        //  System.out.println("You must provide an input file name.");
        //}
    }

    //------------------------------------------------------------------------------
    // Codegen Routines for Individual AST Nodes
    //------------------------------------------

    // Program ---
    // ClassDecl[] classes;
    //
    // Three passes over a program:
    //  0. topo-sort class decls
    //  1. create ClassInfo records
    //  2. generate IR code
    //     2.1 generate list of static data (i.e. class descriptors)
    //     2.2 generate list of functions

    public static IR.Program gen(Ast.Program n) throws Exception {
        Ast.ClassDecl[] classes = topoSort(n.classes);
        ClassInfo cinfo;
        for (Ast.ClassDecl c : classes) {
            cinfo = createClassInfo(c);
            classEnv.put(c.nm, cinfo);
        }
        List<IR.Data> allData = new ArrayList<IR.Data>();
        List<IR.Func> allFuncs = new ArrayList<IR.Func>();
        for (Ast.ClassDecl c : classes) {
            cinfo = classEnv.get(c.nm);
            IR.Data data = genData(c, cinfo);
            List<IR.Func> funcs = gen(c, cinfo);
            if (data != null)
                allData.add(data);
            allFuncs.addAll(funcs);
        }
        return new IR.Program(allData, allFuncs);
    }

    // ClassDecl ---
    // String nm, pnm;
    // VarDecl[] flds;
    // MethodDecl[] mthds;
    //

    // 1. Generate static data
    //
    // Codegen Guideline:
    //   1.1 For each method in class's vtable, construct a global label of form
    //       "<base class name>_<method name>" and save it in an IR.Global node
    //   1.2 Assemble the list of IR.Global nodes into an IR.Data node with a
    //       global label "class_<class name>"

    static IR.Data genData(Ast.ClassDecl n, ClassInfo cinfo) throws Exception {
        ArrayList<IR.Global> temp = new ArrayList<IR.Global>();

        for (String s : cinfo.vtable)
            temp.add(new IR.Global((s.equals("main") ? "" : n.nm + "_") + s));

        return new IR.Data(new IR.Global("class_" + n.nm), cinfo.objSize, temp.toArray(new IR.Global[temp.size()]));
    }

    // 2. Generate code
    //
    // Codegen Guideline:
    //   Straightforward -- generate a IR.Func for each mthdDecl.

    static List<IR.Func> gen(Ast.ClassDecl n, ClassInfo cinfo) throws Exception {
        ArrayList<IR.Func> funList = new ArrayList<IR.Func>();

        for (Ast.MethodDecl m : n.mthds)
           funList.add(gen(m, cinfo));

        return funList;
    }

    // MethodDecl ---
    // Type t;
    // String nm;
    // Param[] params;
    // VarDecl[] vars;
    // Stmt[] stmts;
    //
    // Codegen Guideline:
    // 1. Construct a global label of form "<base class name>_<method name>"
    // 2. Add "obj" into the params list as the 0th item
    // (Skip these two steps if method is "main".)
    // 3. Create an Env() containing all params and all local vars
    // 4. Generate IR code for all statements
    // 5. Return an IR.Func with the above

    static IR.Func gen(Ast.MethodDecl n, ClassInfo cinfo) throws Exception {
        String name = "";
        Env funcEnv = new Env();
        ArrayList<IR.Inst> listInst = new ArrayList<IR.Inst>();
        ArrayList<String> params = new ArrayList<String>();
        ArrayList<String> locals = new ArrayList<String>();


        if (!n.nm.equals("main")) {
            name = cinfo.cdecl.nm + "_" + n.nm;
            params.add("obj");
        } else
            name = n.nm;

        for (Ast.Param p : n.params) {
            funcEnv.put(p.nm, p.t);
            params.add(p.nm);
        }

        for (Ast.VarDecl v : n.vars) {
            funcEnv.put(v.nm, v.t);
            locals.add(v.nm);
            List<IR.Inst> test = gen(v, cinfo, funcEnv);
            if (test != null)
                listInst.addAll(test);
        }

        for (Ast.Stmt s : n.stmts)
            listInst.addAll(gen(s, cinfo, funcEnv));

        return new IR.Func(name, params, locals, listInst);
    }

    // VarDecl ---
    // Type t;
    // String nm;
    // Exp init;
    //
    // Codegen Guideline:
    // 1. If init exp exists, generate IR code for it and assign result to var
    // 2. Return generated code (or null if none)

    private static List<IR.Inst> gen(Ast.VarDecl n, ClassInfo cinfo, Env env) throws Exception {
        if (n.init == null) return null;
        List<IR.Inst> ret = new ArrayList<IR.Inst>();

        CodePack temp = gen(n.init, cinfo, env);
        //Ast.Assign ass = new Ast.Assign()
        IR.Move test = new IR.Move(new IR.Id(n.nm), temp.src);
        ret.add(test);
        return ret;
    }

    // STATEMENTS

    // Dispatch a generic call to a specific Stmt routine

    static List<IR.Inst> gen(Ast.Stmt n, ClassInfo cinfo, Env env) throws Exception {
        if (n instanceof Ast.Block) return gen((Ast.Block) n, cinfo, env);
        if (n instanceof Ast.Assign) return gen((Ast.Assign) n, cinfo, env);
        if (n instanceof Ast.CallStmt) return gen((Ast.CallStmt) n, cinfo, env);
        if (n instanceof Ast.If) return gen((Ast.If) n, cinfo, env);
        if (n instanceof Ast.While) return gen((Ast.While) n, cinfo, env);
        if (n instanceof Ast.Print) return gen((Ast.Print) n, cinfo, env);
        if (n instanceof Ast.Return) return gen((Ast.Return) n, cinfo, env);
        throw new GenException("Illegal Ast Stmt: " + n);
    }

    // Block ---
    // Stmt[] stmts;

    static List<IR.Inst> gen(Ast.Block n, ClassInfo cinfo, Env env) throws Exception {
        ArrayList<IR.Inst> ins = new ArrayList<IR.Inst>();

        //#yolo
        for (Ast.Stmt s: n.stmts)
            ins.addAll(gen(s, cinfo, env));

        return ins;
    }

    // Assign ---
    // Exp lhs, rhs;
    //
    // Codegen Guideline:
    // 1. call gen() on rhs
    // 2. if lhs is ID, check against Env to see if it's a local var or a param;
    //    if yes, generate an IR.Move instruction
    // 3. otherwise, call genAddr() on lhs, and generate an IR.Store instruction

    static List<IR.Inst> gen(Ast.Assign n, ClassInfo cinfo, Env env) throws Exception {
        ArrayList<IR.Inst> ret = new ArrayList<IR.Inst>();


        if (n.lhs instanceof Ast.Id) {
            Ast.Id temp = (Ast.Id) n.lhs;

            if (env.containsKey(temp.nm)) {
                IR.Move mv = new IR.Move(new IR.Id(temp.nm), gen(n.rhs, cinfo, env).src);
                ret.add(mv);
            } else {
                AddrPack lhs = genAddr(n.lhs, cinfo, env);

                IR.Store st = new IR.Store(lhs.type, lhs.addr, gen(n.rhs, cinfo, env).src);
                ret.add(st);
            }
        } else if (n.lhs instanceof Ast.ArrayElm)
            throw new Exception("Fix genAssign");
            //lhs = gen(n.lhs, cinfo, env).code;

        return ret;
    }

    // CallStmt ---
    // Exp obj;
    // String nm;
    // Exp[] args;
    //

    static List<IR.Inst> gen(Ast.CallStmt n, ClassInfo cinfo, Env env) throws Exception {
        if (n.obj != null) {
            CodePack p = handleCall(n.obj, n.nm, n.args, cinfo, env, false);
            return p.code;
        }
        throw new GenException("In CallStmt, obj is null " + n);
    }

    // handleCall
    // ----------
    // Common routine for Call and CallStmt nodes
    //
    // Codegen Guideline:
    // 1. Invoke gen() on obj, which returns obj's storage address (and type and code)
    // 2. Call getClassInfo() on obj to get base ClassInfo
    // 3. Access the base class's ClassInfo rec to get the method's offset in vtable
    // 4. Add obj's as the 0th argument to the args list
    // 5. Generate an IR.Load to get the class descriptor from obj's storage
    // 6. Generate another IR.Load to get the method's global label
    // 7. If retFlag is set, prepare a temp for receiving return value; also figure
    //    out return value's type (through method's decl in ClassInfo rec)
    // 8. Generate an indirect call with the global label

    static CodePack handleCall(Ast.Exp obj, String name, Ast.Exp[] args, ClassInfo cinfo, Env env, boolean retFlag) throws Exception {
        CodePack opack = gen(obj, cinfo, env);
        ClassInfo baseInfo = getClassInfo(obj, cinfo, env);
        int offset = baseInfo.offsets.get(baseInfo.vtable.indexOf(name));

        Ast.Exp [] args2 = new Ast.Exp[args.length + 1];
        args2[0] = obj;
        for (int i = 0; i < args.length; i++)
            args2[i+1] = args[i];


        throw new Exception("Not Done: handleCall [" + obj.getClass().toString() + "]");
        //IR.Load odesc = new IR.Load()

        //return null;
    }

    // If ---
    // Exp cond;
    // Stmt s1, s2;
    //
    // (See class notes.)

    static List<IR.Inst> gen(Ast.If n, ClassInfo cinfo, Env env) throws Exception {
        List<IR.Inst> ret = new ArrayList<IR.Inst>();
        List<IR.Inst> trueStmt = gen(n.s1, cinfo, env);
        List<IR.Inst> falseStmt = gen(n.s2, cinfo, env);
       // n.s1

        IR.LabelDec l0 = new IR.LabelDec(new IR.Label());
        IR.LabelDec l1 = new IR.LabelDec(new IR.Label());

        trueStmt.add(new IR.Jump(l1.lab));


        /*if (n.cond instanceof Ast.Binop) {
            IR.ROP newOp;
            Ast.Binop op = (Ast.Binop) n.cond;
            CodePack s1 = gen(op.e1, cinfo, env);
            CodePack s2 = gen(op.e2, cinfo, env);

            if (op.op == Ast.BOP.GE)
                newOp = IR.ROP.GE;
            else if (op.op == Ast.BOP.GT)
                newOp = IR.ROP.GT;
            else if (op.op == Ast.BOP.LE)
                newOp = IR.ROP.LE;
            else if (op.op == Ast.BOP.LT)
                newOp = IR.ROP.LT;
            else if (op.op == Ast.BOP.EQ)
                newOp = IR.ROP.EQ;
            else if (op.op == Ast.BOP.NE)
                newOp = IR.ROP.NE;
            else
                throw new Exception("Error: IF: Invalid ROP");

            ret.add(new IR.CJump(newOp, s1.src, s2.src, l0.lab));
        } else if (n.cond instanceof Ast.BoolLit) {
            Ast.BoolLit b = (Ast.BoolLit)n.cond;

            ret.add(new IR.CJump(IR.ROP.EQ, new IR.BoolLit(b.b), new IR.BoolLit(false), l0.lab));
        } else
            throw new Exception("Error: IF: bad cond type");*/

        CodePack cond = gen(n.cond, cinfo, env);
        ret.add(new IR.CJump(IR.ROP.EQ, cond.src, new IR.BoolLit(false), l0.lab));

        ret.addAll(trueStmt);
        ret.add(l0);
        ret.addAll(falseStmt);
        ret.add(l1);


        return ret;
    }

    // While ---
    // Exp cond;
    // Stmt s;
    //
    // (See class notes.)
    //
    static List<IR.Inst> gen(Ast.While n, ClassInfo cinfo, Env env) throws Exception {
        List<IR.Inst> ret = new ArrayList<IR.Inst>();
        List<IR.Inst> stmt = gen(n.s, cinfo, env);

        IR.LabelDec l0 = new IR.LabelDec(new IR.Label());
        IR.LabelDec l1 = new IR.LabelDec(new IR.Label());

        stmt.add(new IR.Jump(l0.lab));

        ret.add(l0);

        CodePack cond = gen(n.cond, cinfo, env);
        ret.add(new IR.CJump(IR.ROP.EQ, cond.src, new IR.BoolLit(false), l1.lab));

        ret.addAll(stmt);
        ret.add(l1);


        return ret;
    }

    // Print ---
    // PrArg arg;
    //
    // Codegen Guideline:
    // 1. If arg is null, generate an IR.Call to "printStr" with an empty string arg
    // 2. If arg is StrLit, generate an IR.Call to "printStr"
    // 3. Otherwise, generate IR code for arg, and use its type info
    //    to decide which of the two functions, "printInt" and "printBool",
    //    to call
    //
    static List<IR.Inst> gen(Ast.Print n, ClassInfo cinfo, Env env) throws Exception {
        ArrayList<IR.Inst> ret = new ArrayList<IR.Inst>();
        IR.Call test = null;
        IR.Src [] args = new IR.Src[1];
        String printType = null;


        if (n.arg == null)
            return gen(new Ast.Print(new Ast.StrLit("")), cinfo, env);

        if (n.arg instanceof Ast.StrLit) {
            Ast.StrLit str = (Ast.StrLit)n.arg;
            args[0] = new IR.StrLit(str.s);
            printType = "printStr";
        } else {
            CodePack pack = gen((Ast.Exp)n.arg, cinfo, env);
            args[0] = pack.src;

            if (pack.type == IR.Type.INT)
                printType = "printInt";
            else if (pack.type == IR.Type.BOOL)
                printType = "printBool";
        }

        test = new IR.Call(new IR.Global(printType), false, args , null);

        ret.add(test);
        return ret;
    }

    // Return ---
    // Exp val;
    //
    // Codegen Guideline:
    // 1. If val is non-null, generate IR code for it, and generate an IR.Return
    //    with its value
    // 2. Otherwise, generate an IR.Return with no value
    //
    static List<IR.Inst> gen(Ast.Return n, ClassInfo cinfo, Env env) throws Exception {
        ArrayList<IR.Inst> ret = new ArrayList<IR.Inst>();
        IR.Src arg = null;

        if (n.val != null)
            arg = gen(n.val, cinfo, env).src;

        ret.add(new IR.Return(arg));

        return ret;
    }

    // EXPRESSIONS

    // 1. Dispatch a generic gen() call to a specific gen() routine
    //
    static CodePack gen(Ast.Exp n, ClassInfo cinfo, Env env) throws Exception {
        if (n instanceof Ast.Call) return gen((Ast.Call) n, cinfo, env);
        if (n instanceof Ast.NewObj) return gen((Ast.NewObj) n, cinfo, env);
        if (n instanceof Ast.Field) return gen((Ast.Field) n, cinfo, env);
        if (n instanceof Ast.Id) return gen((Ast.Id) n, cinfo, env);
        if (n instanceof Ast.This) return gen((Ast.This) n, cinfo);
        if (n instanceof Ast.IntLit) return gen((Ast.IntLit) n);
        if (n instanceof Ast.BoolLit) return gen((Ast.BoolLit) n);
        throw new GenException("Exp node not supported in this codegen: " + n);
    }

    // 2. Dispatch a generic genAddr call to a specific genAddr routine
    //    (Only one LHS Exp needs to be implemented for this assignment)
    //
    static AddrPack genAddr(Ast.Exp n, ClassInfo cinfo, Env env) throws Exception {
        if (n instanceof Ast.Field) return genAddr((Ast.Field) n, cinfo, env);
        throw new GenException(" LHS Exp node not supported in this codegen: " + n);
    }

    // Call ---
    // Exp obj;
    // String nm;
    // Exp[] args;
    //
    static CodePack gen(Ast.Call n, ClassInfo cinfo, Env env) throws Exception {
        if (n.obj != null)
            return handleCall(n.obj, n.nm, n.args, cinfo, env, true);
        throw new GenException("In Call, obj is null: " + n);
    }

    // NewObj ---
    // String nm;
    //
    // Codegen Guideline:
    //  1. Use class name to find the corresponding ClassInfo record from classEnv
    //  2. Find the class's type and object size from the ClassInfo record
    //  3. Cosntruct a malloc call to allocate space for the object
    //  4. Store a pointer to the class's descriptor into the first slot of
    //     the allocated space
    //
    static CodePack gen(Ast.NewObj n, ClassInfo cinfo, Env env) throws Exception {


        //    ... need code
        throw new Exception("Not Done: genNewObj");

        //return null;
    }

    // Field ---
    // Exp obj;
    // String nm;
    //

    // 1. gen()
    //
    // Codegen Guideline:
    //   1.1 Call genAddr() to generate field variable's address
    //   1.2 Add an IR.Load to get its value
    //
    static CodePack gen(Ast.Field n, ClassInfo cinfo, Env env) throws Exception {


        //    ... need code
        throw new Exception("Not Done: genField");

        //return null;
    }

    // 2. genAddr()
    //
    // Codegen Guideline:
    //   2.1 Call gen() on the obj component
    //   2.2 Call getClassInfo() on the obj component to get base ClassInfo
    //   2.3 Access base ClassInfo rec to get field variable's offset
    //   2.4 Generate an IR.Addr based on the offset
    //
    static AddrPack genAddr(Ast.Field n, ClassInfo cinfo, Env env) throws Exception {


        //    ... need code
        throw new Exception("Not Done: genAddr");

        //return null;
    }

    // Id ---
    // String nm;
    //
    // Codegen Guideline:
    //  1. Check to see if the Id is in the env.
    //  2. If so, it means it is a local variable or a parameter. Just return
    //     a CodePack containing the Id.
    //  3. Otherwise, the Id is an instance variable. Convert it into an
    //     Ast.Field node with Ast.This() as its obj, and invoke the gen() routine
    //     on this new node
    //
    static CodePack gen(Ast.Id n, ClassInfo cinfo, Env env) throws Exception {
        CodePack ret = null;
        if (env.containsKey(n.nm)) {
            if (env.get(n.nm) instanceof ast.Ast.IntType)
                ret = new CodePack(IR.Type.INT, new IR.Id(n.nm));
            else if (env.get(n.nm) instanceof ast.Ast.BoolType)
                ret = new CodePack(IR.Type.BOOL, new IR.Id(n.nm));
            else
                ret = new CodePack(IR.Type.PTR, new IR.Id(n.nm));
        } else {
            Ast.Field field = new Ast.Field(Ast.This, n.nm);
            ret = gen(field, cinfo, env);
        }


        return ret;
    }

    // This ---
    //
    static CodePack gen(Ast.This n, ClassInfo cinfo) throws Exception {
        return new CodePack(IR.Type.PTR, thisObj);
    }

    // IntLit ---
    // int i;
    //
    static CodePack gen(Ast.IntLit n) throws Exception {
        return new CodePack(IR.Type.INT, new IR.IntLit(n.i));
    }

    // BoolLit ---
    // boolean b;
    //
    static CodePack gen(Ast.BoolLit n) throws Exception {
        return new CodePack(IR.Type.BOOL, n.b ? IR.TRUE : IR.FALSE);
    }

    // StrLit ---
    // String s;
    //
    static CodePack gen(Ast.StrLit n) throws Exception {
        return new CodePack(null, new IR.StrLit(n.s));
    }

    // Type mapping (AST -> IR)
    //
    static IR.Type gen(Ast.Type n) throws Exception {
        if (n == null) return null;
        if (n instanceof Ast.IntType) return IR.Type.INT;
        if (n instanceof Ast.BoolType) return IR.Type.BOOL;
        if (n instanceof Ast.ArrayType) return IR.Type.PTR;
        if (n instanceof Ast.ObjType) return IR.Type.PTR;
        throw new GenException("Invalid Ast type: " + n);
    }

}
