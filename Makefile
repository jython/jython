# GNU make compatible

SUBDIRS= \
	org/python/parser \
	org/python/compiler \
	org/python/core \
	org/python/modules \
	org/python/util

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
