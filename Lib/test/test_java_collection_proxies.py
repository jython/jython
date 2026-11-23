# Copyright (c)2026 Jython Developers.
# Licensed to PSF under a Contributor Agreement.

import copy
import unittest

from java.util import ArrayList, HashMap, HashSet
from org.python.tests.CollectionProxyFixtures import (
    CopyableSet, DictSubclass, ListSubclass, OverridingDict, SetSubclass)
from test import test_support


class NativeCollectionSubclassTest(unittest.TestCase):

    def test_dict_get(self):
        mapping = DictSubclass()
        mapping['key'] = 'value'
        mapping['none'] = None
        self.assertEqual(mapping.get('key'), 'value')
        self.assertEqual(mapping.get('key', 'fallback'), 'value')
        self.assertIsNone(mapping.get('missing'))
        self.assertEqual(mapping.get('missing', 'fallback'), 'fallback')
        self.assertIsNone(mapping.get('none', 'fallback'))

    def test_dict_values(self):
        mapping = DictSubclass()
        mapping['key'] = 'value'
        self.assertEqual(mapping.values(), ['value'])

    def test_dict_get_override(self):
        mapping = OverridingDict()
        self.assertEqual(mapping.get('missing', 'fallback'), 'fallback')
        self.assertEqual(mapping.getCallCount(), 1)

    def test_list_sort(self):
        values = ListSubclass()
        values.append(2)
        values.append(1)
        values.sort()
        self.assertEqual(list(values), [1, 2])

    def test_list_remove(self):
        values = ListSubclass()
        values.append('value')
        values.remove('value')
        self.assertEqual(len(values), 0)

    def test_set_remove(self):
        values = SetSubclass()
        values.add('value')
        values.remove('value')
        self.assertEqual(len(values), 0)

    def test_set_deepcopy_override(self):
        values = CopyableSet()
        values.add('value')
        copied = copy.deepcopy(values)
        self.assertEqual(copied, set(['value']))
        self.assertIsNot(copied, values)

    def test_wrapped_java_collections(self):
        mapping = HashMap()
        mapping['key'] = 'value'
        mapping['none'] = None
        self.assertEqual(mapping.get('key', 'fallback'), 'value')
        self.assertEqual(mapping.get('missing', 'fallback'), 'fallback')
        self.assertIsNone(mapping.get('none', 'fallback'))
        self.assertEqual(sorted(mapping.values(), key=str),
                         sorted(['value', None], key=str))

        values = ArrayList([2, 1])
        values.sort()
        self.assertEqual(list(values), [1, 2])
        values.remove(1)
        self.assertEqual(list(values), [2])

        values = HashSet(['value'])
        values.remove('value')
        self.assertEqual(len(values), 0)


def test_main():
    test_support.run_unittest(NativeCollectionSubclassTest)


if __name__ == '__main__':
    test_main()
