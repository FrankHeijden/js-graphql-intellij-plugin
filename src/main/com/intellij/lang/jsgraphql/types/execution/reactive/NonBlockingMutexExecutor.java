/*
    The MIT License (MIT)

    Copyright (c) 2015 Andreas Marek and Contributors

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
    (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
    publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
    OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.intellij.lang.jsgraphql.types.execution.reactive;


import com.intellij.lang.jsgraphql.types.Internal;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static com.intellij.lang.jsgraphql.types.Assert.assertNotNull;


/**
 * Executor that provides mutual exclusion between the operations submitted to it,
 * without blocking.
 *
 * If an operation is submitted to this executor while no other operation is
 * running, it will run immediately.
 *
 * If an operation is submitted to this executor while another operation is
 * running, it will be added to a queue of operations to run, and the executor will
 * return. The thread currently running an operation will end up running the
 * operation just submitted.
 *
 * Operations submitted to this executor should run fast, as they can end up running
 * on other threads and interfere with the operation of other threads.
 *
 * This executor can also be used to address infinite recursion problems, as
 * operations submitted recursively will run sequentially.
 *
 *
 * Inspired by Public Domain CC0 code at h
 * https://github.com/jroper/reactive-streams-servlet/tree/master/reactive-streams-servlet/src/main/java/org/reactivestreams/servlet
 */
@Internal
class NonBlockingMutexExecutor implements Executor {
    private final AtomicReference<RunNode> last = new AtomicReference<>();

    @Override
    public void execute(final Runnable command) {
        final RunNode newNode = new RunNode(assertNotNull(command, () -> "Runnable must not be null"));
        final RunNode prevLast = last.getAndSet(newNode);
        if (prevLast != null)
            prevLast.lazySet(newNode);
        else
            runAll(newNode);
    }

    private void reportFailure(final Thread runner, final Throwable thrown) {
        if (thrown instanceof InterruptedException) {
            runner.interrupt();
        } else {
            final Thread.UncaughtExceptionHandler ueh = runner.getUncaughtExceptionHandler();
            if (ueh != null) {
                ueh.uncaughtException(runner, thrown);
            }
        }
    }

    // Runs a single RunNode and deals with any thing it throws
    private void run(final RunNode current) {
        try {
            current.runnable.run();
        } catch (final Throwable thrown) {
            reportFailure(Thread.currentThread(), thrown);
        }
    }

    // Runs all the RunNodes starting with `next`
    private void runAll(RunNode next) {
        for (; ; ) {
            final RunNode current = next;
            run(current);
            if ((next = current.get()) == null) { // try advance, if we get null test
                if (last.compareAndSet(current, null)) {
                    return; // end-of-queue: we're done.
                } else {
                    //noinspection StatementWithEmptyBody
                    while ((next = current.get()) == null) {
                        // Thread.onSpinWait(); in Java 9
                    }
                }
            }
        }
    }

    private static final class RunNode extends AtomicReference<RunNode> {
        private final Runnable runnable;

        private RunNode(final Runnable runnable) {
            this.runnable = runnable;
        }
    }

}
