# nice and robust, the way we like shell scripts ;)

pushd $JYTHON_PATH/src
java org.antlr.Tool org/python/antlr/Python.g
javac org/python/antlr/*.java
jar cf ../grammar.jar org/python/antlr/*.class
popd
