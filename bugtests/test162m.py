
import java, java.lang

class ParentlessClass:
    pass

class DerivedPyClass(ParentlessClass):
    pass

class DerivedJClass(java.lang.Object):
    pass