# GNU make compatible

SUBDIRS= \
	src/org/python/parser \
	src/org/python/compiler \
	src/org/python/core \
	src/org/python/modules \
	src/org/python/util

all: subdirs

subdirs: $(SUBDIRS)
	@for d in $(SUBDIRS); \
	do \
		(cd $$d; $(MAKE)); \
	done

clean: $(SUBDIRS)
	@for d in $(SUBDIRS); \
	do \
	    (cd $$d; $(MAKE) clean); \
	done

realclean: $(SUBDIRS)
	@for d in $(SUBDIRS); \
	do \
	    (cd $$d; $(MAKE) realclean); \
	done
