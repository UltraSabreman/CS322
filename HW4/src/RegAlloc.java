// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
// 
// IR1->X86-64 register allocation module.
//
// (For CS322 - Jingke Li, based on Andrew Tolmach's earlier version.)
//

import java.io.*;
import java.util.*;

import ir1.*;

public class RegAlloc {
    static class RegAllocException extends Exception {
        public RegAllocException(String msg) {
            super(msg);
        }
    }

    // scratch registers
    static final X86.Reg tempReg1 = X86.R10;
    static final X86.Reg tempReg2 = X86.R11;

    // Allocate IR1.Ids and IR1.Temps to X86.Regs using the linear scan
    // algorithm.
    // - If an Id or Temp is determined to be dead (e.g. a unsed param),
    //   then no register will be assigned to it.
    //
    static Map<IR1.Dest, X86.Reg> linearScan(IR1.Func func) throws Exception {

        // register mappings (to be returned to caller)
        Map<IR1.Dest, X86.Reg> regMap = new HashMap<IR1.Dest, X86.Reg>();

        // desired register mappings (used in the routine)
        Map<IR1.Dest, X86.Reg> preference = new HashMap<IR1.Dest, X86.Reg>();

        // liveness information for Temps and Ids
        Map<IR1.Dest, Liveness.Interval> liveIntervals
                = Liveness.calculateLiveIntervals(func);
        int liveCount = liveIntervals.size();

        // Computing preferences
        //-------------------------------------------------------------------
        // Preferences are not binding. In particular, ranges that span a
        // call will never end up in a caller-save register, but we don't
        // worry about that now.
        //
        // Note: all preference registers should be caller-save (otherwise
        // they're ignored)

        // Incoming arguments from callee's perspective
        // - just fail if there are more than 6 args
        //
        int paramCount = func.params.length;
        if (paramCount > X86.argRegs.length) {
            throw new RegAllocException("Func has too many args: " + paramCount);
        }
        for (int i = 0; i < paramCount; i++)
            preference.put(new IR1.Id(func.params[i]), X86.argRegs[i]);

        for (IR1.Inst c : func.code) {
            if (c instanceof IR1.Call) {
                // arguments from caller's perspective
                IR1.Call cl = (IR1.Call) c;
                for (int i = 0; i < cl.args.length; i++) {
                    IR1.Src argRand = cl.args[i];
                    if (argRand instanceof IR1.Dest)
                        preference.put((IR1.Dest) argRand, X86.argRegs[i]);
                }
                // return value from caller's perspective
                if (cl.rdst instanceof IR1.Dest)
                    preference.put((IR1.Dest) cl.rdst, X86.RAX);
            } else if (c instanceof IR1.Return) {
                // Return value from callee's perspective
                IR1.Return r = (IR1.Return) c;
                if (r.val instanceof IR1.Dest)
                    preference.put((IR1.Dest) r.val, X86.RAX);
            } else if (c instanceof IR1.Binop) {
                // Argument and result of DIV
                IR1.Binop b = (IR1.Binop) c;
                if (b.op == IR1.AOP.DIV) {
                    if (b.src1 instanceof IR1.Dest)
                        preference.put((IR1.Dest) b.src1, X86.RAX);
                    if (b.dst instanceof IR1.Dest)
                        preference.put((IR1.Dest) b.dst, X86.RAX);
                }
            }
        }

        // Linear Scan Allocation
        //-------------------------------------------------------------------
        // Keep track of available registers.
        // Does not handle spilling (If registers run out, simply leave Id
        // or Temp unassigned.

        // Prepare an array of assignable registers
        //
        boolean[] regAvailable = new boolean[X86.allRegs.length];
        for (int i = 0; i < regAvailable.length; i++)
            regAvailable[i] = true;
        regAvailable[X86.RSP.r] = false;
        regAvailable[tempReg1.r] = false;
        regAvailable[tempReg2.r] = false;

        // Build two parallel lists describing live intervals sorted by
        // start point
        //
        List<Integer> liveStarts = new ArrayList<Integer>(liveCount);
        List<IR1.Dest> liveValues = new ArrayList<IR1.Dest>(liveCount);
        for (Map.Entry<IR1.Dest, Liveness.Interval> me : liveIntervals.entrySet()) {
            IR1.Dest t = me.getKey();
            Liveness.Interval n = me.getValue();
            int ip = insertionPoint(liveStarts, n.start);
            liveStarts.add(ip, n.start);
            liveValues.add(ip, t);
        }

        // Active intervals are maintained as two parallel lists, sorted
        // by end point
        //
        List<Integer> activeEnds = new ArrayList<Integer>(liveCount);
        List<X86.Reg> activeRegs = new ArrayList<X86.Reg>(liveCount);
        Iterator<Integer> it = liveStarts.iterator();
        Iterator<IR1.Dest> pt = liveValues.iterator();
        while (it.hasNext() && pt.hasNext()) {
            int start = it.next();
            IR1.Dest t = pt.next();
            expire:
            {
                Iterator<Integer> jt = activeEnds.iterator();
                Iterator<X86.Reg> kt = activeRegs.iterator();
                while (jt.hasNext() && kt.hasNext()) {
                    int end = jt.next();
                    if (end >= start)
                        break expire;
                    X86.Reg reg = kt.next();
                    jt.remove();
                    kt.remove();
                    regAvailable[reg.r] = true;
                }
            }
            // try to find a register
            X86.Reg treg = null;
            find:
            {
                Liveness.Interval n = liveIntervals.get(t);
                if (intervalContainsCall(func, n)) {
                    // insist on a callee-save reg (ignoring any preference)
                    for (X86.Reg reg : X86.calleeSaveRegs)
                        if (regAvailable[reg.r]) {
                            treg = reg;
                            break find;
                        }
                } else {
                    // try first for a preference register (always caller-save)
                    X86.Reg preg = preference.get(t);
                    if (preg != null && regAvailable[preg.r]) {
                        treg = preg;
                        break find;
                    }
                    // try for arbitrary caller-save reg
                    for (X86.Reg reg : X86.callerSaveRegs)
                        if (regAvailable[reg.r]) {
                            treg = reg;
                            break find;
                        }
                    // otherwise, try a callee-save
                    for (X86.Reg reg : X86.calleeSaveRegs)
                        if (regAvailable[reg.r]) {
                            treg = reg;
                            break find;
                        }
                }
                // couldn't find a register
                throw new RegAllocException("Oops: out of registers");
            }
            // found a register; record it
            regAvailable[treg.r] = false;
            int end = liveIntervals.get(t).end;
            int ip = insertionPoint(activeEnds, end);
            activeEnds.add(ip, end);
            activeRegs.add(ip, treg);
            regMap.put(t, treg);
        }
        // return the register mappings
        return regMap;
    }

    // Return true if specified interval includes an IR instruction
    // that will cause an X86.call (or invoke an X86.divide)
    //
    private static boolean intervalContainsCall(IR1.Func func, Liveness.Interval n) {
        for (int i = n.start + 1; i <= n.end; i++)
            if (func.code[i - 1] instanceof IR1.Call ||
                    (func.code[i - 1] instanceof IR1.Binop &&
                            ((IR1.Binop) func.code[i - 1]).op == IR1.AOP.DIV))
                return true;
        return false;
    }

    // Find insertion point for x in a, assuming a is sorted in natural
    // order
    //
    private static int insertionPoint(List<Integer> a, int x) {
        int i = 0;
        for (Integer y : a) {
            if (x <= y)
                return i;
            i++;
        }
        return i;
    }

}
