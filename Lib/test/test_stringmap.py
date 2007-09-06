import unittest
from test import test_support

from test_userdict import TestMappingProtocol

class SimpleClass:
    pass

class ClassDictTests(TestMappingProtocol):
    """check that os.environ object conform to mapping protocol"""
    _tested_class = None
    def _reference(self):
        return {"key1":"value1", "key2":2, "key3":(1,2,3)}
        #return {"KEY1":"VALUE1", "KEY2":"VALUE2", "KEY3":"VALUE3"}
    def _empty_mapping(self):
        for key in SimpleClass.__dict__:
            SimpleClass.__dict__.pop(key)
        return SimpleClass.__dict__

class InstanceDictTests(TestMappingProtocol):
    """check that os.environ object conform to mapping protocol"""
    _tested_class = None
    def _reference(self):
        return {"key1":"value1", "key2":2, "key3":(1,2,3)}
        #return {"KEY1":"VALUE1", "KEY2":"VALUE2", "KEY3":"VALUE3"}
    def _empty_mapping(self):
        return SimpleClass().__dict__

def test_main():
    test_support.run_unittest(
        ClassDictTests,
        InstanceDictTests,
    )

if __name__ == "__main__":
    test_main()
