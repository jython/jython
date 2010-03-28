/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import org.python.indexer.Indexer;
import org.python.indexer.NBinding;
import org.python.indexer.Scope;
import org.python.indexer.ast.NAlias;
import org.python.indexer.ast.NAssert;
import org.python.indexer.ast.NAssign;
import org.python.indexer.ast.NAttribute;
import org.python.indexer.ast.NAugAssign;
import org.python.indexer.ast.NBinOp;
import org.python.indexer.ast.NBlock;
import org.python.indexer.ast.NBody;
import org.python.indexer.ast.NBoolOp;
import org.python.indexer.ast.NBreak;
import org.python.indexer.ast.NCall;
import org.python.indexer.ast.NClassDef;
import org.python.indexer.ast.NCompare;
import org.python.indexer.ast.NComprehension;
import org.python.indexer.ast.NContinue;
import org.python.indexer.ast.NDelete;
import org.python.indexer.ast.NDict;
import org.python.indexer.ast.NEllipsis;
import org.python.indexer.ast.NExceptHandler;
import org.python.indexer.ast.NExec;
import org.python.indexer.ast.NExprStmt;
import org.python.indexer.ast.NFor;
import org.python.indexer.ast.NFunctionDef;
import org.python.indexer.ast.NGeneratorExp;
import org.python.indexer.ast.NGlobal;
import org.python.indexer.ast.NIf;
import org.python.indexer.ast.NIfExp;
import org.python.indexer.ast.NImport;
import org.python.indexer.ast.NImportFrom;
import org.python.indexer.ast.NIndex;
import org.python.indexer.ast.NKeyword;
import org.python.indexer.ast.NLambda;
import org.python.indexer.ast.NList;
import org.python.indexer.ast.NListComp;
import org.python.indexer.ast.NModule;
import org.python.indexer.ast.NName;
import org.python.indexer.ast.NNode;
import org.python.indexer.ast.NNodeVisitor;
import org.python.indexer.ast.NNum;
import org.python.indexer.ast.NPass;
import org.python.indexer.ast.NPlaceHolder;
import org.python.indexer.ast.NPrint;
import org.python.indexer.ast.NQname;
import org.python.indexer.ast.NRaise;
import org.python.indexer.ast.NRepr;
import org.python.indexer.ast.NReturn;
import org.python.indexer.ast.NSlice;
import org.python.indexer.ast.NStr;
import org.python.indexer.ast.NSubscript;
import org.python.indexer.ast.NTryExcept;
import org.python.indexer.ast.NTryFinally;
import org.python.indexer.ast.NTuple;
import org.python.indexer.ast.NUnaryOp;
import org.python.indexer.ast.NUrl;
import org.python.indexer.ast.NWhile;
import org.python.indexer.ast.NWith;
import org.python.indexer.ast.NYield;
import org.python.indexer.types.NDictType;
import org.python.indexer.types.NFuncType;
import org.python.indexer.types.NListType;
import org.python.indexer.types.NModuleType;
import org.python.indexer.types.NTupleType;
import org.python.indexer.types.NType;
import org.python.indexer.types.NUnionType;
import org.python.indexer.types.NUnknownType;

import java.io.File;
import java.util.List;
import java.util.Set;

public class IndexerTest extends TestBase {

    public void testBuiltinModulePresent() throws Exception {
        NType mod = idx.moduleTable.lookupType("__builtin__");
        assertNotNull("missing __builtin__ module", mod);
        assertTrue("wrong type: " + mod.getClass(), mod instanceof NModuleType);
    }

    public void testLazyModuleLoad() throws Exception {
        assertNull("'array' module should not yet be loaded",
                   idx.moduleTable.lookupType("array"));
        assertNoBinding("array");

        assertNotNull(idx.loadModule("array"));  // lazy loads it

        assertNotNull("'array' module should have been loaded",
                      idx.moduleTable.lookupType("array"));
        assertModuleBinding("array");
    }

    public void testNativeModulesAvailable() throws Exception {
        for (String name : new String[] {
                "array", "ctypes", "errno",
                "math", "operator", "os",
                "signal", "sys", "thread", "time",}) {
            assertNoBinding(name);
            assertNotNull(name, idx.loadModule(name));
            assertModuleBinding(name);
        }
    }

    public void testBuiltinObject() throws Exception {
        assertClassBinding("__builtin__.object");
        assertClassBinding("__builtin__.object.__class__");
    }

    public void testBuiltinTuple() throws Exception {
        assertClassBinding("__builtin__.tuple");
        assertMethodBinding("__builtin__.tuple.__rmul__");
        assertMethodBinding("__builtin__.tuple.__iter__");
    }

    public void testBuiltinList() throws Exception {
        assertClassBinding("__builtin__.list");
        assertMethodBinding("__builtin__.list.append");
        assertMethodBinding("__builtin__.list.count");
    }

    public void testBuiltinNum() throws Exception {
        assertClassBinding("__builtin__.float");
        NBinding b = assertMethodBinding("__builtin__.float.fromhex");
        assertTrue(b.isBuiltin());
    }

    public void testBuiltinStr() throws Exception {
        assertClassBinding("__builtin__.str");
        assertMethodBinding("__builtin__.str.encode");
        assertMethodBinding("__builtin__.str.startswith");
        assertMethodBinding("__builtin__.str.split");
        assertMethodBinding("__builtin__.str.partition");
    }

    public void testBuiltinDict() throws Exception {
        assertClassBinding("__builtin__.dict");
        assertMethodBinding("__builtin__.dict.__getitem__");
        assertMethodBinding("__builtin__.dict.keys");
        assertMethodBinding("__builtin__.dict.clear");
    }

    public void testBuiltinFile() throws Exception {
        assertClassBinding("__builtin__.file");
        assertMethodBinding("__builtin__.file.__enter__");
        assertMethodBinding("__builtin__.file.readline");
        assertMethodBinding("__builtin__.file.readlines");
        assertMethodBinding("__builtin__.file.isatty");
    }

    public void testBuiltinFuncs() throws Exception {
        assertFunctionBinding("__builtin__.apply");
        assertFunctionBinding("__builtin__.abs");
        assertFunctionBinding("__builtin__.hex");
        assertFunctionBinding("__builtin__.range");
        assertFunctionBinding("__builtin__.globals");
        assertFunctionBinding("__builtin__.open");
    }

    public void testBuiltinTypes() throws Exception {
        assertClassBinding("__builtin__.ArithmeticError");
        assertClassBinding("__builtin__.ZeroDivisionError");
        assertAttributeBinding("__builtin__.True");
        assertAttributeBinding("__builtin__.False");
        assertAttributeBinding("__builtin__.None");
        assertAttributeBinding("__builtin__.Ellipsis");
    }

    public void testStrConstructor() throws Exception {
        String src = index(
            "newstr.py",
            "x = str([])");
        assertStringType("newstr.x");
    }

    public void testListSubscript() throws Exception {
        String src = index(
            "test.py",
            "x = [1, 2, 3]",
            "y = x[2]");
        assertNumType("test.y");
    }

    public void testBuiltinSys() throws Exception {
        idx.loadModule("sys");
        assertModuleBinding("sys");
        assertAttributeBinding("sys.__stderr__");
        NBinding b = assertFunctionBinding("sys.exit");
        assertTrue(b.isBuiltin());
        assertFunctionBinding("sys.getprofile");
        assertFunctionBinding("sys.getdefaultencoding");
        assertAttributeBinding("sys.api_version");
        assertNumType("sys.api_version");
        assertAttributeBinding("sys.argv");
        assertBindingType("sys.argv", NListType.class);
        assertAttributeBinding("sys.byteorder");
        assertStringType("sys.byteorder");
        assertAttributeBinding("sys.flags");
        assertBindingType("sys.flags", NDictType.class);
    }

    public void testFetchAst() throws Exception {
        NModule ast = idx.getAstForFile(abspath("hello.py"));
        assertNotNull("failed to load file", ast);
        assertEquals("module has wrong name", "hello", ast.name);
        assertNotNull("AST has no body", ast.body);
        assertNotNull("AST body has no children", ast.body.seq);
        assertEquals("wrong number of children", 1, ast.body.seq.size());
        NNode e = ast.body.seq.get(0);
        assertTrue("Incorrect AST: " + e.getClass(), e instanceof NExprStmt);
        e = ((NExprStmt)e).value;
        assertTrue("Incorrect AST: " + e.getClass(), e instanceof NStr);
        assertEquals("Wrong string content", "Hello", ((NStr)e).n.toString());
    }

    public void testFileLoad() throws Exception {
        idx.loadFile(abspath("testfileload.py"), /*skipParentChain=*/true);
        idx.ready();
        assertEquals("loaded more than 1 file", 1, idx.numFilesLoaded());
    }

    public void testAstCacheTmpDir() throws Exception {
        AstCache cache = AstCache.get();
        File f = new File(AstCache.CACHE_DIR);
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertTrue(f.canWrite());
        assertTrue(f.isDirectory());
    }

    public void testAstCacheNames() throws Exception {
        AstCache cache = AstCache.get();
        String sourcePath = abspath("hello.py");
        String cachePath = cache.getCachePath(new File(sourcePath));
        String cachedName = new File(cachePath).getName();
        assertTrue("Invalid cache name: " + cachedName,
                   cachedName.matches("^hello.py[A-Za-z0-9]{32}.ast$"));
    }

    public void testAstCache() throws Exception {
        AstCache cache = AstCache.get();
        String sourcePath = abspath("hello.py");

        // ensure not cached on disk
        NModule ast = cache.getSerializedModule(sourcePath);
        assertNull(ast);

        cache.getAST(sourcePath);

        // ensure cached on disk
        ast = cache.getSerializedModule(sourcePath);
        assertNotNull(ast);

        assertEquals(sourcePath, ast.getFile());
    }

    public void testAstCacheEmptyFile() throws Exception {
        AstCache cache = AstCache.get();
        NModule mod = cache.getAST(abspath("empty_file.py"));
        assertNotNull(mod);
        NBlock seq = mod.body;
        assertNotNull(seq);
        assertTrue(seq.seq.isEmpty());
    }

    // Make sure node types all have NType None when constructed,
    // to ensure that no nodes are relying on a particular type when being
    // resolved (since deserialization won't call the constructor).
    public void testConstructedTypes() throws Exception {
        assertNoneType(new NAlias(null, null, null));
        assertNoneType(new NAssert(null, null));
        assertNoneType(new NAssign(null, null));
        assertNoneType(new NAttribute(new NStr(), new NName("")));
        assertNoneType(new NAugAssign(null, null, null));
        assertNoneType(new NBinOp(null, null, null));
        assertNoneType(new NBlock(null));
        assertNoneType(new NBody((List<NNode>)null));
        assertNoneType(new NBoolOp(null, null));
        assertNoneType(new NBreak());
        assertNoneType(new NCall(null, null, null, null, null));
        assertNoneType(new NClassDef(null, null, null));
        assertNoneType(new NCompare(null, null, null));
        assertNoneType(new NComprehension(null, null, null));
        assertNoneType(new NContinue());
        assertNoneType(new NDelete(null));
        assertNoneType(new NDict(null, null));
        assertNoneType(new NEllipsis());
        assertNoneType(new NExceptHandler(null, null, null));
        assertNoneType(new NExec(null, null, null));
        assertNoneType(new NExprStmt(null));
        assertNoneType(new NFor(null, null, null, null));
        assertNoneType(new NFunctionDef(null, null, null, null, null, null));
        assertNoneType(new NGeneratorExp(null, null));
        assertNoneType(new NGlobal(null));
        assertNoneType(new NIf(null, null, null));
        assertNoneType(new NIfExp(null, null, null));
        assertNoneType(new NImport(null));
        assertNoneType(new NImportFrom(null, null, null));
        assertNoneType(new NIndex(null));
        assertNoneType(new NKeyword(null, null));
        assertNoneType(new NLambda(null, null, null, null, null));
        assertNoneType(new NList(null));
        assertNoneType(new NListComp(null, null));
        assertNoneType(new NModule(null, 0, 1));
        assertNoneType(new NName(""));
        assertNoneType(new NNum(-1));
        assertNoneType(new NPass());
        assertNoneType(new NPlaceHolder());
        assertNoneType(new NPrint(null, null));
        assertNoneType(new NQname(null, new NName("")));
        assertNoneType(new NRaise(null, null, null));
        assertNoneType(new NRepr(null));
        assertNoneType(new NReturn(null));
        assertNoneType(new NSlice(null, null, null));
        assertNoneType(new NStr());
        assertNoneType(new NSubscript(null, null));
        assertNoneType(new NTryExcept(null, null, null));
        assertNoneType(new NTryFinally(null, null));
        assertNoneType(new NTuple(null));
        assertNoneType(new NUnaryOp(null, null));
        assertNoneType(new NUrl(""));
        assertNoneType(new NWhile(null, null, null));
        assertNoneType(new NWith(null, null, null));
        assertNoneType(new NYield(null));
    }

    private void assertNoneType(NNode n) {
        assertEquals(n.getType(), Indexer.idx.builtins.None);
    }

    public void testClassTypeBuiltinAttrs() throws Exception {
        String file = "classtype_builtins.py";
        buildIndex(file);
        NModuleType module = (NModuleType)idx.moduleTable.lookupType(abspath(file));
        Scope mtable = module.getTable();
        assertTrue(mtable.lookupType("MyClass").isClassType());
        assertTrue(mtable.lookupType("MyClassNoDoc").isClassType());
        assertTrue(mtable.lookupType("MyClass").getTable().getParent() == mtable);
        assertEquals(NBinding.Kind.CLASS, mtable.lookup("MyClass").getKind());
        Scope t = mtable.lookupType("MyClass").getTable();
        assertTrue(t.lookupType("__bases__").isTupleType());
        assertTrue(t.lookupType("__dict__").isDictType());
        assertEquals(idx.builtins.BaseStr, t.lookupType("__name__"));
        assertEquals(idx.builtins.BaseStr, t.lookupType("__module__"));
        assertEquals(idx.builtins.BaseStr, t.lookupType("__doc__"));
        t = mtable.lookupType("MyClassNoDoc").getTable();
        assertEquals(idx.builtins.BaseStr, t.lookupType("__doc__"));
    }

    public void testMethodBuiltinAttrs() throws Exception {
        String file = "classtype_builtins.py";
        buildIndex(file);

        Scope mtable = idx.moduleTable.lookupType(abspath(file)).getTable();
        NBinding method = mtable.lookupType("MyClass").getTable().lookup("__init__");
        assertNotNull(method);
        assertEquals(NBinding.Kind.CONSTRUCTOR, method.getKind());
        assertEquals("classtype_builtins.MyClass.__init__", method.getQname());

        NType ftype = mtable.lookupType("MyClass").getTable().lookupType("__init__");
        assertTrue(ftype.isFuncType());

        NBinding c = mtable.lookup("MyClass");
        for (String special : new String[]{"im_class", "__class__", "im_self", "__self__"}) {
            NBinding attr = ftype.getTable().lookup(special);
            assertNotNull("missing binding for " + special, attr);
            assertEquals(c.getType(), attr.getType());
        }
    }

    public void testModulePaths() throws Exception {
        idx.loadModule("pkg");
        idx.loadModule("pkg.animal");
        idx.loadModule("pkg.mineral.stone.lapis");
        idx.ready();

        assertModuleBinding("pkg");
        assertModuleBinding("pkg.animal");
        assertModuleBinding("pkg.mineral.stone.lapis");
    }

    public void testCircularImport() throws Exception {
        idx.loadModule("pkg.animal.mammal.cat");
        idx.ready();
        // XXX:  finish me
    }

    public void testBasicDefsAndRefs() throws Exception {
        idx.loadModule("refs");
        idx.ready();
        assertScopeBinding("refs.foo");
        String src = getSource("refs.py");
        assertDefinition("refs.foo", "foo", nthIndexOf(src, "foo", 1));

        assertNoReference("Definition site should not produce a reference",
                          "refs.foo", nthIndexOf(src, "foo", 1), "foo".length());

        assertReference("refs.foo", nthIndexOf(src, "foo", 2));
        assertReference("refs.foo", nthIndexOf(src, "foo", 3));
        assertReference("refs.foo", nthIndexOf(src, "foo", 4));
        assertReference("refs.foo", nthIndexOf(src, "foo", 5));

        assertNoReference("Should not have been a reference inside a string",
                          "refs.foo", nthIndexOf(src, "foo", 6), "foo".length());

        assertReference("refs.foo", nthIndexOf(src, "foo", 7));
        assertReference("refs.foo", nthIndexOf(src, "foo", 8));
        assertReference("refs.foo", nthIndexOf(src, "foo", 9));
        assertReference("refs.foo", nthIndexOf(src, "foo", 10));
        assertReference("refs.foo", nthIndexOf(src, "foo", 11));
        assertReference("refs.foo", nthIndexOf(src, "foo", 12));

        assertNoReference("Function param cannot refer to outer scope",
                          "refs.foo", nthIndexOf(src, "foo", 13), "foo".length());

        assertNoReference("Function param 'foo' should hide outer foo",
                          "refs.foo", nthIndexOf(src, "foo", 14), "foo".length());

        assertReference("refs.foo", nthIndexOf(src, "foo", 15));
        assertReference("refs.foo", nthIndexOf(src, "foo", 16));
    }

    public void testAutoClassBindings() throws Exception {
        idx.loadModule("class1");
        idx.ready();
        assertModuleBinding("class1");
        assertClassBinding("class1.A");

        NBinding b = assertAttributeBinding("class1.A.__bases__");
        assertStaticSynthetic(b);
        assertTrue(b.getType().isTupleType());
        assertTrue(((NTupleType)b.getType()).getElementTypes().isEmpty());

        b = assertAttributeBinding("class1.A.__name__");
        assertStaticSynthetic(b);
        assertEquals(b.getType(), idx.builtins.BaseStr);

        b = assertAttributeBinding("class1.A.__module__");
        assertStaticSynthetic(b);
        assertEquals(b.getType(), idx.builtins.BaseStr);

        b = assertAttributeBinding("class1.A.__doc__");
        assertStaticSynthetic(b);
        assertEquals(b.getType(), idx.builtins.BaseStr);

        b = assertAttributeBinding("class1.A.__dict__");
        assertStaticSynthetic(b);
        assertTrue(b.getType().isDictType());
        assertEquals(((NDictType)b.getType()).getKeyType(), idx.builtins.BaseStr);
        assertTrue(((NDictType)b.getType()).getValueType().isUnknownType());
    }

    public void testLocalVarRef() throws Exception {
        idx.loadModule("class2");
        idx.ready();
        assertFunctionBinding("class2.hi");
        assertParamBinding("class2.hi@msg");
        String src = getSource("class2.py");
        assertReference("class2.hi@msg", nthIndexOf(src, "msg", 2));
    }

    public void testClassMemberBindings() throws Exception {
        idx.loadModule("class1");
        idx.ready();
        assertScopeBinding("class1.A.a");
        assertConstructorBinding("class1.A.__init__");
        assertMethodBinding("class1.A.hi");
        assertParamBinding("class1.A.__init__@self");
        assertParamBinding("class1.A.hi@self");
        assertParamBinding("class1.A.hi@msg");

        String src = getSource("class1.py");
        assertReference("class1.A.hi@msg", nthIndexOf(src, "msg", 2));
        assertReference("class1.A", src.indexOf("A.a"), 1);
        assertReference("class1.A.a", src.indexOf("A.a") + 2, 1);
        assertScopeBinding("class1.x");
        assertScopeBinding("class1.y");
        assertScopeBinding("class1.z");
        assertReference("class1.A", src.indexOf("= A") + 2, 1);
        assertConstructed("class1.A", src.indexOf("A()"), 1);
        assertReference("class1.y", src.indexOf("y.b"), 1);

        assertInstanceType("class1.y", "class1.A");
        assertReference("class1.A.b", src.indexOf("y.b") + 2, 1);
        assertScopeBinding("class1.z");
        assertNumType("class1.z");
    }

    public void testCallNewRef() throws Exception {
        idx.loadModule("callnewref");
        idx.ready();
        String src = getSource("callnewref.py");

        String fsig = "callnewref.myfunc";
        assertFunctionBinding(fsig);
        assertDefinition(fsig, "myfunc", src.indexOf("myfunc"));
        assertReference(fsig, nthIndexOf(src, "myfunc", 2));
        assertCall(fsig, nthIndexOf(src, "myfunc", 3));

        String csig = "callnewref.MyClass";
        assertClassBinding(csig);
        assertDefinition(csig, "MyClass", src.indexOf("MyClass"));
        assertReference(csig, nthIndexOf(src, "MyClass", 2));
        assertConstructed(csig, nthIndexOf(src, "MyClass", 3));

        String msig = "callnewref.MyClass.mymethod";
        assertMethodBinding(msig);
        assertDefinition(msig, "mymethod", src.indexOf("mymethod"));
        assertReference(msig, nthIndexOf(src, "mymethod", 2));
        assertCall(msig, nthIndexOf(src, "mymethod", 3));
    }

    public void testPackageLoad() throws Exception {
        idx.loadModule("pkgload");
        idx.ready();
        assertModuleBinding("pkgload");
        assertModuleBinding("pkg");
        assertScopeBinding("pkg.myvalue");
    }

    public void testUnqualifiedSamePkgImport() throws Exception {
        idx.loadModule("pkg.animal.reptile.snake");
        idx.ready();
        assertModuleBinding("pkg.animal.reptile.snake");
        assertModuleBinding("pkg.animal.reptile.croc");
        assertClassBinding("pkg.animal.reptile.snake.Snake");
        assertClassBinding("pkg.animal.reptile.snake.Python");
        assertClassBinding("pkg.animal.reptile.croc.Crocodilian");
        assertClassBinding("pkg.animal.reptile.croc.Gavial");

        String snakeSrc = getSource("pkg/animal/reptile/snake.py");
        assertReference("pkg.animal.reptile.croc", snakeSrc.indexOf("croc"));
        assertReference("pkg.animal.reptile.croc", nthIndexOf(snakeSrc, "croc", 2));
        assertReference("pkg.animal.reptile.croc.Gavial", snakeSrc.indexOf("Gavial"));
    }

    public void testAbsoluteImport() throws Exception {
        idx.loadModule("pkg.mineral.metal.lead");
        idx.ready();
        assertModuleBinding("pkg");
        assertModuleBinding("pkg.plant");
        assertModuleBinding("pkg.plant.poison");
        assertModuleBinding("pkg.plant.poison.eggplant");

        String src = getSource("pkg/mineral/metal/lead.py");
        assertReference("pkg", nthIndexOf(src, "pkg", 1));
        assertReference("pkg", nthIndexOf(src, "pkg", 2));

        assertReference("pkg.plant", nthIndexOf(src, "plant", 1));
        assertReference("pkg.plant", nthIndexOf(src, ".plant", 2) + 1);

        assertReference("pkg.plant.poison", nthIndexOf(src, "poison", 1));
        assertReference("pkg.plant.poison", nthIndexOf(src, ".poison", 2) + 1);

        assertReference("pkg.plant.poison.eggplant", nthIndexOf(src, "eggplant", 1));
        assertReference("pkg.plant.poison.eggplant", nthIndexOf(src, ".eggplant", 2) + 1);
    }

    public void testAbsoluteImportAs() throws Exception {
        idx.loadModule("pkg.mineral.metal.iron");
        idx.ready();
        assertModuleBinding("pkg");
        assertModuleBinding("pkg.mineral");
        assertModuleBinding("pkg.mineral.metal");
        assertModuleBinding("pkg.mineral.metal.iron");
        assertModuleBinding("pkg.plant");
        assertModuleBinding("pkg.plant.poison");
        assertModuleBinding("pkg.plant.poison.eggplant");

        String adjectives = "pkg.plant.poison.eggplant.adjectives";
        assertScopeBinding(adjectives);

        String aubergine = "pkg.mineral.metal.iron.aubergine";
        assertScopeBinding(aubergine);
        assertBindingType(aubergine, "pkg.plant.poison.eggplant");

        String src = getSource("pkg/mineral/metal/iron.py");
        assertReference("pkg", src.indexOf("pkg"));
        assertReference("pkg.plant", src.indexOf("plant"));
        assertReference("pkg.plant.poison", src.indexOf("poison"));
        assertReference("pkg.plant.poison.eggplant", src.indexOf("eggplant"));
        assertReference(aubergine, nthIndexOf(src, "aubergine", 2));
        assertReference(adjectives, src.indexOf("adjectives"));
    }

    public void testImportFrom() throws Exception {
        idx.loadModule("pkg.other.color.white");
        idx.ready();
        String src = getSource("pkg/other/color/white.py");
        assertReference("pkg.other.color.red", src.indexOf("red"));
        assertReference("pkg.other.color.green", src.indexOf("green"));
        assertReference("pkg.other.color.blue", src.indexOf("blue"));

        assertReference("pkg.other.color.red.r", src.indexOf("r as"), 1);
        assertReference("pkg.other.color.blue.b", src.indexOf("b as"), 1);

        assertReference("pkg.other.color.red.r", src.indexOf("= R") + 2, 1);
        assertReference("pkg.other.color.green.g", src.indexOf("g #"), 1);
        assertReference("pkg.other.color.blue.b", src.indexOf("= B") + 2, 1);
    }

    public void testImportStar() throws Exception {
        idx.loadModule("pkg.other.color.crimson");
        idx.ready();
        String src = getSource("pkg/other/color/crimson.py");
        assertReference("pkg.other.color.red.r", src.indexOf("r,"), 1);
        assertReference("pkg.other.color.red.g", src.indexOf("g,"), 1);
        assertReference("pkg.other.color.red.b", src.indexOf("b"), 1);
    }

    public void testImportStarAll() throws Exception {
        idx.loadModule("pkg.misc.moduleB");
        idx.ready();
        String src = getSource("pkg/misc/moduleB.py");
        assertReference("pkg.misc.moduleA.a", src.indexOf("a #"), 1);
        assertReference("pkg.misc.moduleA.b", src.indexOf("b #"), 1);
        assertReference("pkg.misc.moduleA.c", src.indexOf("c #"), 1);

        assertNoReference("Should not have imported 'd'",
                          "pkg.misc.moduleA.d", src.indexOf("d #"), 1);
    }

    public void testImportFromInitPy() throws Exception {
        idx.loadModule("pkg.animal");
        idx.ready();
        assertModuleBinding("pkg");
        assertModuleBinding("pkg.animal");
        assertModuleBinding("pkg.animal.animaltest");
        assertScopeBinding("pkg.animal.success");
        assertScopeBinding("pkg.animal.animaltest.living");
    }

    // // Tests to add:
    // //  - import inside a function; check that names are VARIABLE (not SCOPE)

    // public void finishme_testModuleDictMerging() throws Exception {
    //     // goal is to test this case:
    //     //  mod1.py:
    //     //    a = 1
    //     //  mod2.py:
    //     //   import mod1
    //     //   def test():
    //     //     print mod1.b  # at this point mod1.b is an unknown attr of mod1
    //     //  mod3.py:
    //     //   import mod1
    //     //   mod1.b = 2      # at this later point it gets defined
    //     //  test:
    //     //   load mod1, mod2, mod3
    //     //  => assert that in mod2.py, mod1.b refers to the definition in mod3.py
    // }

    // test creating temp definition and then re-resolving it
    public void testTempName() throws Exception {
        String src = index(
            "tmpname.py",
            "def purge():",
            "  cache.clear()",
            "cache = {}");
        assertScopeBinding("tmpname.cache");
        assertBindingType("tmpname.cache", NDictType.class);
        assertDefinition("tmpname.cache", "cache", src.lastIndexOf("cache"));

        assertReference("tmpname.cache", src.indexOf("cache"));
        assertNoDefinition("Temp-def should have been replaced",
                           "tmpname.cache", src.indexOf("cache"), "cache".length());

        assertCall("__builtin__.dict.clear", src.lastIndexOf("clear"));
    }

    public void testTempAttr() throws Exception {
        String src = index(
            "tmpattr.py",
            "x = app.usage",
            "app.usage = 'hi'");
        assertScopeBinding("tmpattr.x");
        assertScopeBinding("tmpattr.app");
        assertAttributeBinding("tmpattr.app.usage");
        assertStringType("tmpattr.app.usage");
        assertStringType("tmpattr.x");
        assertDefinition("tmpattr.app.usage", src.lastIndexOf("usage"));
        assertReference("tmpattr.app.usage", src.indexOf("usage"));
    }

    public void testTempAttrOnParam() throws Exception {
        String src = index(
            "tmpattr_param.py",
            "def foo(x):",
            "  x.hello = 'hi'",
            "def bar(y=None):",
            "  y.hello = 'hola'");
        assertFunctionBinding("tmpattr_param.foo");
        assertParamBinding("tmpattr_param.foo@x");
        assertAttributeBinding("tmpattr_param.foo@x.hello");
        assertStringType("tmpattr_param.foo@x.hello");
        assertReference("tmpattr_param.foo@x", src.indexOf("x.hello"), 1);

        assertFunctionBinding("tmpattr_param.bar");
        assertParamBinding("tmpattr_param.bar@y");
        assertAttributeBinding("tmpattr_param.bar@y.hello");
        assertStringType("tmpattr_param.bar@y.hello");
        assertReference("tmpattr_param.bar@y", src.indexOf("y.hello"), 1);
    }

    public void testParamDefaultLambdaBinding() throws Exception {
        String src = index(
            "test.py",
            "def foo(arg=lambda name: name + '!'):",
            "  x = arg('hi')");
        assertFunctionBinding("test.foo");
        assertParamBinding("test.foo@arg");
        assertFunctionBinding("test.lambda%1");
        assertParamBinding("test.lambda%1@name");
        assertReference("test.lambda%1@name", src.lastIndexOf("name"));
        assertCall("test.foo@arg", src.lastIndexOf("arg"));
        assertStringType("test.foo&x");
    }

    public void testNestedLambdaParam() throws Exception {
        String src = index(
            "test.py",
            "def util(create):",
            "  return create()",
            "z = lambda:util(create=lambda: str())",
            "y = z()()");

        assertScopeBinding("test.z");
        assertFunctionBinding("test.lambda%1&lambda%1");

        // XXX:  will require inferring across calls
        // assertStringType("test.y");
    }

    public void testReassignAttrOfUnknown() throws Exception {
        // This test has broken surprisingly often, so don't change it.
        String src = index(
            "reassign.py",
            "app.foo = 'hello'",
            "app.foo = 2");
        assertScopeBinding("reassign.app");
        NBinding nb = assertAttributeBinding("reassign.app.foo");
        NType type = nb.getType();
        assertTrue(type.isUnionType());
        Set<NType> types = ((NUnionType)type).getTypes();
        assertEquals(2, types.size());
        assertTrue(types.contains(idx.builtins.BaseStr));
        assertTrue(types.contains(idx.builtins.BaseNum));
    }

    public void testRefToProvisionalBinding() throws Exception {
        String src = index(
            "provisional.py",
            "for a in []:",
            "  a.dump()",
            "for a in []:",
            "  a.dump()");
        assertModuleBinding("provisional");
        assertScopeBinding("provisional.a");
        assertNoBinding("provisional.a.dump");
    }

    public void testRefToProvisionalBindingNewType() throws Exception {
        String src = index(
            "provisional.py",
            "for b in []:",
            "  b.dump()",
            "for b in ():",
            "  b.dump()");
        assertModuleBinding("provisional");
        assertScopeBinding("provisional.b");
        assertNoBinding("provisional.b.dump");
    }

    // http://www.python.org/dev/peps/pep-0227
    public void testSkipClassScope() throws Exception {
        String src = index(
            "skipclass.py",
            "def aa():",
            "  xx = 'foo'",
            "  class bb:",
            "    xx = 10",
            "    def cc(self):",
            "      print bb.xx",
            "      print xx");
        assertReference("skipclass.aa&bb.xx", nthIndexOf(src, "xx", 3));
        assertReference("skipclass.aa&xx", nthIndexOf(src, "xx", 4));
    }

    public void testLambdaArgs() throws Exception {
        String src = index(
            "lambda_args.py",
            "y = lambda x='hi': x.upper()",
            "y = lambda x='there': x.lower()");
        assertScopeBinding("lambda_args.y");

        assertFunctionBinding("lambda_args.lambda%1");
        assertParamBinding("lambda_args.lambda%1@x");
        assertStringType("lambda_args.lambda%1@x");
        assertReference("lambda_args.lambda%1@x", nthIndexOf(src, "x", 2));
        assertCall("__builtin__.str.upper", src.indexOf("upper"));

        assertFunctionBinding("lambda_args.lambda%2");
        assertParamBinding("lambda_args.lambda%1@x");
        assertReference("lambda_args.lambda%2@x", nthIndexOf(src, "x", 4));
        assertCall("__builtin__.str.lower", src.indexOf("lower"));
    }

    public void testFunArgs() throws Exception {
        String src = index(
            "funargs.py",
            "def foo(x, y='hi'):",
            "  z = 9",
            "  return x + y.upper() + z");
        assertFunctionBinding("funargs.foo");

        assertParamBinding("funargs.foo@x");
        assertReference("funargs.foo@x", nthIndexOf(src, "x", 2));

        assertParamBinding("funargs.foo@y");
        assertStringType("funargs.foo@y");
        assertReference("funargs.foo@y", nthIndexOf(src, "y", 2));

        assertCall("__builtin__.str.upper", src.indexOf("upper"));

        assertVariableBinding("funargs.foo&z");
        assertReference("funargs.foo&z", nthIndexOf(src, "z", 2));
    }

    public void testDatetime() throws Exception {
        String src = index(
            "date_time.py",
            "from datetime import datetime as dt",
            "import datetime",
            "now = dt.now()",
            "d = now.date()",
            "tz = now.tzinfo");
        assertModuleBinding("datetime");
        assertClassBinding("datetime.datetime");
        assertMethodBinding("datetime.datetime.date");

        assertReference("datetime", nthIndexOf(src, "datetime", 1));
        assertReference("datetime.datetime", nthIndexOf(src, "datetime", 2));
        assertReference("datetime.datetime", nthIndexOf(src, "dt", 1), 2);
        assertReference("datetime.datetime", nthIndexOf(src, "dt", 2), 2);
        assertReference("datetime", nthIndexOf(src, "datetime", 3));
        assertCall("datetime.datetime.now", nthIndexOf(src, "now", 2));
        assertCall("datetime.datetime.date", nthIndexOf(src, "date()", 1));
        assertReference("datetime.time.tzinfo", nthIndexOf(src, "tzinfo", 1));
        assertBindingType("date_time.tz", "datetime.tzinfo");
    }

    public void testUnpackList() throws Exception {
        index("unpacklist.py",
              "a = [1, 2]",
              "(b, c) = [3, 4]",
              "[d, e] = ['hi', 'there']");
        assertScopeBinding("unpacklist.a");
        assertScopeBinding("unpacklist.b");
        assertScopeBinding("unpacklist.c");
        assertScopeBinding("unpacklist.d");
        assertScopeBinding("unpacklist.e");
        assertListType("unpacklist.a", "__builtin__.float");
        assertNumType("unpacklist.b");
        assertNumType("unpacklist.c");
        assertStringType("unpacklist.d");
        assertStringType("unpacklist.e");
    }

    public void testStringSlice() throws Exception {
        String src = index(
            "slicestring.py",
            "a = 'hello'[2]",
            "b = 'hello'[2:4]",
            "test = 'testing'",
            "test[-3:].lower()");
        assertScopeBinding("slicestring.a");
        assertScopeBinding("slicestring.b");
        assertStringType("slicestring.a");
        assertStringType("slicestring.b");
        assertCall("__builtin__.str.lower", src.lastIndexOf("lower"));
    }

    public void testUnionStringSliceTempAttr() throws Exception {
        String src = index(
            "tmpattr_slice.py",
            "def foo(filename):",
            "  module = filename or '<unknown>'",
            "  module[-3:].lower()");
        assertCall("__builtin__.str.lower", src.lastIndexOf("lower"));
    }

    public void testSelfBinding() throws Exception {
        String src = index(
            "selfish.py",
            "class Foo():",
            "  def hello(self):",
            "    print self");
        assertClassBinding("selfish.Foo");
        assertMethodBinding("selfish.Foo.hello");
        assertParamBinding("selfish.Foo.hello@self");
        assertDefinition("selfish.Foo.hello@self", nthIndexOf(src, "self", 1));
        assertReference("selfish.Foo.hello@self", nthIndexOf(src, "self", 2));
        assertBindingType("selfish.Foo.hello@self", "selfish.Foo");
    }

    public void testInstanceAttrs() throws Exception {
        String src = index(
            "attr.py",
            "class Foo():",
            "  def __init__(self):",
            "    self.elts = []",
            "  def add(self, item):",
            "    self.elts.append(item)");
        assertClassBinding("attr.Foo");
        assertConstructorBinding("attr.Foo.__init__");
        assertParamBinding("attr.Foo.__init__@self");
        assertDefinition("attr.Foo.__init__@self", nthIndexOf(src, "self", 1));
        assertReference("attr.Foo.__init__@self", nthIndexOf(src, "self", 2));
        assertBindingType("attr.Foo.__init__@self", "attr.Foo");

        assertAttributeBinding("attr.Foo.elts");
        assertListType("attr.Foo.elts");

        assertMethodBinding("attr.Foo.add");
        assertParamBinding("attr.Foo.add@self");
        assertBindingType("attr.Foo.add@self", "attr.Foo");
        assertParamBinding("attr.Foo.add@item");
        assertReference("attr.Foo.add@self", nthIndexOf(src, "self", 4));
        assertReference("attr.Foo.elts", nthIndexOf(src, "elts", 2));
        assertCall("__builtin__.list.append", src.indexOf("append"));
        assertReference("attr.Foo.add@item", src.lastIndexOf("item"));
    }

    public void testInstanceAttrsWithStdLib() throws Exception {
        includeStandardLibrary();
        String src = index(
            "dice.py",
            "import random",
            "class Dice(object):",
            "  def __init__(self):",
            "    self.__random = random.Random()",
            "  def set_seed(self, seed):",
            "    self.__random.seed(seed)");
        assertModuleBinding("random");
        NBinding r = assertClassBinding("random.Random");
        assertFalse(r.isBuiltin());

        assertReference("random", nthIndexOf(src, "random", 3));
        assertConstructed("random.Random", src.indexOf("Random"));

        assertClassBinding("dice.Dice");
        assertReference("__builtin__.object", src.indexOf("object"));

        assertConstructorBinding("dice.Dice.__init__");
        assertParamBinding("dice.Dice.__init__@self");
        assertDefinition("dice.Dice.__init__@self", nthIndexOf(src, "self", 1));
        assertReference("dice.Dice.__init__@self", nthIndexOf(src, "self", 2));

        assertBindingType("dice.Dice.__init__@self", "dice.Dice");

        assertAttributeBinding("dice.Dice.__random");
        assertInstanceType("dice.Dice.__random", "random.Random");

        assertMethodBinding("dice.Dice.set_seed");
        assertParamBinding("dice.Dice.set_seed@self");
        assertBindingType("dice.Dice.set_seed@self", "dice.Dice");
        assertParamBinding("dice.Dice.set_seed@seed");

        assertReference("dice.Dice.set_seed@self", nthIndexOf(src, "self", 4));
        assertReference("dice.Dice.__random", nthIndexOf(src, "__random", 2));
        assertCall("random.Random.seed", nthIndexOf(src, "seed", 3));
        assertReference("dice.Dice.set_seed@seed", src.lastIndexOf("seed"));
    }

    public void testOsPath() throws Exception {
        String src = index(
            "test.py",
            "from os import path",
            "print path.devnull",
            "base, ext = path.split('/foo/bar/baz.py')",
            "print ext.endswith('py')");
        assertReference("os.path.devnull", src.indexOf("devnull"));
        assertStringType("os.path.devnull");
        assertStringType("test.base");
        assertStringType("test.ext");
        assertCall("os.path.split", src.indexOf("split"));
        assertCall("__builtin__.str.endswith", src.indexOf("endswith"));
    }

    public void testImportOsPath() throws Exception {
        String src = index(
            "test.py",
            "import os.path",
            "print os.path.devnull");
        assertReference("os", nthIndexOf(src, "os", 1));
        assertReference("os", nthIndexOf(src, "os", 2));
        assertReference("os.path", nthIndexOf(src, "path", 1));
        assertReference("os.path", nthIndexOf(src, "path", 2));
        assertReference("os.path.devnull", src.indexOf("devnull"));
    }

    public void testExceptionsModule() throws Exception {
        String src = index(
            "test.py",
            "import exceptions",
            "raise exceptions.NotImplementedError");
        assertModuleBinding("exceptions");
        assertClassBinding("exceptions.NotImplementedError");
        assertReference("exceptions.NotImplementedError", src.indexOf("Not"));
    }

    public void testDupFunctionDecl() throws Exception {
        String src = index(
            "test.py",
            "if x:",
            "  def a(args):",
            "    print args",
            "elif y:",
            "  def a(args):",
            "    print args");
        assertFunctionBinding("test.a");
        assertParamBinding("test.a@args");
    }

    public void testResolveExportedNames() throws Exception {
        String src = index(
            "test.py",
            "__all__ = ['foo', 'bar' + 'baz', 'one', 'two']",
            "def foo(x):",
            "  return x",
            "bar = 6",
            "baz = 7",
            "barbaz = 8",
            "one = 'hi'",
            "two = 'there'");
        assertReference("test.foo", src.indexOf("'foo"), 5);
        assertReference("test.one", src.indexOf("'one"), 5);
        assertReference("test.two", src.indexOf("'two"), 5);

        assertNoReference("Should not have referenced 'bar'",
                          "test.bar", src.indexOf("bar"), 3);
    }

    public void testImportFromPlusAssign() throws Exception {
        String src = index(
            "test.py",
            "from os import sep",
            "os = 10",
            "print os");
        assertModuleBinding("os");
        assertReference("os", src.indexOf("os"));
        assertNoDefinition("Import-from should not introduce a definition",
                           "test.os", src.indexOf("os"), "os".length());
        assertDefinition("test.os", nthIndexOf(src, "os", 2));
        assertNumType("test.os");
        assertReference("test.os", src.lastIndexOf("os"));
    }

    public void testCircularTypeFunAndTuple() throws Exception {
        String src = index(
            "test.py",
            "def foo():",
            "  return (foo,)");
        assertFunctionBinding("test.foo");
        NType ftype = idx.lookupQnameType("test.foo");
        assertTrue(ftype instanceof NFuncType);
        NType rtype = ftype.asFuncType().getReturnType();
        assertTrue(rtype instanceof NTupleType);
        assertEquals(1, rtype.asTupleType().getElementTypes().size());
        assertEquals(ftype, rtype.asTupleType().getElementTypes().get(0));
        assertEquals("<FuncType=#1:_:<TupleType:[<#1>]>>", ftype.toString());
    }

    public void testCircularTypeXInOwnList() throws Exception {
        String src = index(
            "test.py",
            "x = (2,)",
            "y = [x]",
            "x = y");
        NType xtype = idx.lookupQnameType("test.x");
        assertTrue(xtype instanceof NUnionType);

        // Jump through some hoops to allow for either order in the union.
        Set<NType> types = xtype.asUnionType().getTypes();
        assertEquals(2, types.size());
        NType[] array = types.toArray(new NType[2]);
        boolean array0List = array[0] instanceof NListType;
        boolean array1List = array[1] instanceof NListType;

        assertTrue(array0List || array1List);
        int other = array0List ? 1 : 0;
        assertTrue("Expected tuple: " + array[other], array[other].isTupleType());
        assertEquals(1, array[other].asTupleType().getElementTypes().size());
        assertEquals(idx.builtins.BaseNum, array[other].asTupleType().getElementTypes().get(0));

        String s = xtype.toString();
        int index = s.indexOf("<TupleType=#");
        assertTrue(index != -1);
        int spot = index + "<TupleType=#".length();
        int num = Integer.parseInt(s.substring(spot, spot+1));

        String ttype = "<TupleType=#" + num + ":[<ClassType:float>]>";
        String ref = "<#" + num + ">";

        if (array0List) {
            // union(list(unknown(tuple)), ref)
            assertEquals("<UnionType:[<ListType:" + ttype + ">," + ref + "]>", s);
        } else {
            // union(tuple, list(unknown(ref)))
            assertEquals("<UnionType:[" + ttype + ",<ListType:" + ref + ">]>", s);
        }
    }

    public void testFunReturn() throws Exception {
        // This use case used to extend the function return type by one
        // wrapped NUnknownType for each static invocation of the function.
        String src = index(
            "fret.py",
            "def foo(x): return x",
            "a = foo('a')",
            "b = foo('b')",
            "c = foo('c')");
        NType ftype = idx.lookupQnameType("fret.foo");
        assertEquals("<FuncType:_:<UnknownType:null>>", ftype.toString());
        NType ctype = idx.lookupQnameType("fret.c");
        assertEquals(ctype.follow(), ftype.asFuncType().getReturnType());
    }

    public void testListCompForIn() throws Exception {
        String src = index(
            "listforin.py",
            "[line for line in ['foo']]");
        assertStringType("listforin.line");
    }

    public void testNoAddToBuiltin() throws Exception {
        String src = index(
            "nob.py",
            "x = [line.rstrip() + '\\n' for line in ['a ']]");
        assertStringType("nob.line");
        assertCall("__builtin__.str.rstrip", src.indexOf("rstrip"));
        assertNoBinding("__builtin__.list.rstrip");
        assertListType("nob.x", "__builtin__.str");
    }

    public void testDecoratorSyntax() throws Exception {
        String deco1 = "@deco1";
        String deco2 = "@deco2 ('yargh')";
        String src = index(
            "deco.py",
            deco1,
            deco2,
            "def foo(): pass");
        assertFunctionBinding("deco.foo");
        NModule m = idx.getAstForFile("deco.py");
        assertNotNull(m);

        NNode obj = m.body.seq.get(0);
        assertTrue(obj instanceof NFunctionDef);
        NFunctionDef f = (NFunctionDef)obj;
        List<NNode> decos = f.getDecoratorList();
        assertNotNull(decos);
        assertEquals(2, decos.size());
        assertTrue(decos.get(0) instanceof NName);

        NName d1 = (NName)decos.get(0);
        assertEquals(nthIndexOf(src, "deco1", 1), d1.start());
        assertEquals("deco1".length(), d1.length());
        assertEquals("deco1", d1.id);

        assertTrue(decos.get(1) instanceof NCall);
        NCall d2 = (NCall)decos.get(1);
        assertTrue(d2.func instanceof NName);
        assertEquals("deco2", ((NName)d2.func).id);
    }

    public void testBasicDecoratorSyntax() throws Exception {
        String src = index(
            "deco.py",
            "def deco1(func): print 'hello'; return func",
            "@deco1()",
            "def foo(): pass");
        assertFunctionBinding("deco.deco1");
        assertFunctionBinding("deco.foo");
        assertCall("deco.deco1", nthIndexOf(src, "deco1", 2));
    }
}
