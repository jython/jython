/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.pipe;

import org.python.core.*;
import com.ziclix.python.sql.*;
import com.ziclix.python.sql.util.*;

/**
 * Manager for a Sink and Source.  The Pipe creates a Queue through which the Source
 * can feed data to the Sink.  Both Sink and Source run in their own thread and can
 * are completely independent of the other.  When the Source pushes None onto the
 * Queue, the piping is stopped and the Sink finishes processing all the remaining
 * data.  This class is especially useful for loading/copying data from one database
 * or table to another.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class Pipe {

    /**
     * Default empty constructor.
     */
    public Pipe() {
    }

    /**
     * Start the processing of the Source->Sink.
     *
     * @param source the data generator
     * @param sink   the consumer of the data
     * @return the number of rows seen (this includes the header row)
     */
    public PyObject pipe(Source source, Sink sink) {

        Queue queue = new Queue();
        SourceRunner sourceRunner = new SourceRunner(queue, source);
        SinkRunner sinkRunner = new SinkRunner(queue, sink);

        sourceRunner.start();
        sinkRunner.start();

        try {
            sourceRunner.join();
        } catch (InterruptedException e) {
            queue.close();

            throw zxJDBC.makeException(e);
        }

        try {
            sinkRunner.join();
        } catch (InterruptedException e) {
            queue.close();

            throw zxJDBC.makeException(e);
        }

        /*
         * This is interesting territory.  I originally tried to store the the Throwable in the Thread instance
         * and then re-throw it here, but whenever I tried, I would get an NPE in the construction of the
         * PyTraceback required for the PyException.  I tried calling .fillInStackTrace() but that didn't work
         * either.  So I'm left with getting the String representation and throwing that.  At least it gives
         * the relevant error messages, but the stack is lost.  This might have something to do with a Java
         * issue I don't completely understand, such as what happens for an Exception whose Thread is no longer
         * running?  Anyways, if anyone knows what to do I would love to hear about it.
         */
        if (sourceRunner.threwException()) {
            throw zxJDBC.makeException(sourceRunner.getException().toString());
        }

        if (sinkRunner.threwException()) {
            throw zxJDBC.makeException(sinkRunner.getException().toString());
        }

        // if the source count is -1, no rows were queried
        if (sinkRunner.getCount() == 0) {
            return Py.newInteger(0);
        }

        // Assert that both sides handled the same number of rows.  I know doing the check up front kinda defeats
        // the purpose of the assert, but there's no need to create the buffer if I don't need it and I still
        // want to throw the AssertionError if required
        if ((sourceRunner.getCount() - sinkRunner.getCount()) != 0) {
            Integer[] counts = {new Integer(sourceRunner.getCount()),
                                new Integer(sinkRunner.getCount())};
            String msg = zxJDBC.getString("inconsistentRowCount", counts);

            Py.assert_(Py.Zero, Py.newString(msg));
        }

        return Py.newInteger(sinkRunner.getCount());
    }
}

/**
 * Class PipeRunner
 *
 * @author
 * @author last modified by $Author$
 * @version $Revision$
 * @date $today.date$
 * @date last modified on $Date$
 * @copyright 2001 brian zimmer
 */
abstract class PipeRunner extends Thread {

    /**
     * Field counter
     */
    protected int counter;

    /**
     * Field queue
     */
    protected Queue queue;

    /**
     * Field exception
     */
    protected Throwable exception;

    /**
     * Constructor PipeRunner
     *
     * @param Queue queue
     */
    public PipeRunner(Queue queue) {

        this.counter = 0;
        this.queue = queue;
        this.exception = null;
    }

    /**
     * The total number of rows handled.
     */
    public int getCount() {
        return this.counter;
    }

    /**
     * Method run
     */
    public void run() {

        try {
            this.pipe();
        } catch (QueueClosedException e) {

            /*
             * thrown by a closed queue when any operation is performed.  we know
             * at this point that nothing else can happen to the queue and that
             * both producer and consumer will stop since one closed the queue
             * by throwing an exception (below) and the other is here.
             */
            return;
        } catch (Throwable e) {
            this.exception = e.fillInStackTrace();

            this.queue.close();
        }
    }

    /**
     * Handle the source/destination specific copying.
     */
    abstract protected void pipe() throws InterruptedException;

    /**
     * Return true if the thread terminated because of an uncaught exception.
     */
    public boolean threwException() {
        return this.exception != null;
    }

    /**
     * Return the uncaught exception.
     */
    public Throwable getException() {
        return this.exception;
    }
}

/**
 * Class SourceRunner
 *
 * @author
 * @author last modified by $Author$
 * @version $Revision$
 * @date $today.date$
 * @date last modified on $Date$
 * @copyright 2001 brian zimmer
 */
class SourceRunner extends PipeRunner {

    /**
     * Field source
     */
    protected Source source;

    /**
     * Constructor SourceRunner
     *
     * @param Queue  queue
     * @param Source source
     */
    public SourceRunner(Queue queue, Source source) {

        super(queue);

        this.source = source;
    }

    /**
     * Method pipe
     *
     * @throws InterruptedException
     */
    protected void pipe() throws InterruptedException {

        PyObject row = Py.None;

        this.source.start();

        try {
            while ((row = this.source.next()) != Py.None) {
                this.queue.enqueue(row);

                this.counter++;
            }
        } finally {
            try {
                this.queue.enqueue(Py.None);
            } finally {
                this.source.end();
            }
        }
    }
}

/**
 * Class SinkRunner
 *
 * @author
 * @author last modified by $Author$
 * @version $Revision$
 * @date $today.date$
 * @date last modified on $Date$
 * @copyright 2001 brian zimmer
 */
class SinkRunner extends PipeRunner {

    /**
     * Field sink
     */
    protected Sink sink;

    /**
     * Constructor SinkRunner
     *
     * @param Queue queue
     * @param Sink  sink
     */
    public SinkRunner(Queue queue, Sink sink) {

        super(queue);

        this.sink = sink;
    }

    /**
     * Method pipe
     *
     * @throws InterruptedException
     */
    protected void pipe() throws InterruptedException {

        PyObject row = Py.None;

        this.sink.start();

        try {
            while ((row = (PyObject) this.queue.dequeue()) != Py.None) {
                this.sink.row(row);

                this.counter++;
            }
        } finally {
            this.sink.end();
        }
    }
}
