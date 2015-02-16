# Makefile for CS322 Lab2. (Jingke Li)
#
JFLAGS = -g
JC = javac
JCC = javacc

.SUFFIXES: .jj .java .class

.PRECIOUS: %.java

.jj.java:
	$(JCC) $*.jj

.java.class:
	$(JC) $(JFLAGS) $*.java

ll0:	LL0Interp.class

ll1:	LL1Interp.class

el0: EL0.class EL0Parser.class EL0Interp.class

el1: EL1.class EL1Parser.class EL1Interp.class
