/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.util;

import java.util.LinkedList;

/**
 * This queue blocks until closed or an element is enqueued.  If the queue
 * reaches capacity, the dequeue thread gets priority in order to bring the
 * queue size under a certain threshold.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class Queue {

    /**
     * Field closed
     */
    protected boolean closed;

    /**
     * Field queue
     */
    protected LinkedList queue;

    /**
     * Field capacity, threshold
     */
    protected int capacity, threshold;

    /**
     * Instantiate a blocking queue with no bounded capacity.
     */
    public Queue() {
        this(0);
    }

    /**
     * Instantiate a blocking queue with the specified capacity.
     */
    public Queue(int capacity) {

        this.closed = false;
        this.capacity = capacity;
        this.queue = new LinkedList();
        this.threshold = (int) (this.capacity * 0.75f);
    }

    /**
     * Enqueue an object and notify all waiting Threads.
     */
    public synchronized void enqueue(Object element) throws InterruptedException {

        if (closed) {
            throw new QueueClosedException();
        }

        this.queue.addLast(element);
        this.notify();

        /*
         * Block while the capacity of the queue has been breached.
         */
        while ((this.capacity > 0) && (this.queue.size() >= this.capacity)) {
            this.wait();

            if (closed) {
                throw new QueueClosedException();
            }
        }
    }

    /**
     * Blocks until an object is dequeued or the queue is closed.
     */
    public synchronized Object dequeue() throws InterruptedException {

        while (this.queue.size() <= 0) {
            this.wait();

            if (closed) {
                throw new QueueClosedException();
            }
        }

        Object object = this.queue.removeFirst();

        // if space exists, notify the other threads
        if (this.queue.size() < this.threshold) {
            this.notify();
        }

        return object;
    }

    /**
     * Close the queue and notify all waiting Threads.
     */
    public synchronized void close() {

        this.closed = true;

        this.notifyAll();
    }
}
