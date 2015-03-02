// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
// 
// Liveness analysis on IR1 program.
//
// (For CS322 - JIngke Li, based on Andrew Tolmach's earlier version.)
//

import java.io.*;
import java.util.*;

import ir1.*;

class Liveness {

    // Utility class for describing sets of registers
    static class RegSet extends HashSet<IR1.Dest> implements Iterable<IR1.Dest> {
        void add_source(IR1.Src rand) {
            if (rand instanceof IR1.Dest)
                add((IR1.Dest) rand);
        }

        void add_dest(IR1.Dest rand) {
            if (rand instanceof IR1.Dest)
                add((IR1.Dest) rand);
        }

        void diff(RegSet os) {
            removeAll(os);
        }

        void union(RegSet os) {
            addAll(os);
        }

        RegSet copy() {
            RegSet s = new RegSet();
            s.addAll(this);
            return s;
        }

        public String toString() {
            String r = "{ ";
            for (IR1.Dest rand : this)
                r += rand + " ";
            r += "}";
            return r;
        }
    }

    // Utility class for describing lists of integers
    // Mainly useful just for its specialized version of toString
    static class IndexList extends ArrayList<Integer> {
        public String toString() {
            String r = "[";
            if (size() > 0) {
                r += get(0);
                for (int i = 1; i < size(); i++)
                    r += "," + get(i);
            }
            r += "]";
            return r;
        }
    }

    // Calculate successor information for each instruction in a function
    static IndexList[] calculateSuccessors(IR1.Func func) {
        SortedMap<String, Integer> labelMap = new TreeMap<String, Integer>();
        for (int i = 1; i <= func.code.length; i++) {
            IR1.Inst c = func.code[i - 1];
            if (c instanceof IR1.LabelDec)
                labelMap.put(((IR1.LabelDec) c).lab.name, i);
        }
        IndexList[] allSuccs = new IndexList[func.code.length + 1];
        for (int i = 1; i < func.code.length; i++) { // there's always a label at the end
            IR1.Inst inst = func.code[i - 1];
            IndexList succs = new IndexList();
            if (inst instanceof IR1.CJump) {
                succs.add(labelMap.get(((IR1.CJump) inst).lab.name));
                succs.add(i + 1);      // safe because there's always a label at the end
            } else if (inst instanceof IR1.Jump)
                succs.add(labelMap.get(((IR1.Jump) inst).lab.name));
            else
                succs.add(i + 1);
            allSuccs[i] = succs;
        }
        allSuccs[func.code.length] = new IndexList();
        return allSuccs;
    }


    // Calculate liveOut information for each instruction in a function
    static RegSet[] calculateLiveness(IR1.Func func) {
        IndexList[] allSuccs = calculateSuccessors(func);

        // Calculate sets of operands used and defined by each Inst
        final RegSet[] used = new RegSet[func.code.length + 1];
        final RegSet[] defined = new RegSet[func.code.length + 1];
        for (int i = 1; i <= func.code.length; i++) {
            used[i] = new RegSet();
            defined[i] = new RegSet();
        }
        for (int i = 1; i <= func.code.length; i++) {
            IR1.Inst inst = func.code[i - 1];
            final int i0 = i;
            calculate(inst, used[i0], defined[i0]);
        }
        for (String var : func.params)
            defined[1].add(new IR1.Id(var));

        // Now solve dataflow equations to calculate
        // set of operands that are live out of each Inst
        RegSet[] liveIn = new RegSet[func.code.length + 1];
        RegSet[] liveOut = new RegSet[func.code.length + 1];
        for (int i = 1; i <= func.code.length; i++) {
            liveIn[i] = new RegSet();
            liveOut[i] = new RegSet();
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = func.code.length; i > 0; i--) {
                RegSet newLiveIn = liveOut[i].copy();
                newLiveIn.diff(defined[i]);
                newLiveIn.union(used[i]);
                liveIn[i] = newLiveIn;
                RegSet newLiveOut = new RegSet();
                for (int n = 0; n < allSuccs[i].size(); n++)
                    newLiveOut.union(liveIn[allSuccs[i].get(n)]);
                if (!liveOut[i].equals(newLiveOut)) {
                    liveOut[i] = newLiveOut;
                    changed = true;
                }
            }
        }
        liveOut[0] = liveIn[1];
        return liveOut;
    }

    // INSTRUCTIONS

    static void calculate(IR1.Inst n, RegSet used, RegSet defined) {
        if (n instanceof IR1.Binop) calculate((IR1.Binop) n, used, defined);
        else if (n instanceof IR1.Unop) calculate((IR1.Unop) n, used, defined);
        else if (n instanceof IR1.Move) calculate((IR1.Move) n, used, defined);
        else if (n instanceof IR1.Load) calculate((IR1.Load) n, used, defined);
        else if (n instanceof IR1.Store) calculate((IR1.Store) n, used, defined);
        else if (n instanceof IR1.LabelDec) ; // no action needed
        else if (n instanceof IR1.CJump) calculate((IR1.CJump) n, used, defined);
        else if (n instanceof IR1.Jump) ; // no action needed
        else if (n instanceof IR1.Call) calculate((IR1.Call) n, used, defined);
        else if (n instanceof IR1.Return) calculate((IR1.Return) n, used, defined);
    }

    static void calculate(IR1.Binop n, RegSet used, RegSet defined) {
        used.add_source(n.src1);
        used.add_source(n.src2);
        defined.add_dest(n.dst);
    }

    static void calculate(IR1.Unop n, RegSet used, RegSet defined) {
        used.add_source(n.src);
        defined.add_dest(n.dst);
    }

    static void calculate(IR1.Move n, RegSet used, RegSet defined) {
        used.add_source(n.src);
        defined.add_dest(n.dst);
    }

    static void calculate(IR1.Load n, RegSet used, RegSet defined) {
        used.add_source(n.addr.base);
        defined.add_dest(n.dst);
    }

    static void calculate(IR1.Store n, RegSet used, RegSet defined) {
        used.add_source(n.src);
        used.add_source(n.addr.base);
    }

    static void calculate(IR1.CJump n, RegSet used, RegSet defined) {
        used.add_source(n.src1);
        used.add_source(n.src2);
    }

    static void calculate(IR1.Call n, RegSet used, RegSet defined) {
        for (IR1.Src a : n.args)
            used.add_source(a);
        if (n.rdst != null)
            defined.add_dest(n.rdst);
    }

    static void calculate(IR1.Return n, RegSet used, RegSet defined) {
        if (n.val != null)
            used.add_source(n.val);
    }

    static class Interval {
        int start;
        int end;

        Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public String toString() {
            return "[" + start + "," + end + "]";
        }
    }

    // calculate live interval for each operand in function
    static Map<IR1.Dest, Interval> calculateLiveIntervals(IR1.Func func) {
        Map<IR1.Dest, Interval> liveIntervals = new HashMap<IR1.Dest, Interval>();
        RegSet liveOut[] = calculateLiveness(func);
        for (int i = 0; i <= func.code.length; i++) {
            for (IR1.Dest t : liveOut[i]) {
                Interval n = liveIntervals.get(t);
                if (n == null) {
                    n = new Interval(i, i);
                    liveIntervals.put(t, n);
                } else
                    n.end = i;
            }
        }
        return liveIntervals;
    }

}


