/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.connect;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import java.lang.reflect.*;

import org.python.core.*;
import com.ziclix.python.sql.*;
import com.ziclix.python.sql.util.*;

/**
 * Connect using through a javax.sql.DataSource or javax.sql.ConnectionPooledDataSource.
 *
 * @author brian zimmer
 * @author last revised by $Author$
 * @version $Revision$
 */
public class Connectx extends PyObject {

    private final String SET = "set";
    private final PyString doc = new PyString("establish a connection through a javax.sql.DataSource or javax.sql.ConnectionPooledDataSource");

    /**
     * Constructor Connectx
     */
    public Connectx() {
    }

    /**
     * Method __findattr__
     *
     * @param String name
     * @return PyObject
     */
    public PyObject __findattr__(String name) {

        if ("__doc__".equals(name)) {
            return doc;
        }

        return super.__findattr__(name);
    }

    /**
     * Construct a javax.sql.DataSource or javax.sql.ConnectionPooledDataSource
     */
    public PyObject __call__(PyObject[] args, String[] keywords) {

        Connection c = null;
        PyConnection pc = null;
        Object datasource = null;
        PyArgParser parser = new PyArgParser(args, keywords);

        try {
            String _class = (String) parser.arg(0).__tojava__(String.class);

            datasource = Class.forName(_class).newInstance();
        } catch (Exception e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, "unable to instantiate datasource");
        }

        String[] kws = parser.kws();
        Class clazz = datasource.getClass();

        for (int i = 0; i < kws.length; i++) {
            String methodName = kws[i];

            if (methodName == null) {
                continue;
            }

            Object value = parser.kw(kws[i]).__tojava__(Object.class);

            if (methodName.length() > SET.length()) {
                if (!SET.equals(methodName.substring(0, SET.length()))) {

                    // prepend "set"
                    invoke(datasource, SET + methodName.substring(0, 1).toUpperCase() + methodName.substring(1), value);
                } else {

                    // starts with "set" so just pass it on
                    invoke(datasource, methodName, value);
                }
            } else {

                // shorter than "set" so it can't be a full method name
                invoke(datasource, SET + methodName.substring(0, 1).toUpperCase() + methodName.substring(1), value);
            }
        }

        try {
            if (datasource instanceof ConnectionPoolDataSource) {
                c = ((ConnectionPoolDataSource) datasource).getPooledConnection().getConnection();
            } else if (datasource instanceof DataSource) {
                c = ((DataSource) datasource).getConnection();
            }
        } catch (SQLException e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
        }

        try {
            if ((c == null) || c.isClosed()) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, "unable to establish connection");
            }

            pc = new PyConnection(c);
        } catch (SQLException e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
        }

        return pc;
    }

    /**
     * Method toString
     *
     * @return String
     */
    public String toString() {
        return "<connectx object instance at " + Py.id(this) + ">";
    }

    /**
     * Method invoke
     *
     * @param Object src
     * @param String methodName
     * @param Object value
     */
    protected void invoke(Object src, String methodName, Object value) {

        Method method = null;
        StringBuffer exceptionMsg = new StringBuffer("method [").append(methodName).append("] using arg type [");

        exceptionMsg.append(value.getClass()).append("], value [").append(value.toString()).append("]");

        try {
            method = getMethod(src.getClass(), methodName, value.getClass());

            if (method == null) {
                throw zxJDBC.makeException("no such " + exceptionMsg);
            }

            method.invoke(src, new Object[]{value});
        } catch (IllegalAccessException e) {
            throw zxJDBC.makeException("illegal access for " + exceptionMsg);
        } catch (InvocationTargetException e) {
            throw zxJDBC.makeException("invocation target exception for " + exceptionMsg);
        }

        return;
    }

    /**
     * Try to find the method by the given name.  If failing that, see if the valueClass
     * is perhaps a primitive and attempt to recurse using the primitive type.  Failing
     * that return null.
     */
    protected Method getMethod(Class srcClass, String methodName, Class valueClass) {

        Method method = null;

        try {
            method = srcClass.getMethod(methodName, new Class[]{valueClass});
        } catch (NoSuchMethodException e) {
            Class primitive = null;

            try {
                Field f = valueClass.getField("TYPE");

                primitive = (Class) f.get(valueClass);
            } catch (NoSuchFieldException ex) {
            } catch (IllegalAccessException ex) {
            } catch (ClassCastException ex) {
            }

            if ((primitive != null) && primitive.isPrimitive()) {
                return getMethod(srcClass, methodName, primitive);
            }
        }

        return method;
    }

    // __class__ boilerplate -- see PyObject for details

    /**
     * Field __class__
     */
    public static PyClass __class__;

    /**
     * Method getPyClass
     *
     * @return PyClass
     */
    protected PyClass getPyClass() {
        return __class__;
    }
}
