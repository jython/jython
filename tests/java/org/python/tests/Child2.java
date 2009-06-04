package org.python.tests;

public class Child2 extends Parent {

    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = "Child2 " + value;
    }
}
