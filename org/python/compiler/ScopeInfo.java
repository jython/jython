// (C) Copyright 2001 Samuele Pedroni

package org.python.compiler;

import java.util.*;
import org.python.parser.SimpleNode;

public class ScopeInfo extends Object implements ScopeConstants {

    public SimpleNode scope_node;
    public String scope_name;
    public int level;
    public int func_level;

    public void dump() { // for debugging
        if (org.python.core.Options.verbose < org.python.core.Py.DEBUG) return;
        for(int i=0; i<level; i++) System.err.print(' ');
        System.err.print(((kind != CLASSSCOPE)?scope_name:"class "+scope_name)+": ");
        for (Enumeration e = tbl.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            SymInfo info = (SymInfo)tbl.get(name);
            int flags = info.flags;
            System.err.print(name);
            if ((flags&BOUND) != 0) System.err.print('=');
            if ((flags&NGLOBAL) != 0) System.err.print('G'); else // func scope global (affect nested scopes) 
            if ((flags&CLASS_GLOBAL) != 0) System.err.print('g'); // vs. class scope global
            if ((flags&PARAM) != 0) System.err.print('P'); else
            if ((flags&FROM_PARAM) != 0) System.err.print('p');
            if ((flags&CELL) != 0) System.err.print('!');
            if ((flags&FREE) != 0) System.err.print(",f");
            System.err.print(" ");
        }
        System.err.println(); 
    }
        
    public ScopeInfo(String name,SimpleNode node,int level,int kind,int func_level,ArgListCompiler ac,boolean nested_scopes) {
        scope_name = name;
        scope_node = node;
        this.level = level;
        this.kind = kind;
        this.func_level = func_level;
        this.ac = ac;
        this.nested_scopes = nested_scopes;
    }
    
    public int kind;
    public boolean nested_scopes;
    
    public boolean unqual_exec;
    public boolean exec;
    public boolean from_import_star;
    
    public ArgListCompiler ac;
    
    public Hashtable tbl = new Hashtable();
    public Vector names = new Vector();
    
    public int addGlobal(String name) {
        int global = kind==CLASSSCOPE?CLASS_GLOBAL:NGLOBAL; // global kind = func vs. class
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
    
    public Vector xxx_paramcells = new Vector();
    
    public int xxx_npurecell; // ?? xxx
    
    public int cell;
    
    public void cook(ScopeInfo up,CodeCompiler ctxt) throws Exception {
        if (up == null) return; // top level => nop
        
        boolean nested_scopes = this.nested_scopes;
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
                if ((flags&NGLOBAL) == 0 && (flags&BOUND) != 0) { // not func global and bound ?
                    if (nested_scopes) {
                        info.flags |= CELL;
                        if ((info.flags&PARAM) != 0) xxx_paramcells.addElement(name);                        
                        cellvars.addElement(name);
                        info.env_index = cell++;
                        if ((flags&PARAM) == 0) purecells.addElement(name);
                        continue;
                    }
                    ctxt.error("local name '"+name+"' in '"+scope_name+"' shadows use as global in nested scopes",false,scope_node);
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
        if ((xxx_npurecell = purecells.size()) > 0) {
            int sz = purecells.size();
            for (int i = 0; i < sz; i++) {
                names.addElement(purecells.elementAt(i));
            }
        }
        if ((unqual_exec || from_import_star)) {
            if(some_inner_free) dynastuff_trouble(true, ctxt);
            else if(func_level > 1 && some_free) dynastuff_trouble(false, ctxt);
        }
        
    }

    private void dynastuff_trouble(boolean inner_free,CodeCompiler ctxt) throws Exception {
        String illegal;
        if (unqual_exec && from_import_star)
         illegal = "function '"+scope_name+"' uses import * and bare exec, which are illegal";
        else if (unqual_exec)
         illegal = "unqualified exec is not allowed in function '"+scope_name+"'";
        else illegal = "import * is not allowed in function '"+scope_name+"'";
        String why;
        if (inner_free)
         why = " because it contains a function with free variables";
        else
         why = " because it contains free variables";
        ctxt.error(illegal + why ,nested_scopes,scope_node);
    }
    
    public Vector freevars = new Vector();

    public void setup_closure(ScopeInfo up) {
        if (!nested_scopes) return;
        int free = cell; // env = cell...,free...
        Hashtable up_tbl = up.tbl;
        boolean nested = up.kind != TOPSCOPE;
        for (Enumeration e = tbl.keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            SymInfo info = (SymInfo)tbl.get(name);
            int flags = info.flags;
            if ((flags&FREE) != 0) {
                SymInfo up_info = (SymInfo)up_tbl.get(name);
                if (up_info != null) { // ?? differs from CPython -- what is the intended behaviour?
                    int up_flags = up_info.flags;
                    if ((up_flags&(CELL|FREE)) != 0) {
                        info.env_index = free++;
                        freevars.addElement(name);
                        continue;
                    }
                    if (nested && (up_flags&NGLOBAL) != 0) { // ! func global affect nested scopes
                        info.flags = NGLOBAL|BOUND;
                        continue;
                    } 
                }
                info.flags &= ~FREE;
            }
        }
        
    }
        
}
