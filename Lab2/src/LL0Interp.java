// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
// 
// An LL0 interpreter. (For CS322, Jingke Li)
//
//
import java.io.*;

public class LL0Interp {

  static enum OpCode { LOAD, STORE, MOVE, ADD, SUB, JUMP, JUMPZ, HALT }
  static enum State  { RUNNING, HALTED, FAILED }

  static class Inst {
    public OpCode op;
    public int n;
    public Inst(OpCode op, int n) { this.op=op; this.n=n; }
    public Inst(OpCode op) { this.op=op; this.n=0; }
  }

  static final int CODESIZE=4096, MEMSIZE=4096;
  static Inst[] code = new Inst[CODESIZE];
  static int[] mem = new int[MEMSIZE];
  static int PC, ACC;
  static State status;

  // The main routine
  //
  public static void main(String [] args) {
    PC = 0; ACC = 0; status = State.RUNNING;
    load();
    // Instruction Semantics:
    // 	 LOAD n:  ACC <- mem[n]		 
    // 	 STORE n: mem[n] <- ACC		 
    // 	 MOVE n:  ACC <- n			 
    // 	 ADD n:   ACC <- ACC + mem[n]	 
    // 	 SUB n:   ACC <- ACC - mem[n]	 
    // 	 JUMP n:  PC <- n			 
    // 	 JUMPZ n: if ACC==0 then PC <- n	 
    // 	 HALT:    stop execution               
    //
    do {
      Inst inst = code[PC++];
      int n = inst.n;
      switch (inst.op) {
      case LOAD:  ACC = mem[n];  	 break;
      case STORE: mem[n] = ACC;  	 break;
      case MOVE:  ACC = n;       	 break;
      case ADD:   ACC += mem[n]; 	 break;
      case SUB:   ACC -= mem[n]; 	 break;  
      case JUMP:  PC = n;                break;
      case JUMPZ: if (ACC==0) PC = n;    break;
      case HALT:  status = State.HALTED; break;
      default:    status = State.FAILED;
      }
    } while (status == State.RUNNING);

    if (status == State.HALTED)
      System.out.println(ACC);
    else
      System.out.println("Program failed");
  }

  // A test program for adding 1, 2, and 3
  //
  // 0.  MOVE 1       ; load 1 to ACC
  // 1.  STORE 0      ; store 1 to mem[0]
  // 2.  MOVE 2       ; load 2 to ACC 
  // 3.  STORE 1      ; store 2 to mem[1]
  // 4.  MOVE 3       ; load 3 to ACC
  // 5.  ADD 0        ; ACC += mem[0] 
  // 6.  ADD 1        ; ACC += mem[1] 
  // 7.  HALT
  //
  static void load() {
    code[0] = new Inst(OpCode.MOVE, 1);
    code[1] = new Inst(OpCode.STORE, 0);
    code[2] = new Inst(OpCode.MOVE, 2);
    code[3] = new Inst(OpCode.STORE, 1);
    code[4] = new Inst(OpCode.MOVE, 3);
    code[5] = new Inst(OpCode.ADD, 0);
    code[6] = new Inst(OpCode.ADD, 1);
    code[7] = new Inst(OpCode.HALT);
  }

}
