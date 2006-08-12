package org.python.parser;

import org.python.parser.ast.*;
import org.python.core.PyObject;

public class TreeBuilder implements PythonGrammarTreeConstants {
    private JJTPythonGrammarState stack;
    CtxVisitor ctx;

    public TreeBuilder(JJTPythonGrammarState stack) {
        this.stack = stack;
        ctx = new CtxVisitor();
    }

    private stmtType[] makeStmts(int l) {
        stmtType[] stmts = new stmtType[l];
        for (int i = l-1; i >= 0; i--) {
            stmts[i] = (stmtType) stack.popNode();
        }
        return stmts;
    }

    private stmtType[] popSuite() {
        return ((Suite) popNode()).body;
    }

    private exprType[] makeExprs() {
        if (stack.nodeArity() > 0 && peekNode().getId() == JJTCOMMA)
            popNode();
        return makeExprs(stack.nodeArity());
    }

    private exprType[] makeExprs(int l) {
        exprType[] exprs = new exprType[l];
        for (int i = l-1; i >= 0; i--) {
            exprs[i] = makeExpr();
        }
        return exprs;
    }

    private exprType makeExpr(SimpleNode node) {
        return (exprType) node;
    }

    private exprType makeExpr() {
        return makeExpr((SimpleNode) stack.popNode());
    }

    private String makeIdentifier() {
        return ((Name) stack.popNode()).id;
    }

    private String[] makeIdentifiers() {
        int l = stack.nodeArity();
        String[] ids = new String[l];
        for (int i = l - 1; i >= 0; i--) {
            ids[i] = makeIdentifier();
        }
        return ids;
    }

    private aliasType[] makeAliases() {
        return makeAliases(stack.nodeArity());
    }

    private aliasType[] makeAliases(int l) {
        aliasType[] aliases = new aliasType[l];
        for (int i = l-1; i >= 0; i--) {
            aliases[i] = (aliasType) stack.popNode();
        }
        return aliases;
    }

    private static SimpleNode[] nodes =
        new SimpleNode[PythonGrammarTreeConstants.jjtNodeName.length];

    public SimpleNode openNode(int id) {
        if (nodes[id] == null)
            nodes[id] = new IdentityNode(id);
        return nodes[id];
    }

    
    public SimpleNode closeNode(SimpleNode n, int arity) throws Exception {
        exprType value;
        exprType[] exprs;

        switch (n.getId()) {
        case -1:
            System.out.println("Illegal node");
        case JJTSINGLE_INPUT:
            return new Interactive(makeStmts(arity));
        case JJTFILE_INPUT:
            return new Module(makeStmts(arity));
        case JJTEVAL_INPUT:
            return new Expression(makeExpr());

        case JJTNAME:
            return new Name(n.getImage().toString(), Name.Load);
        case JJTNUM:
            return new Num((PyObject) n.getImage());
        case JJTSTRING:
            return new Str(n.getImage().toString());
        case JJTUNICODE:
            return new Unicode(n.getImage().toString());

        case JJTSUITE:
            stmtType[] stmts = new stmtType[arity];
            for (int i = arity-1; i >= 0; i--) {
                stmts[i] = (stmtType) popNode();
            }
            return new Suite(stmts);
        case JJTEXPR_STMT:
            value = makeExpr();
            if (arity > 1) {
                exprs = makeExprs(arity-1);
                ctx.setStore(exprs);
                return new Assign(exprs, value);
            } else {
                return new Expr(value);
            }
        case JJTINDEX_OP:
            sliceType slice = (sliceType) stack.popNode();
            value = makeExpr();
            return new Subscript(value, slice, Subscript.Load);
        case JJTDOT_OP:
            String attr = makeIdentifier();
            value = makeExpr();
            return new Attribute(value, attr, Attribute.Load);
        case JJTDEL_STMT:
            exprs = makeExprs(arity);
            ctx.setDelete(exprs);
            return new Delete(exprs);
        case JJTPRINT_STMT:
            boolean nl = true;
            if (stack.nodeArity() == 0)
                return new Print(null, null, true);
            if (peekNode().getId() == JJTCOMMA) {
                popNode();
                nl = false;
            }
            return new Print(null, makeExprs(), nl);
        case JJTPRINTEXT_STMT:
            nl = true;
            if (peekNode().getId() == JJTCOMMA) {
                popNode();
                nl = false;
            }
            exprs = makeExprs(stack.nodeArity()-1);
            return new Print(makeExpr(), exprs, nl);
        case JJTFOR_STMT:
            stmtType[] orelse = null;
            if (stack.nodeArity() == 4)
                orelse = popSuite();
            stmtType[] body = popSuite();
            exprType iter = makeExpr();
            exprType target = makeExpr();
            ctx.setStore(target);
            return new For(target, iter, body, orelse);
        case JJTWHILE_STMT:
            orelse = null;
            if (stack.nodeArity() == 3)
                orelse = popSuite();
            body = popSuite();
            exprType test = makeExpr();
            return new While(test, body, orelse);
        case JJTIF_STMT:
            orelse = null;
            if (arity % 2 == 1)
                orelse = popSuite();
            body = popSuite();
            test = makeExpr();
            If last = new If(test, body, orelse);
            for (int i = 0; i < (arity / 2)-1; i++) {
                body = popSuite();
                test = makeExpr();
                last = new If(test, body, new stmtType[] { last });
            }
            return last;
        case JJTPASS_STMT:
            return new Pass();
        case JJTBREAK_STMT:
            return new Break();
        case JJTCONTINUE_STMT:
            return new Continue();
        case JJTFUNCDEF:
            body = popSuite();
            argumentsType arguments = makeArguments(arity - 2);
            String name = makeIdentifier();
            return new FunctionDef(name, arguments, body);
        case JJTDEFAULTARG:
            value = (arity == 1) ? null : makeExpr();
            return new DefaultArg(makeExpr(), value);
        case JJTEXTRAARGLIST:
            return new ExtraArg(makeIdentifier(), JJTEXTRAARGLIST);
        case JJTEXTRAKEYWORDLIST:
            return new ExtraArg(makeIdentifier(), JJTEXTRAKEYWORDLIST);
/*
        case JJTFPLIST:
            fpdefType[] list = new fpdefType[arity];
            for (int i = arity-1; i >= 0; i--) {
                list[i] = popFpdef();
            }
            return new FpList(list);
*/
        case JJTCLASSDEF:
            body = popSuite();
            exprType[] bases = makeExprs(stack.nodeArity() - 1);
            name = makeIdentifier();
            return new ClassDef(name, bases, body);
        case JJTRETURN_STMT:
            value = arity == 1 ? makeExpr() : null;
            return new Return(value);
        case JJTYIELD_STMT:
            return new Yield(makeExpr());
        case JJTRAISE_STMT:
            exprType tback = arity >= 3 ? makeExpr() : null;
            exprType inst = arity >= 2 ? makeExpr() : null;
            exprType type = arity >= 1 ? makeExpr() : null;
            return new Raise(type, inst, tback);
        case JJTGLOBAL_STMT:
            return new Global(makeIdentifiers());
        case JJTEXEC_STMT:
            exprType globals = arity >= 3 ? makeExpr() : null;
            exprType locals = arity >= 2 ? makeExpr() : null;
            value = makeExpr();
            return new Exec(value, locals, globals);
        case JJTASSERT_STMT:
            exprType msg = arity == 2 ? makeExpr() : null;
            test = makeExpr();
            return new Assert(test, msg);
        case JJTTRYFINALLY_STMT:
            orelse = popSuite();
            return new TryFinally(popSuite(), orelse);
        case JJTTRY_STMT:
            orelse = null;
            if (peekNode() instanceof Suite) {
                arity--;
                orelse = popSuite();
            }
            int l = arity - 1;
            excepthandlerType[] handlers = new excepthandlerType[l];
            for (int i = l - 1; i >= 0; i--) {
                handlers[i] = (excepthandlerType) popNode();
            }
            return new TryExcept(popSuite(), handlers, orelse);
        case JJTEXCEPT_CLAUSE:
            body = popSuite();
            exprType excname = arity == 3 ? makeExpr() : null;
            if (excname != null)    
                ctx.setStore(excname);
            type = arity >= 2 ? makeExpr() : null;
            return new excepthandlerType(type, excname, body);
        case JJTOR_BOOLEAN:
            return new BoolOp(BoolOp.Or, makeExprs());
        case JJTAND_BOOLEAN:
            return new BoolOp(BoolOp.And, makeExprs());
        case JJTCOMPARISION:
            l = arity / 2;
            exprType[] comparators = new exprType[l];
            int[] ops = new int[l];
            for (int i = l-1; i >= 0; i--) {
                comparators[i] = makeExpr();
                SimpleNode op = (SimpleNode) stack.popNode();
                switch (op.getId()) {
                case JJTLESS_CMP:          ops[i] = Compare.Lt; break;
                case JJTGREATER_CMP:       ops[i] = Compare.Gt; break;
                case JJTEQUAL_CMP:         ops[i] = Compare.Eq; break;
                case JJTGREATER_EQUAL_CMP: ops[i] = Compare.GtE; break;
                case JJTLESS_EQUAL_CMP:    ops[i] = Compare.LtE; break;
                case JJTNOTEQUAL_CMP:      ops[i] = Compare.NotEq; break;
                case JJTIN_CMP:            ops[i] = Compare.In; break;
                case JJTNOT_IN_CMP:        ops[i] = Compare.NotIn; break;
                case JJTIS_NOT_CMP:        ops[i] = Compare.IsNot; break;
                case JJTIS_CMP:            ops[i] = Compare.Is; break;
                default:
                    throw new RuntimeException("Unknown cmp op:" + op.getId());
                }
            }
            return new Compare(makeExpr(), ops, comparators);
        case JJTLESS_CMP:
        case JJTGREATER_CMP:
        case JJTEQUAL_CMP:
        case JJTGREATER_EQUAL_CMP:
        case JJTLESS_EQUAL_CMP:
        case JJTNOTEQUAL_CMP:
        case JJTIN_CMP:
        case JJTNOT_IN_CMP:
        case JJTIS_NOT_CMP:
        case JJTIS_CMP:
            return n;
        case JJTOR_2OP:
            return makeBinOp(BinOp.BitOr);
        case JJTXOR_2OP:
            return makeBinOp(BinOp.BitXor);
        case JJTAND_2OP:
            return makeBinOp(BinOp.BitAnd);
        case JJTLSHIFT_2OP:
            return makeBinOp(BinOp.LShift);
        case JJTRSHIFT_2OP:
            return makeBinOp(BinOp.RShift);
        case JJTADD_2OP:  
            return makeBinOp(BinOp.Add);
        case JJTSUB_2OP: 
            return makeBinOp(BinOp.Sub);
        case JJTMUL_2OP:
            return makeBinOp(BinOp.Mult);
        case JJTDIV_2OP: 
            return makeBinOp(BinOp.Div);
        case JJTMOD_2OP:
            return makeBinOp(BinOp.Mod);
        case JJTPOW_2OP:
            return makeBinOp(BinOp.Pow);
        case JJTFLOORDIV_2OP:
            return makeBinOp(BinOp.FloorDiv);
        case JJTPOS_1OP:
            return new UnaryOp(UnaryOp.UAdd, makeExpr());
        case JJTNEG_1OP:
            return new UnaryOp(UnaryOp.USub, makeExpr());
        case JJTINVERT_1OP:
            return new UnaryOp(UnaryOp.Invert, makeExpr());
        case JJTNOT_1OP:
            return new UnaryOp(UnaryOp.Not, makeExpr());
        case JJTCALL_OP:
            //if (arity == 1)
            //    return new Call(makeExpr(), null, null, null, null);
            exprType starargs = null;
            exprType kwargs = null;

            l = arity - 1;
            if (l > 0 && peekNode().getId() == JJTEXTRAKEYWORDVALUELIST) {
                kwargs = ((ExtraArgValue) popNode()).value;
                l--;
            }
            if (l > 0 && peekNode().getId() == JJTEXTRAARGVALUELIST) {
                starargs = ((ExtraArgValue) popNode()).value;
                l--;
            }
            
            int nargs = l;

            SimpleNode[] tmparr = new SimpleNode[l]; 
            for (int i = l - 1; i >= 0; i--) {
                tmparr[i] = popNode();
                if (tmparr[i] instanceof keywordType) {
                    nargs = i;
                }
            }
            
            exprType[] args = new exprType[nargs];
            for (int i = 0; i < nargs; i++) {
                args[i] = makeExpr(tmparr[i]);
            }

            keywordType[] keywords = new keywordType[l - nargs];
            for (int i = nargs; i < l; i++) {
                if (!(tmparr[i] instanceof keywordType))
                    throw new ParseException(
                        "non-keyword argument following keyword", tmparr[i]);
                keywords[i - nargs] = (keywordType) tmparr[i];
            }
            exprType func = makeExpr();
            return new Call(func, args, keywords, starargs, kwargs);
        case JJTEXTRAKEYWORDVALUELIST:
            return new ExtraArgValue(makeExpr(), JJTEXTRAKEYWORDVALUELIST);
        case JJTEXTRAARGVALUELIST:
            return new ExtraArgValue(makeExpr(), JJTEXTRAARGVALUELIST);
        case JJTKEYWORD:
            value = makeExpr();
            name = makeIdentifier();
            return new keywordType(name, value);
        case JJTTUPLE:
            return new Tuple(makeExprs(), Tuple.Load);
        case JJTLIST:
            if (stack.nodeArity() > 0 && peekNode() instanceof listcompType) {
                listcompType[] generators = new listcompType[arity-1];
                for (int i = arity-2; i >= 0; i--) {
                    generators[i] = (listcompType) popNode();
                }
                return new ListComp(makeExpr(), generators);
            }
            return new List(makeExprs(), List.Load);
        case JJTDICTIONARY:
            l = arity / 2;
            exprType[] keys = new exprType[l];
            exprType[] vals = new exprType[l];
            for (int i = l - 1; i >= 0; i--) {
                vals[i] = makeExpr();
                keys[i] = makeExpr();
            }
            return new Dict(keys, vals);
        case JJTSTR_1OP:
            return new Repr(makeExpr());
        case JJTSTRJOIN:
            String str2 = ((Str) popNode()).s;
            String str1 = ((Str) popNode()).s;
            return new Str(str1 + str2);
        case JJTLAMBDEF:
            test = makeExpr();
            arguments = makeArguments(arity - 1);
            return new Lambda(arguments, test);
        case JJTELLIPSES:
            return new Ellipsis();
        case JJTSLICE:
            SimpleNode[] arr = new SimpleNode[arity];
            for (int i = arity-1; i >= 0; i--) {
                arr[i] = popNode();
            }

            exprType[] values = new exprType[3];
            int k = 0;
            for (int j = 0; j < arity; j++) {
                if (arr[j].getId() == JJTCOLON)
                    k++;
                else
                    values[k] = makeExpr(arr[j]);
            }
            if (k == 0) {
                return new Index(values[0]);
            } else {
                return new Slice(values[0], values[1], values[2]);
            }
        case JJTSUBSCRIPTLIST:
            if (arity > 0 && peekNode().getId() == JJTCOMMA){
                arity--;
                popNode();
            }
            sliceType[] dims = new sliceType[arity];
            for (int i = arity - 1; i >= 0; i--) {
                dims[i] = (sliceType) popNode();
            }
            return new ExtSlice(dims);
        case JJTAUG_PLUS:     
            return makeAugAssign(AugAssign.Add);
        case JJTAUG_MINUS:   
            return makeAugAssign(AugAssign.Sub);
        case JJTAUG_MULTIPLY:  
            return makeAugAssign(AugAssign.Mult);
        case JJTAUG_DIVIDE:   
            return makeAugAssign(AugAssign.Div);
        case JJTAUG_MODULO:  
            return makeAugAssign(AugAssign.Mod);
        case JJTAUG_AND:    
            return makeAugAssign(AugAssign.BitAnd);
        case JJTAUG_OR:    
            return makeAugAssign(AugAssign.BitOr);
        case JJTAUG_XOR:  
            return makeAugAssign(AugAssign.BitXor);
        case JJTAUG_LSHIFT:   
            return makeAugAssign(AugAssign.LShift);
        case JJTAUG_RSHIFT:  
            return makeAugAssign(AugAssign.RShift);
        case JJTAUG_POWER:  
            return makeAugAssign(AugAssign.Pow);
        case JJTAUG_FLOORDIVIDE:  
            return makeAugAssign(AugAssign.FloorDiv);
        case JJTLIST_FOR:
            exprType[] ifs = new exprType[arity-2];
            for (int i = arity-3; i >= 0; i--) {
                ifs[i] = makeExpr();
            }
            iter = makeExpr();
            target = makeExpr();
            ctx.setStore(target);
            return new listcompType(target, iter, ifs);
        case JJTIMPORTFROM:
            aliasType[] aliases = makeAliases(arity - 1);
            String module = makeIdentifier();
            return new ImportFrom(module, aliases);
        case JJTIMPORT:
            return new Import(makeAliases());
    
        case JJTDOTTED_NAME:
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < arity; i++) {
                if (i > 0)
                    sb.insert(0, '.');
                sb.insert(0, makeIdentifier());
            }
            return new Name(sb.toString(), Name.Load);

        case JJTDOTTED_AS_NAME:
            String asname = null;
            if (arity > 1)
                asname = makeIdentifier();
            return new aliasType(makeIdentifier(), asname);

        case JJTIMPORT_AS_NAME:
            asname = null;
            if (arity > 1)
                asname = makeIdentifier();
            return new aliasType(makeIdentifier(), asname);
        case JJTCOMMA:
        case JJTCOLON:
            return n;
        default:
            return null;
        }
    }

    private stmtType makeAugAssign(int op) throws Exception {
        exprType value = makeExpr();
        exprType target = makeExpr();
        ctx.setAugStore(target);
        return new AugAssign(target, op, value);
    }

    private void dumpStack() {
        int n = stack.nodeArity();
        System.out.println("nodeArity:" + n);
        if (n > 0) {
            System.out.println("peek:" + stack.peekNode());
        }
    }

    SimpleNode peekNode() {
        return (SimpleNode) stack.peekNode();
    }

    SimpleNode popNode() {
        return (SimpleNode) stack.popNode();
    }

    BinOp makeBinOp(int op) {
        exprType right = makeExpr();
        exprType left = makeExpr();
        return new BinOp(left, op, right);
    }

    argumentsType makeArguments(int l) throws Exception {
        String kwarg = null;
        String stararg = null;
        if (l > 0 && peekNode().getId() == JJTEXTRAKEYWORDLIST) {
            kwarg = ((ExtraArg) popNode()).name;
            l--;
        }
        if (l > 0 && peekNode().getId() == JJTEXTRAARGLIST) {
            stararg = ((ExtraArg) popNode()).name;
            l--;
        }
        int startofdefaults = l;
        exprType fpargs[] =  new exprType[l];
        exprType defaults[] =  new exprType[l];
        for (int i = l-1; i >= 0; i--) {
            DefaultArg node = (DefaultArg) popNode();
            fpargs[i] = node.parameter;
            ctx.setStore(fpargs[i]);
            defaults[i] = node.value;
            if (node.value != null)
                startofdefaults = i;
        }
//System.out.println("start "+  startofdefaults + " " + l);
        exprType[] newdefs = new exprType[l-startofdefaults];
        System.arraycopy(defaults, startofdefaults, newdefs, 0, newdefs.length);
        
        return new argumentsType(fpargs, stararg, kwarg, newdefs);
    }
}

class DefaultArg extends SimpleNode {
    public exprType parameter;
    public exprType value;
    DefaultArg(exprType parameter, exprType value) {
        this.parameter = parameter;
        this.value = value;
    }
}

class ExtraArg extends SimpleNode {
    public String name;
    public int id;
    ExtraArg(String name, int id) {
        this.name = name;
        this.id = id;
    }
    public int getId() {
        return id;
    }
}


class ExtraArgValue extends SimpleNode {
    public exprType value;
    public int id;
    ExtraArgValue(exprType value, int id) {
        this.value = value;
        this.id = id;
    }
    public int getId() {
        return id;
    }
}


class IdentityNode extends SimpleNode {
    public int id;
    public Object image;

    IdentityNode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setImage(Object image) {
        this.image = image;
    }

    public Object getImage() {
        return image;
    }

    public String toString() {
        return "IdNode[" + PythonGrammarTreeConstants.jjtNodeName[id] + ", " +
                image + "]";
    }
}

class CtxVisitor extends Visitor {
    private int ctx;

    public CtxVisitor() { }

    public void setStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Store;
        visit(node);
    }

    public void setStore(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) 
            setStore(nodes[i]);
    }

    public void setDelete(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Del;
        visit(node);
    }

    public void setDelete(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) 
            setDelete(nodes[i]);
    }

    public void setAugStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.AugStore;
        visit(node);
    }

    public Object visitName(Name node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitAttribute(Attribute node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitSubscript(Subscript node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitList(List node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            throw new ParseException(
                    "augmented assign to list not possible", node);
        }
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    public Object visitTuple(Tuple node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            throw new ParseException(
                    "augmented assign to tuple not possible", node);
        }
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    public Object visitCall(Call node) throws Exception {
        throw new ParseException("can't assign to function call", node);
    }

    public Object visitListComp(Call node) throws Exception {
        throw new ParseException("can't assign to list comprehension call",
                                 node);
    }

    public Object unhandled_node(SimpleNode node) throws Exception {
        throw new ParseException("can't assign to operator", node);
    }
}
