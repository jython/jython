package org.python.compiler.custom_proxymaker;

/*
 * This is a bare bones implementation of ClampMaker. It's goal is to be a "reference"
 * implementation for the features that are provided by customizable ProxyMaker
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.python.compiler.Code;
import org.python.compiler.JavaMaker;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.__builtin__;
import org.python.util.Generic;

public class MiniClampMaker extends JavaMaker {
    
    private final Map<String, PyObject> methodsToAdd = Generic.map();
    private final Map<String, PyObject> constructorsToAdd = Generic.map();
    private AnnotationDescr[] classAnnotations = new AnnotationDescr[]{};
    
    private static AnnotationDescr[] extractAnnotation(PyDictionary dict) {
        List<AnnotationDescr> annotationDescrs = Generic.list();
        for (PyObject annotationIter: dict.iteritems().asIterable()) {
            PyObject annotationClass = annotationIter.__getitem__(0);
            PyObject annotationFields = annotationIter.__getitem__(1);
            AnnotationDescr annotationDescr = null;
            if (annotationFields == Py.None) {
                annotationDescr = new AnnotationDescr(Py.tojava(annotationClass, Class.class));
            } else {
                Map<String, Object> fields = Generic.map();
                for (PyObject item: ((PyDictionary)annotationFields).iteritems().asIterable()) {
                    fields.put(Py.tojava(item.__getitem__(0), String.class), Py.tojava(item.__getitem__(1), Object.class));
                }
                annotationDescr = new AnnotationDescr(Py.tojava(annotationClass, Class.class), fields);
            }
            annotationDescrs.add(annotationDescr);
        }
        return (AnnotationDescr[]) annotationDescrs.toArray(new AnnotationDescr[annotationDescrs.size()]);
    }
    
    public MiniClampMaker(Class<?> superclass,
            Class<?>[] interfaces,
            String pythonClass,
            String pythonModule,
            String myClass,
            PyObject methods) {
        super(superclass, interfaces, pythonClass, pythonModule, myClass, methods);
        
        // if we find __java_package__, override the default proxy naming with
        // __java_package__ + .pythonClass
        PyObject javaPackage = methods.__finditem__("__java_package__");
        if (javaPackage != null) {
            String newMyClass = new String((String)javaPackage.__tojava__(String.class));
            newMyClass += "." + pythonClass;
            this.myClass = newMyClass;
        }
        
        
        PyObject clampAttr = Py.newString("_clamp");
        for (PyObject pykey : methods.asIterable()) {
            String key = Py.tojava(pykey, String.class);
            PyObject value = methods.__finditem__(key);
            PyObject clampObj = __builtin__.getattr(value, clampAttr, Py.None);
            if (clampObj == Py.None) {
                continue;
            }
            String name = (String)clampObj.__getattr__("name").__tojava__(String.class);
            if (name.equals("__init__")) {
                constructorsToAdd.put(key, clampObj);
            } else {
                methodsToAdd.put(key, clampObj);
            }
        }
        PyObject pyAnnotations = methods.__finditem__("_clamp_class_annotations");
        if (pyAnnotations != null) {
            classAnnotations = extractAnnotation((PyDictionary)pyAnnotations);
        }
        
    }

    @Override
    protected void visitClassAnnotations() throws Exception {
        for (AnnotationDescr annotation: classAnnotations) {
            addClassAnnotation(annotation);
        }
    }
    
    @Override
    protected void visitConstructors() throws Exception {

        Set<Constructor<?>> superConstructors = Generic.set();
        for (Constructor<?> constructor: superclass.getDeclaredConstructors()) {
            superConstructors.add(constructor);
        }
        
        for (Entry<String, PyObject> meth : constructorsToAdd.entrySet()) {
            Constructor<?> superToCall = null;
            String pyName = meth.getKey();
            PyObject clampObj = meth.getValue();

            Class<?>[] thrownClasses = Py.tojava(clampObj.__getattr__("throws"), Class[].class);
            Class<?>[] parameterClasses = Py.tojava(clampObj.__getattr__("argtypes"), Class[].class);

            if (clampObj.__findattr__("super_constructor") != null) {
                superToCall = (Constructor<?>)clampObj.__getattr__("super_constructor").__tojava__(Constructor.class);
            } else { // try to find a matching super constructor
                try {
                    superToCall = superclass.getDeclaredConstructor(parameterClasses);
                } catch (NoSuchMethodException err) {
                    // FIXME need a fancy constructor finder
                    superToCall = superConstructors.iterator().next();
                }
            }
            
            for (Constructor<?> constructor: superConstructors) {
                if (Arrays.equals(constructor.getParameterTypes(), superToCall.getParameterTypes())) {
                    superConstructors.remove(constructor);
                }
            }
            
            AnnotationDescr[] methodAnnotations = extractAnnotation((PyDictionary)clampObj.__getattr__("method_annotations"));
            PyObject[] parameterAnnotationObjs = (PyObject[])clampObj.__getattr__("parameter_annotations").__tojava__(PyObject[].class);
            
            AnnotationDescr[][]parameterAnnotations = new AnnotationDescr[parameterAnnotationObjs.length][];
            for (int i = 0; i<parameterAnnotationObjs.length; i++) {
                if (parameterAnnotationObjs[i].isMappingType()) {
                    parameterAnnotations[i] = extractAnnotation((PyDictionary)parameterAnnotationObjs[i]);
                }
            }
            
            String fullsig = makeSig(Void.TYPE, parameterClasses);
            String[] mappedExceptions = mapExceptions(thrownClasses);
            
            // if our superclass already has 
            Code code = classfile.addMethod("<init>", fullsig, Modifier.PUBLIC, mappedExceptions, methodAnnotations, parameterAnnotations);
            callSuper(code, "<init>", mapClass(superclass), superToCall.getParameterTypes(), Void.TYPE, false);
            // instead of calling the proxy, we construct the full method code

            addConstructorMethodCode(pyName, superToCall.getParameterTypes(), thrownClasses, Modifier.PUBLIC, superclass, code);
              
        }
        
        // the non-overwritten constructors
        for (Constructor<?> constructor: superConstructors) {
            addConstructor(constructor.getParameterTypes(), Modifier.PUBLIC);
        }
    }

    @Override
    protected void visitMethods () throws Exception {
        for (Entry<String, PyObject> meth : methodsToAdd.entrySet()) {
            PyObject clampObj = meth.getValue();
            String methodName = (String)clampObj.__getattr__("name").__tojava__(String.class);
            Class<?> returnClass = (Class<?>)clampObj.__getattr__("returntype").__tojava__(Class.class);
            Class<?>[] thrownClasses = Py.tojava(clampObj.__getattr__("throws"), Class[].class);
            Class<?>[] parameterClasses = Py.tojava(clampObj.__getattr__("argtypes"), Class[].class);
            AnnotationDescr[] methodAnnotations = extractAnnotation((PyDictionary)clampObj.__getattr__("method_annotations"));
            PyObject[] parameterAnnotationObjs = (PyObject[])clampObj.__getattr__("parameter_annotations").__tojava__(PyObject[].class);
            
            AnnotationDescr[][]parameterAnnotations = new AnnotationDescr[parameterAnnotationObjs.length][];
            for (int i = 0; i<parameterAnnotationObjs.length; i++) {
                if (parameterAnnotationObjs[i].isMappingType()) {
                    parameterAnnotations[i] = extractAnnotation((PyDictionary)parameterAnnotationObjs[i]);
                }
            }

            addMethod(methodName, meth.getKey(), (Class<?>)returnClass, parameterClasses, thrownClasses,
                    Modifier.PUBLIC, superclass, methodAnnotations, parameterAnnotations);
              
        }
    }
}

