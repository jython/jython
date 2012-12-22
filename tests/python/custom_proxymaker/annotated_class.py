import java.io.BufferedInputStream

from java.lang import Deprecated

from org.python.compiler.custom_proxymaker import MiniClampMaker
from org.python.compiler.custom_proxymaker import CustomAnnotation

class AnnotatedInputStream(java.io.BufferedInputStream):
    __proxymaker__ = MiniClampMaker
    __java_package__ = 'custom_proxymaker.tests'

    _clamp_class_annotations = {CustomAnnotation:
                                {'createdBy': 'Darusik',
                                 'priority': CustomAnnotation.Priority.LOW,
                                 'changedBy': ['Darjus', 'Darjunia']},
                                Deprecated:None}


