// Copyright (c)2019 Jython Developers.
// Licensed to the Python Software Foundation under a Contributor Agreement.

package org.python.tests.mro;

import java.io.Serializable;
import java.util.Map;

/**
 * A class providing interface and abstract class relationships that approximate the structure of
 * com.ibm classes related to MQ, in order to exercise b.j.o issue 2445. The complex inheritance
 * confused PyJavaType handling of the MRO. This class is imported by
 * {@code test_java_integration.JavaMROTest.test_mro_ibmmq}.
 * <p>
 * An invocation at the prompt (for debugging use), and output before the fix, is: <pre>
 * PS &gt; dist\bin\jython -S -c "from org.python.tests.mro import IBMMQChallengeMRO; t=m.mq.jms.MQQueue"
 * Traceback (most recent call last):
 *   File "&lt;string&gt;", line 1, in &lt;module&gt;
 * TypeError: Supertypes that share a modified attribute have an MRO conflict
 * [attribute=get,supertypes=[&lt;type
 * 'org.python.tests.mro.IBMMQChallengeMRO$msg$client$jms$internal$JmsPropertyContextImpl'&gt;],
 * type=IBMMQChallengeMRO$mq$jms$MQQueue]
 * </pre>
 */
public class IBMMQChallengeMRO {

    static class javax_jms { // represents javax.jms

        public interface Queue extends Destination {}
        public interface Destination {}
    }

    static class javax_naming { // represents javax.naming

        public interface Referenceable {}
    }

    static class msg { // represents com.ibm.msg

        static class client { // represents com.ibm.msg.client

            static class jms { // represents com.ibm.msg.client.jms

                public interface JmsDestination extends JmsPropertyContext, javax_jms.Destination {}
                public interface JmsPropertyContext
                        extends JmsReadablePropertyContext, Map<String, Object> {}
                public interface JmsReadablePropertyContext extends Serializable {}
                public interface JmsQueue extends JmsDestination, javax_jms.Queue {}

                static class admin { // represents com.ibm.msg.client.jms.admin

                    public static abstract/* ? */ class JmsJndiDestinationImpl
                            extends JmsDestinationImpl
                            implements JmsDestination, javax_naming.Referenceable, Serializable {}
                    public static abstract/* ? */ class JmsDestinationImpl
                            extends internal.JmsPropertyContextImpl implements JmsDestination {}
                }

                static class internal { // represents com.ibm.msg.client.jms.internal

                    public static abstract/* ? */ class JmsPropertyContextImpl
                            extends JmsReadablePropertyContextImpl implements JmsPropertyContext,
                            provider.ProviderPropertyContextCallback {}
                    public static abstract class JmsReadablePropertyContextImpl
                            implements JmsReadablePropertyContext {}
                }
            }

            static class provider { // represents com.ibm.msg.client.provider

                public interface ProviderPropertyContextCallback {}
            }
        }
    }

    static class jms { // represents com.ibm.jms

        public interface JMSDestination extends javax_jms.Destination {}
    }

    public static class mq { // represents com.ibm.mq

        public static class jms { // represents com.ibm.mq.jms

            public abstract/* ? */ class MQDestination
                    extends msg.client.jms.admin.JmsJndiDestinationImpl implements
                    javax_jms.Destination, IBMMQChallengeMRO.jms.JMSDestination, Serializable {}
            /** Target class in the test **/
            public abstract/* ? */ class MQQueue extends MQDestination implements javax_jms.Queue,
                    msg.client.jms.JmsQueue, javax_naming.Referenceable, Serializable {}
        }
    }
}
