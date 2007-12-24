from javatests import Dict2JavaTest
import unittest, test.test_support

# Test the java.util.Map interface of org.python.core.PyDictionary.
# This tests the functionality of being able to pass a dictionaries
# created in Jython to a java method, and the ability to manipulate
# the dictionary object once in Java code.  The Java Dict2JavaTest is
# used to run some tests in Java code since they cannot be done on
# the Jython side.

class JythonMapInJavaTest(unittest.TestCase):

    def test_pydictionary_in_java(self):

        dict = {"a":"x", "b":"y", "c":"z", "d": None, None: "foo"}
        jmap = Dict2JavaTest(dict)

        # Make sure we see it on the java side
        self.assertEqual(True, len(dict) == jmap.size() and jmap.containsKey("a")
               and jmap.containsKey("b") and jmap.containsKey("c")
               and jmap.containsKey("d"))


        # Add {"e":"1", "f":null, "g":"2"} using the Map.putAll method
        oldlen = len(dict)
        self.assertEqual(True, jmap.test_putAll_efg())
        self.assertEqual(True, jmap.size() == len(dict) == oldlen + 3)
        self.assertEqual(True, dict["e"] == "1" and dict["f"] == None and dict["g"] == "2")

        # test Map.get method, get "g" and "d" test will throw an exception if fail    
        self.assertEqual(True, jmap.test_get_gd())

        # remove elements with keys "a" and "c" with the Map.remove method
        oldlen = len(dict)
        self.assertEqual(True, jmap.test_remove_ac())
        self.assertEqual(True, jmap.size() == len(dict) == oldlen - 2
               and "a" not in dict and "c" not in dict)

        # test Map.put method, adds {"h":null} and {"i": Integer(3)} and {"g": "3"}
        # "g" replaces a previous value of "2"
        oldlen = len(dict)
        self.assertEqual(True, jmap.test_put_hig())
        self.assertEqual(True, dict["h"] == None and dict["i"] == 3 and dict["g"] == "3"
               and len(dict) == oldlen+2)

        self.assertEqual(True, jmap.test_java_mapentry())

        set = jmap.entrySet()
        self.assertEqual(True, set.size() == len(dict))

        # Make sure the set is consistent with the dictionary
        for entry in set:
            self.assertEqual(True, dict.has_key(entry.getKey()))
            self.assertEqual(True, dict[entry.getKey()] == entry.getValue())
            self.assertEqual(True, set.contains(entry))

        # make sure changes in the set are reflected in the dictionary
        for entry in set:
            if entry.getKey() == "h":
                hentry = entry
            if entry.getKey() == "e":
                eentry = entry

        # Make sure nulls and non Map.Entry object do not match anything in the set
        self.assertEqual(True, jmap.test_entry_set_nulls())

        self.assertEqual(True, hentry != None and eentry != None)
        self.assertEqual(True, set.remove(eentry))
        self.assertEqual(True, not set.contains(eentry) and "e" not in dict)
        self.assertEqual(True, set.remove(hentry))
        self.assertEqual(True, not set.contains(hentry) and "h" not in dict)
        self.assertEqual(True, jmap.size() == set.size() == len(dict))
        oldlen = set.size()
        self.assertEqual(True, not set.remove(eentry))
        self.assertEqual(True, jmap.size() == set.size() == len(dict) == oldlen)

        # test Set.removeAll method
        oldlen = len(dict)
        elist = [ entry for entry in set if entry.key in ["b", "g", "d", None]]
        self.assertEqual(len(elist), 4)
        self.assertEqual(True, set.removeAll(elist))
        self.assertEqual(True, "b" not in dict and "g" not in dict and "d"
                       not in dict and None not in dict)
        self.assertEqual(True, len(dict) == set.size() == jmap.size() == oldlen - 4)

        itr = set.iterator()
        while (itr.hasNext()):
            val = itr.next()
            itr.remove()
        self.assertEqual(True, set.isEmpty() and len(dict) == jmap.size() == 0)

        # Test collections returned by keySet() 
        jmap.put("foo", "bar")
        jmap.put("num", 5)
        jmap.put(None, 4.3)
        jmap.put(34, None)
        keyset = jmap.keySet()
        self.assertEqual(True, len(dict) == jmap.size() == keyset.size() == 4)

        self.assertEqual(True, keyset.remove(None))
        self.assertEqual(True, len(dict) == 3 and not keyset.contains(None))
        self.assertEqual(True, keyset.remove(34))
        self.assertEqual(True, len(dict) == 2 and not keyset.contains(34))
        itr = keyset.iterator()
        while itr.hasNext():
            key = itr.next()
            if key == "num":
                itr.remove()
        self.assertEqual(True, len(dict) == jmap.size() == keyset.size() == 1)

        # test collections returned by values()
        jmap.put("foo", "bar")
        jmap.put("num", "bar")
        jmap.put(None, 3.2)
        jmap.put(34, None)
        values = jmap.values()
        self.assertEqual(True, len(dict) == jmap.size() == values.size() == 4)

        self.assertEqual(True, values.remove(None))
        self.assertEqual(True, values.size() == 3)
        itr = values.iterator()
        while itr.hasNext():
            val = itr.next()
            if val == "bar":
                itr.remove()
        self.assertEqual(True, len(dict) == values.size() == jmap.size() == 1)
        values.clear()
        self.assertEqual(True, values.isEmpty() and len(dict) == 0 and jmap.size() == 0)


def test_main():
    test.test_support.run_unittest(JythonMapInJavaTest)

if __name__ == '__main__':
    test_main()
