package org.python.modules._threading;

/**
 * The protocol needed for a Lock object to work with a Condition
 * object.
 */
interface ConditionSupportingLock
{
    java.util.concurrent.locks.Lock getLock();
    boolean acquire();
    boolean acquire(boolean blocking);
    void release();
    boolean _is_owned();
    int	getWaitQueueLength(java.util.concurrent.locks.Condition condition);
}
