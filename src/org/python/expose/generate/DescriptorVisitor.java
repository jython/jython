package org.python.expose.generate;

abstract class DescriptorVisitor extends RestrictiveAnnotationVisitor {

    private String val;

    private String doc;

    DescriptorVisitor(String defaultName) {
        val = defaultName;
    }

    @Override
    public void visit(String name, Object value) {
        if (name.equals("name")) {
            val = (String)value;
        } else if (name.equals("doc")) {
            doc = (String)value;
        } else {
            super.visit(name, value);
        }
    }

    @Override
    public void visitEnd() {
        handleResult(val, doc);
    }

    public abstract void handleResult(String name, String doc);
}
