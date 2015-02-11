"""
Tests some Jython-specific gc aspects and debugging
features.
"""

import unittest
#from test.test_support import verbose, run_unittest
#import sys
import time
import gc
import weakref
from java.lang import System, Runnable

# class FinalizationDummy:
#     def __del__(self):
#         time.sleep(3.5)
#         print "FinalizationDummy.__del__"
#         time.sleep(3.5)
# 
# class ResurrectionDummy:
#     def __del__(self):
#         print "ResurrectionDummy.__del__"
#         ResurrectionDummy.resurrected = self.toResurrect
# 
# class SelfResurrectionDummy:
#     def __del__(self):
#         print "SelfResurrectionDummy.__del__"
#         SelfResurrectionDummy.resurrected = self

class GCTests_Jy_CyclicGarbage(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        #Jython-specific block:
        try:
            cls.savedJythonGCFlags = gc.getJythonGCFlags()
            #the finalizer-related tests need this flag to pass in Jython:
            gc.addJythonGCFlags(gc.DONT_FINALIZE_CYCLIC_GARBAGE)
            gc.stopMonitoring()
        except Exception:
            pass
     
    @classmethod
    def tearDownClass(cls):
        try:
            gc.setJythonGCFlags(cls.savedJythonGCFlags)
        except Exception:
            pass


    # In contrast to the tests in test_gc, these finalizer tests shall work
    # even if gc-monitoring is disabled.
    def test_finalizer(self):
        # A() is uncollectable if it is part of a cycle, make sure it shows up
        # in gc.garbage.
        class A:
            def __del__(self): pass
        class B:
            pass
        a = A()
        a.a = a
        id_a = id(a)
        b = B()
        b.b = b
        gc.collect()
        del a
        del b
        self.assertNotEqual(gc.collect(), 0)
        time.sleep(4)
        for obj in gc.garbage:
            if id(obj) == id_a:
                del obj.a
                break
        else:
            self.fail("didn't find obj in garbage (finalizer)")
        gc.garbage.remove(obj)


    def test_finalizer_newclass(self):
        # A() is uncollectable if it is part of a cycle, make sure it shows up
        # in gc.garbage.
        class A(object):
            def __del__(self): pass
        class B(object):
            pass
        a = A()
        a.a = a
        id_a = id(a)
        b = B()
        b.b = b
        gc.collect()
        del a
        del b
        self.assertNotEqual(gc.collect(), 0)
        time.sleep(1)
        for obj in gc.garbage:
            if id(obj) == id_a:
                del obj.a
                break
        else:
            self.fail("didn't find obj in garbage (finalizer)")
        gc.garbage.remove(obj)

    def test_manual_monitoring(self):
        # since tuples are immutable we close the loop with a list
        l = []
        t = (l,)
        l.append(t)
        gc.monitorObject(l)
        #gc.monitorObject(t) <- intentionally only monitor one of them
        gc.collect()
        del t
        del l
        #Note that usually two collected objects would be expected - l and t.
        #But we intentionally only monitored one of them, so only one should
        #be counted.
        self.assertEqual(gc.collect(), 1)


class GCTests_Jy_preprocess_and_postprocess(unittest.TestCase):

    def test_finalization_preprocess_and_postprocess(self):
        #print "test_finalization_preprocess_and_postprocess"
        #Note that this test is done here again (already was in another class
        #in this module), to see that everything works as it should also with
        #a different flag-context.
        #print "test_finalization_preprocess_and_postprocess"
        #gc.removeJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
        comments = []
        self0 = self
        class A:
            def __del__(self):
                #print "del A"
                self0.assertIn("run PreProcess", comments)
                comments.append("A del")
                #let's simulate a time-consuming finalizer
                #to ensure that post finalization processing
                #is sensitive to this
                time.sleep(0.5)
                comments.append("A del done")

        class PreProcess(Runnable):
            def run(self):
                self0.assertEqual(comments, [])
                comments.append("run PreProcess")

        class PostProcess(Runnable):
            def run(self):
                self0.assertIn("run PreProcess", comments)
                self0.assertIn("A del", comments)
                self0.assertIn("A del done", comments)
                comments.append("run PostProcess")

        a = A()
        a = None
        prePr = PreProcess()
        postPr = PostProcess()
        time.sleep(1) #   <- to avoid that the newly registered processes
                      #      become subject to previous run
        gc.registerPreFinalizationProcess(prePr)
        gc.registerPostFinalizationProcess(postPr)
        #Note that order matters here:
        #If the flag gc.DONT_FINALIZE_RESURRECTED_OBJECTS is used,
        #gc.registerPostFinalizationProcess(postPr, 0) would lead to failure,
        #because postPr asserts that a's finalizer already ran. Since
        #DONT_FINALIZE_RESURRECTED_OBJECTS also inserted a postprocess,
        #to perform delayed finalization, the 0-index would prepend postPr
        #before the process that actually runs the finalizers.
        System.gc()
        #we wait a bit longer here, since PostProcess runs asynchronous
        #and must wait for the finalizer of A
        time.sleep(2)
        self.assertIn("run PostProcess", comments)
        comments = []
        gc.unregisterPreFinalizationProcess(prePr)
        gc.unregisterPostFinalizationProcess(postPr)


class GCTests_Jy_Delayed_Finalization(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        #Jython-specific block:
        try:
            cls.savedJythonGCFlags = gc.getJythonGCFlags()
            #the finalizer-related tests need this flag to pass in Jython:
            gc.addJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
            gc.stopMonitoring()
        except Exception:
            pass

    @classmethod
    def tearDownClass(cls):
        try:
            gc.setJythonGCFlags(cls.savedJythonGCFlags)
        except Exception:
            pass

    def test_finalization_preprocess_and_postprocess(self):
        #print "test_finalization_preprocess_and_postprocess"
        #Note that this test is done here again (already was in another class
        #in this module), to see that everything works as it should also with
        #a different flag-context.
        #print "test_finalization_preprocess_and_postprocess"
        #gc.removeJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
        comments = []
        self0 = self
        class A:
            def __del__(self):
                #print "del A"
                self0.assertIn("run PreProcess", comments)
                comments.append("A del")
                #let's simulate a time-consuming finalizer
                #to ensure that post finalization processing
                #is sensitive to this
                time.sleep(0.5)
                comments.append("A del done")

        class PreProcess(Runnable):
            def run(self):
                self0.assertEqual(comments, [])
                comments.append("run PreProcess")

        class PostProcess(Runnable):
            def run(self):
                self0.assertIn("run PreProcess", comments)
                self0.assertIn("A del", comments)
                self0.assertIn("A del done", comments)
                comments.append("run PostProcess")

        a = A()
        a = None
        prePr = PreProcess()
        postPr = PostProcess()
        time.sleep(1) #   <- to avoid that the newly registered processes
                      #      become subject to previous run
        gc.registerPreFinalizationProcess(prePr)
        gc.registerPostFinalizationProcess(postPr)
        #Note that order matters here:
        #If the flag gc.DONT_FINALIZE_RESURRECTED_OBJECTS is used,
        #gc.registerPostFinalizationProcess(postPr, 0) would lead to failure,
        #because postPr asserts that a's finalizer already ran. Since
        #DONT_FINALIZE_RESURRECTED_OBJECTS also inserted a postprocess,
        #to perform delayed finalization, the 0-index would prepend postPr
        #before the process that actually runs the finalizers.
        System.gc()
        #we wait a bit longer here, since PostProcess runs asynchronous
        #and must wait for the finalizer of A
        time.sleep(2)
        self.assertIn("run PostProcess", comments)
        comments = []
        gc.unregisterPreFinalizationProcess(prePr)
        gc.unregisterPostFinalizationProcess(postPr)


    def test_delayedFinalization(self):
        #gc.addJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
        #gc.addJythonGCFlags(gc.VERBOSE)
        resurrect = []
        comments = []

        class Test_Finalizable(object):
            def __init__(self, name):
                self.name = name

            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)

        class Test_Resurrection(object):
            def __init__(self, name):
                self.name = name
            
            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)
                if hasattr(self, "toResurrect"):
                    resurrect.append(self.toResurrect)

        a = Test_Finalizable("a")
        a.b = Test_Finalizable("b")
        c = Test_Resurrection("c")
        c.a = a
        c.toResurrect = Test_Finalizable("d")
         
        del a
        del c
        self.assertNotEqual(gc.collect(), 0)
        time.sleep(1)
        #print comments
        #print resurrect
        self.assertIn('del c', comments)
        self.assertEqual(1, len(comments))
        comments = []
        self.assertNotEqual(gc.collect(), 0)
        time.sleep(1)
        #print comments
        #print resurrect
        self.assertIn('del a', comments)
        self.assertEqual(1, len(comments))
        comments = []
        self.assertNotEqual(gc.collect(), 0)
        time.sleep(1)
        self.assertIn('del b', comments)
        self.assertEqual(1, len(comments))
        #gc.removeJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
        #gc.removeJythonGCFlags(gc.VERBOSE)


class GCTests_Jy_Monitoring(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        #Jython-specific block:
        try:
            cls.savedJythonGCFlags = gc.getJythonGCFlags()
            gc.setMonitorGlobal(True)
            gc.addJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
            #since gc module already exists, it would not be caught by monitorGlobal.
            #so we have to monitor it manually:
            gc.monitorObject(gc)
            #the finalizer-related tests need this flag to pass in Jython:
            #gc.addJythonGCFlags(gc.DONT_FINALIZE_CYCLIC_GARBAGE)
        except Exception:
            pass

    @classmethod
    def tearDownClass(cls):
        try:
            gc.setJythonGCFlags(cls.savedJythonGCFlags)
            gc.stopMonitoring()
        except Exception:
            pass

    def test_monitor_status_after_delayed_finalization(self):
        resurrect = []
        comments = []

        class Test_Finalizable(object):
            def __init__(self, name):
                self.name = name

            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)

        class Test_Resurrection(object):
            def __init__(self, name):
                self.name = name
            
            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)
                if hasattr(self, "toResurrect"):
                    resurrect.append(self.toResurrect)

        a = Test_Finalizable("a")
        a.b = Test_Finalizable("b")
        c = Test_Resurrection("c")
        c.toResurrect = a
        a.b.a = a
        self.assertTrue(gc.isMonitored(a))
        self.assertTrue(gc.isMonitored(a.b))
        self.assertTrue(gc.isMonitored(c))
        gc.collect()
        del a
        del c
        #gc.set_debug(gc.DEBUG_SAVEALL)
        #gc.collect()
        self.assertEqual(gc.collect(), 0) #c is not cyclic and a, b are resurrected,
                                          #so nothing to count here
        #self.asserEqual(len(gc.garbage), 0)
            #if we called gc.set_debug(gc.DEBUG_SAVEALL) above, it would
            #be okay for gc.garbage to be empty, because a and b
            #are not finalized and c is not cyclic.
        self.assertEqual(comments, ['del c'])
        self.assertEqual(str(resurrect), "[<a>]")
        self.assertTrue(gc.isMonitored(resurrect[0]))
        self.assertTrue(gc.isMonitored(resurrect[0].b))

    def test_notifyRerun_for_delayed_finalization(self):
        gc.collect()
        resurrect = []
        comments = []

        class Test_Finalizable(object):
            def __init__(self, name):
                self.name = name

            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)

        a = Test_Finalizable("a")
        lst = []
        lst1 = [lst]
        lst.append(lst1)
        a.b = Test_Finalizable("b")
        a.b.lst = lst
        del lst
        del lst1
        self.assertTrue(gc.isMonitored(a))
        self.assertTrue(gc.isMonitored(a.b))
        del a
        self.assertEqual(gc.collect(), 2) # c is not cyclic and a, b are resurrected,
                                          # the cycle of two lists is counted here
        self.assertEqual(comments, ['del a', 'del b'])


class GCTests_Jy_Weakref(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        #Jython-specific block:
        try:
            cls.savedJythonGCFlags = gc.getJythonGCFlags()
            gc.addJythonGCFlags(gc.PRESERVE_WEAKREFS_ON_RESURRECTION)
        except Exception:
            pass

    @classmethod
    def tearDownClass(cls):
        try:
            gc.setJythonGCFlags(cls.savedJythonGCFlags)
            gc.stopMonitoring()
        except Exception:
            pass

    def test_weakref_after_resurrection(self):
        resurrect = []
        comments = []
        class Test_Finalizable(object):
            def __init__(self, name):
                self.name = name

            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)

        class Test_Resurrection(object):
            def __init__(self, name):
                self.name = name
            
            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)
                if hasattr(self, "toResurrect"):
                    resurrect.append(self)

        def clb(ref):
            comments.append("clb")

        def clb2(ref):
            comments.append("clb2 "+str(comments))

        a = Test_Finalizable("a")
        wa = weakref.ref(a, clb)
        self.assertEqual(wa(), a)
        c = Test_Resurrection("c")
        c.toResurrect = a
        wc = weakref.ref(c, clb2)
        try:
            gc.monitorObject(c)
        except Exception:
            pass
        del a
        del c
        gc.collect()
        self.assertIn('clb2 []', comments)
        self.assertNotIn("clb", comments)
        self.assertEqual(str(resurrect), "[<c>]")
        self.assertEqual(str(wa()), "<a>")
        self.assertEqual(wc(), None)

    def test_weakref_after_resurrection_and_delayed_finalize(self):
        resurrect = []
        comments = []
        class Test_Finalizable(object):
            def __init__(self, name):
                self.name = name

            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)

        class Test_Resurrection(object):
            def __init__(self, name):
                self.name = name
            
            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)
                if hasattr(self, "toResurrect"):
                    resurrect.append(self)

        def clb(ref):
            comments.append("clb")

        def clb2(ref):
            comments.append("clb2 "+str(comments))

        a = Test_Finalizable("a")
        wa = weakref.ref(a, clb)
        self.assertEqual(wa(), a)
        c = Test_Resurrection("c")
        c.toResurrect = a
        wc = weakref.ref(c, clb2)
        try:
            gc.monitorObject(c)
            gc.addJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
        except Exception:
            pass
        del a
        del c
        gc.collect()
        self.assertIn('del c', comments)
        self.assertNotIn('del a', comments)
        self.assertIn('clb2 []', comments)
        self.assertNotIn("clb", comments)
        self.assertEqual(str(resurrect), "[<c>]")
        self.assertEqual(str(wa()), "<a>")
        self.assertEqual(wc(), None)
        try:
            gc.removeJythonGCFlags(gc.DONT_FINALIZE_RESURRECTED_OBJECTS)
        except Exception:
            pass

    def test_weakref_after_resurrection_threadsafe(self):
        resurrect = []
        comments = []

        class Test_Finalizable(object):
            def __init__(self, name):
                self.name = name

            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)

        class Test_Resurrection(object):
            def __init__(self, name):
                self.name = name
            
            def __repr__(self):
                return "<"+self.name+">"

            def __del__(self):
                comments.append("del "+self.name)
                if hasattr(self, "toResurrect"):
                    resurrect.append(self)

        a = Test_Finalizable("a")
        wa = weakref.ref(a)
        c = Test_Resurrection("c")
        c.toResurrect = a
        wc = weakref.ref(c)
        del a
        del c
        try:
            gc.addJythonGCFlags(gc.PRESERVE_WEAKREFS_ON_RESURRECTION)
            System.gc()
            # We intentionally don't wait here, but want to observe
            # the situation with gc unfinnished. Note that wa() preserves
            # its result right away, due to thread-safe implementation.
            # Technically, the weak reference breaks and is restored after
            # gc-run finishes. However wa() blocks until the referent is
            # restored or the deletion is confirmed.
        except Exception:
            pass
        #self.assertEqual(str(wa()), '<a>')
        self.assertEqual(comments, [])
        self.assertEqual(resurrect, [])
        while comments == [] or resurrect == []:
            self.assertEqual(str(wa()), '<a>')
            self.assertEqual(wc(), None)
        self.assertEqual(str(wa()), '<a>')
        self.assertEqual(wc(), None)


if __name__ == "__main__":
    unittest.main()
