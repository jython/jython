// Copyright © Corporation for National Research Initiatives
package org.python.compiler;

import java.util.*;
import java.io.*;

class Bytes {
    public byte[] data;
    Bytes(ByteArrayOutputStream data) {
	this.data = data.toByteArray();
    }

    public boolean equals(Object o) {
	if (o instanceof Bytes) {
	    byte[] odata = ((Bytes)o).data;
	    int n = data.length;
	    if (odata.length != n)
		return false;
	    for (int i=0; i<n; i++) {
		if (data[i] != odata[i])
		    return false;
	    }
	    return true;
	}
	return false;
    }

    public int hashCode() {
	int h=0xa538;
	int n = data.length;
	for(int i=0; i<n; i++) {
	    h = h ^ data[i];
	}
	return h;
    }
}


public class ConstantPool {
    Hashtable constants;
    int index;
    DataOutputStream tdata;
    ByteArrayOutputStream pool, tarray;
    int[] sizes;

    public ConstantPool() {
	constants = new Hashtable();
	index = 0;
	pool = new ByteArrayOutputStream();
	tarray = new ByteArrayOutputStream();
	tdata = new DataOutputStream(tarray);
	sizes = new int[256];
    }

    public void write(DataOutputStream stream) throws IOException {
	stream.writeShort(index+1);
	stream.write(pool.toByteArray());
    }

    public int addConstant(int slots) throws IOException {
	//tarray.flush();
	//byte[] data = tarray.toByteArray();
	Bytes data = new Bytes(tarray);
	tarray.reset();
	Integer i = (Integer)constants.get(data);
	if (i == null) {
	    pool.write(data.data);
	    i = new Integer(index);
	    constants.put(data, i);
	    if (index+1 >= sizes.length) {
		int[] new_sizes = new int[sizes.length*2];
		System.arraycopy(sizes, 0, new_sizes, 0, sizes.length);
		sizes = new_sizes;
	    }
	    sizes[index+1] = slots;
	    index += slots;
	} 
	//System.out.print("Constant: ");
	//for(int j=0; j<data.length; j++)
	//	System.out.print(Integer.toString(data[j])+", ");
	//System.out.println("");
	return i.intValue()+1;
    }

    public int UTF8(String s) throws IOException {
	tdata.writeByte(1);
	tdata.writeUTF(s);
	return addConstant(1); 
    }

    public int Class(String s) throws IOException {
	int c = UTF8(s);
	tdata.writeByte(7);
	tdata.writeShort(c);
	return addConstant(1); 
    }

    public int Fieldref(String c, String name, String type)
	throws IOException
    {
	int ic = Class(c);
	int nt = NameAndType(name, type);
	tdata.writeByte(9);
	tdata.writeShort(ic);
	tdata.writeShort(nt);
	int size = 1;
	if (type.equals("D") || type.equals("J"))
	    size = 2;
	int index = addConstant(1);
	sizes[index] = size;
	//System.out.println("field: "+c+", "+name+", "+type+": "+index);
	return index;
    }

    public static int sigSize(String sig, boolean includeReturn) {
	int stack = 0;
	int i = 0;
	char[] c = sig.toCharArray();
	int n = c.length;
	boolean ret=false;
	boolean array=false;

	while (++i<n) {
	    switch (c[i]) {
	    case ')':
		if (!includeReturn)
		    return stack;
		ret=true;
		continue;
	    case '[':
		array=true;
		continue;
	    case 'V':
		continue;
	    case 'D':
	    case 'J':
		if (array) {
		    if (ret) stack += 1;
		    else stack -=1;
		    array = false;
		} else {
		    if (ret) stack += 2;
		    else stack -=2;
		}
		break;
	    case 'L':
		while (c[++i] != ';') {;}
	    default:
		if (ret) stack++;
		else stack--;
		array = false;
	    }
	}
	return stack;
    }

    public int Methodref(String c, String name, String type)
	throws IOException
    {
	int ic = Class(c);
	int nt = NameAndType(name, type);

	tdata.writeByte(10);
	tdata.writeShort(ic);
	tdata.writeShort(nt);
	int index = addConstant(1);
	sizes[index] = sigSize(type, true);
	//System.out.println("method: "+c+", "+name+", "+type+": "+index);
	return index;	
    }

    public int InterfaceMethodref(String c, String name, String type)
	throws IOException
    {
	int ic = Class(c);
	int nt = NameAndType(name, type);

	tdata.writeByte(11);
	tdata.writeShort(ic);
	tdata.writeShort(nt);
	int index = addConstant(1);
	sizes[index] = sigSize(type, true);
	return index;	
    }

    public int String(String s) throws IOException {
	int i = UTF8(s); 
	tdata.writeByte(8);
	tdata.writeShort(i);
	return addConstant(1); 
    }

    public int Integer(int i) throws IOException {
	tdata.writeByte(3);
	tdata.writeInt(i);
	return addConstant(1); 
    }

    public int Float(float f) throws IOException {
	tdata.writeByte(4);
	tdata.writeFloat(f);
	return addConstant(1); 
    }

    public int Long(long l) throws IOException {
	tdata.writeByte(5);
	tdata.writeLong(l);
	return addConstant(2); 
    }

    public int Double(double d) throws IOException {
	tdata.writeByte(6);
	tdata.writeDouble(d);
	return addConstant(2); 
    }

    public int NameAndType(String name, String type) throws IOException {
	int n = UTF8(name);
	int t = UTF8(type);

	tdata.writeByte(12);
	tdata.writeShort(n);
	tdata.writeShort(t);
	return addConstant(1); 
    }

    public static void main(String[] args) throws Exception {
	ConstantPool cp = new ConstantPool();

	System.out.println("c: "+cp.Class("org/python/core/PyString"));
	System.out.println("c: "+cp.Class("org/python/core/PyString"));

	for(int i=0; i<args.length; i++)
	    System.out.println(args[i]+": "+sigSize(args[i], true));
    }
}
