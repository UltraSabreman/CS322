# Makefile for CS322 Homework 4.
#
JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

all:	codegen

ir1:	ir1/IR1.class

codegen: ir1 CodeGen.class

clean:
	'rm' *.class ir1/*.class


