# GNU make compatible

JAVAC = javac
JAVA = java
OPT =
CLASSPATH = ../../..

.SUFFIXES= .java .class

%.class: %.java
	$(JAVAC) $(OPT) -classpath "$(CLASSPATH)" $?

SOURCES = $(wildcard *.java)
CLASSES= $(SOURCES:%.java=%.class)

all: $(CLASSES)

clean:
	rm *.class

realclean: clean
