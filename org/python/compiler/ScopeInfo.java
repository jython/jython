// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import java.util.*;
import org.python.parser.SimpleNode;

public class ScopeInfo extends Object implements ScopeConstants {

    public SimpleNode scope_node;
    public String scope_name;
    public int level;
    public int func_level;
    public int list_comprehension_count;

    public void dump() { // for debugging
        if (org.python.core.Options.verbose < org.python.core.Py.DEBUG)
            return;
        for(int i=0; i<level; i++) System.err.print(' ');
        System.err.print(((kind != CLASSSCOPE)?scope_name:"class "+
                         scope_name)+": ");
        for (Enumeration e = tbl.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            SymInfo info = (SymInfo)tbl.get(name);
            int flags = info.flags;
            System.err.print(name);
            if ((flags&BOUND) != 0) System.err.print('=');
            // func scope global (affect nested scopes)
            // vs. class scope global
            if ((flags&NGLOBAL) != 0) System.err.print('G');
            else if ((flags&CLASS_GLOBAL) != 0) System.err.print('g');
            if ((flags&PARAM) != 0) System.err.print('P');
            else if ((flags&FROM_PARAM) != 0) System.err.print('p');
            if ((flags&CELL) != 0) System.err.print('!');
            if ((flags&FREE) != 0) System.err.print(",f");
            System.err.print(" ");
        }
        System.err.println();
    }

    public ScopeInfo(String name, SimpleNode node, int level, int kind,
                     int func_level, ArgListCompiler ac) {
        scope_name = name;
        scope_node = node;
        this.level = level;
        this.kind = kind;
        this.func_level = func_level;
        this.ac = ac;
    }

    public int kind;

    public boolean unqual_exec;
    public boolean exec;
    public boolean from_import_star;

    public ArgListCompiler ac;

    public Hashtable tbl = new Hashtable();
    public Vector names = new Vector();

    public int addGlobal(String name) {
        // global kind = func vs. class
        int global = kind==CLASSSCOPE?CLASS_GLOBAL:NGLOBAL;
        SymInfo info = (SymInfo)tbl.get(name);
        if (info == null) {
            tbl.put(name,new SymInfo(global|BOUND));
            return -1;
        }
        int prev = info.flags;
        info.flags |= global|BOUND;
        return prev;
    }

    public int local = 0;

    public void addParam(String name) {
//System.out.println("addParam " + name);
        tbl.put(name, new SymInfo(PARAM|BOUND,local++));
        names.addElement(name);
    }

    public void markFromParam() {
        for (Enumeration e=tbl.elements(); e.hasMoreElements(); ) {
            SymInfo info = (SymInfo)e.nextElement();
            info.flags |= FROM_PARAM;
        }
    }

    public void addBound(String name) {
        SymInfo info = (SymInfo)tbl.get(name);
        if (info == null) {
            tbl.put(name, new SymInfo(BOUND));
            return;
        }
        info.flags |= BOUND;
    }

    public void addUsed(String name) {
        if (tbl.get(name) == null) {
            tbl.put(name, new SymInfo(0));
            return;
        }
    }

    private final static Object PRESENT = new Object();

    public Hashtable inner_free = new Hashtable();

    public Vector cellvars = new Vector();

    public Vector jy_paramcells = new Vector();

    public int jy_npurecell;

    public int cell;

    public void cook(ScopeInfo up,CompilationContext ctxt) throws Exception {
        if (up == null) return; // top level => nop

        boolean func = kind == FUNCSCOPE;
        Vector purecells = new Vector();
        cell = 0;
        boolean some_inner_free = inner_free.size() > 0;

        for (Enumeration e = inner_free.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            SymInfo info = (SymInfo)tbl.get(name);
            if (info == null) {
                tbl.put(name,new SymInfo(FREE));
                continue;
            }
            int flags = info.flags;
            if (func) {
                // not func global and bound ?
                if ((flags&NGLOBAL) == 0 && (flags&BOUND) != 0) {
                    info.flags |= CELL;
                    if ((info.flags&PARAM) != 0)
                        jy_paramcells.addElement(name);
                    cellvars.addElement(name);
                    info.env_index = cell++;
                    if ((flags&PARAM) == 0) purecells.addElement(name);
                    continue;
                }
            } else {
                info.flags |= FREE;
            }
        }
        boolean some_free = false;

        boolean nested = up.kind != TOPSCOPE;
        for (Enumeration e = tbl.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            SymInfo info = (SymInfo)tbl.get(name);
            int flags = info.flags;
            if (nested && (flags&FREE) != 0) up.inner_free.put(name,PRESENT);
            if ((flags&(GLOBAL|PARAM|CELL)) == 0) {
                if ((flags&BOUND) != 0) { // ?? only func
                    // System.err.println("local: "+name);
                    names.addElement(name);
                    info.locals_index = local++;
                    continue;
                }
                info.flags |= FREE;
                some_free = true;
                if (nested) up.inner_free.put(name,PRESENT);
            }
        }
        if ((jy_npurecell = purecells.size()) > 0) {
            int sz = purecells.size();
            for (int i = 0; i < sz; i++) {
                names.addElement(purecells.elementAt(i));
            }
        }
        if ((unqual_exec || from_import_star)) {
            if(some_inner_free) dynastuff_trouble(true, ctxt);
            else if(func_level > 1 && some_free)
                dynastuff_trouble(false, ctxt);
        }

    }

    private void dynastuff_trouble(boolean inner_free,
                                   CompilationContext ctxt) throws Exception {
        String illegal;
        if (unqual_exec && from_import_star)
            illegal = "function '"+scope_name+
                      "' uses import * and bare exec, which are illegal";
        else if (unqual_exec)
            illegal = "unqualified exec is not allowed in function '"+
                      scope_name+"'";
        else
            illegal = "import * is not allowed in function '"+scope_name+"'";
        String why;
        if (inner_free)
            why = " because it contains a function with free variables";
        else
            why = " because it contains free variables";
        ctxt.error(illegal + why, true, scope_node);
    }

    public Vector freevars = new Vector();

    public void setup_closure(ScopeInfo up) {
        int free = cell; // env = cell...,free...
        Hashtable up_tbl = up.tbl;
        boolean nested = up.kind != TOPSCOPE;
        for (Enumeration e = tbl.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            SymInfo info = (SymInfo)tbl.get(name);
            int flags = info.flags;
            if ((flags&FREE) != 0) {
                SymInfo up_info = (SymInfo)up_tbl.get(name);
                // ?? differs from CPython -- what is the intended behaviour?
                if (up_info != null) {
                    int up_flags = up_info.flags;
                    if ((up_flags&(CELL|FREE)) != 0) {
                        info.env_index = free++;
                        freevars.addElement(name);
                        continue;
                    }
                    // ! func global affect nested scopes
                    if (nested && (up_flags&NGLOBAL) != 0) {
                        info.flags = NGLOBAL|BOUND;
                        continue;
                    }
                }
                info.flags &= ~FREE;
            }
        }

    }
}
