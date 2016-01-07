package com.ziclix.python.sql;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.python.core.PyObject;
import org.python.core.PySystemState;

import junit.framework.TestCase;

public class DataHandlerTest extends TestCase {

    private DataHandler _handler;

    @Override
    protected void setUp() throws Exception {
        PySystemState.initialize();
        _handler = new DataHandler();
    }

    /**
     * make sure we handle every {@link java.sql.Types} somehow
     * 
     * @throws Exception
     */
    public void testGetPyObjectResultSetIntInt() throws Exception {
        ResultSet rs = (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),
                                                         new Class<?>[] {ResultSet.class},
                                                         new DefaultReturnHandler());
        List<String> unsupportedTypes = Arrays.asList(
                "ARRAY",
                "DATALINK",
                "DISTINCT",
                "REF",
                "REF_CURSOR",
                "ROWID",
                "STRUCT",
                "TIME_WITH_TIMEZONE",
                "TIMESTAMP_WITH_TIMEZONE"
        );
        for (Field field : Types.class.getDeclaredFields()) {
            String typeName = field.getName();
            int type = field.getInt(null);
            if (unsupportedTypes.contains(typeName)) {
                try {
                    _handler.getPyObject(rs, 1, type);
                    fail("SQLException expected: " + typeName);
                } catch (SQLException sqle) {
                    // expected
                }
            } else {
                try {
                    PyObject pyobj = _handler.getPyObject(rs, 1, type);
                    assertNotNull(typeName + " should return None", pyobj);
                } catch (SQLException sqle) {
                    // unexpected! but useful for future proofing changes in SQL support
                    fail("unexpected SQLException: " + typeName);
                }
            }
        }
    }

    /**
     * This is a poor man's mock - i cannot introduce a mock framework at this point in time
     */
    static class DefaultReturnHandler implements InvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> returnType = method.getReturnType();
            if (returnType.equals(Boolean.class) || returnType.equals(Boolean.TYPE)) {
                return Boolean.FALSE;
            } else if (Character.TYPE.equals(returnType)) {
                return Character.valueOf('0');
            } else if (Byte.TYPE.equals(returnType)) {
                return Byte.valueOf((byte)0);
            } else if (Short.TYPE.equals(returnType)) {
                return Short.valueOf((short)0);
            } else if (Integer.TYPE.equals(returnType)) {
                return Integer.valueOf(0);
            } else if (Long.TYPE.equals(returnType)) {
                return Long.valueOf(0L);
            } else if (Float.TYPE.equals(returnType)) {
                return Float.valueOf(0);
            } else if (Double.TYPE.equals(returnType)) {
                return Double.valueOf(0);
            } else if (returnType.isPrimitive()) {
                throw new RuntimeException("unhandled primitve type " + returnType);
            } else if (returnType.isAssignableFrom(BigInteger.class)) {
                return BigInteger.ZERO;
            } else if (returnType.isAssignableFrom(BigDecimal.class)) {
                return BigDecimal.ZERO;
            } else if (returnType.isAssignableFrom(Number.class)) {
                return 0;
            } else {
                return null;
            }
        }
    }
}
