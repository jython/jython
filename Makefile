# Copyright © Corporation for National Research Initiatives
#
# Makefile for building JPython.  See the instructions in
# rules/make.rules for details.

include Misc/make.rules

.PHONY: installer

SUBDIRS= \
	org/apache/oro/text/regex \
	org/python/parser \
	org/python/compiler \
	org/python/core \
	org/python/modules \
	org/python/util \
	org/python/rmi \
	Lib/jxxload_help

CLEANDIRS = $(SUBDIRS) \
	Lib \
	Lib/pawt \
	Lib/test \
	Lib/test/bugs \
	Lib/test/bugs/pr133 \
	Lib/test/javatests \
	Tools/jythonc \
	Tools/freeze \
	Demo/applet \
	Demo/awt \
	Demo/bean \
	Demo/embed \
	Demo/javaclasses \
	Demo/swing

all: subdirs

subdirs:
	@for d in $(SUBDIRS); \
	do \
		(cd $$d; $(MAKE)); \
	done

clean::
	@for d in $(CLEANDIRS); \
	do \
	    (cd $$d; $(MAKE) clean); \
	done

realclean::
	@for d in $(CLEANDIRS); \
	do \
	    (cd $$d; $(MAKE) realclean); \
	done
	-(cd installer; $(MAKE) realclean)
