# Copyright © Corporation for National Research Initiatives
#
# Makefile for building JPython.  See the instructions in
# rules/make.rules for details.

include rules/make.rules

.PHONY: installer

SUBDIRS= \
	org/python/parser \
	org/python/compiler \
	org/python/core \
	org/python/modules \
	org/python/util \
	org/python/rmi

all: subdirs installer

subdirs:
	@for d in $(SUBDIRS); \
	do \
		(cd $$d; $(MAKE)); \
	done

installer:
	-(cd installer; $(MAKE))

clean::
	@for d in $(SUBDIRS); \
	do \
	    (cd $$d; $(MAKE) clean); \
	done
	-(cd installer; $(MAKE) clean)

realclean::
	@for d in $(SUBDIRS); \
	do \
	    (cd $$d; $(MAKE) realclean); \
	done
	-(cd installer; $(MAKE) realclean)
