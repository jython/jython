// Copyright © Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;

/** Abstract package manager.
 */
public abstract class PackageManager extends Object {

    public PyJavaPackage topLevelPackage;

    public PackageManager() {
        topLevelPackage = new PyJavaPackage("",this);
    }

    abstract public Class findClass(String pkg,String name,String reason);
    
    public Class findClass(String pkg,String name) {
        return findClass(pkg,name,"java class");
    }

    public void notifyPackageImport(String pkg,String name) {}
    
    /** Dynamically check if pkg.name exists as java pkg in the controlled hierarchy.
     * Should be overriden.
     * @param pkg parent pkg name
     * @param name candidate name
     * @return true if pkg exists
     */
    public abstract boolean packageExists(String pkg,String name);

    /** Reports the specified package content names. Should be overriden.
     * Used by {@link PyJavaPackage#__dir__} and {@link PyJavaPackage#fillDir}.
     * @return resulting list of names (PyList of PyString)
     * @param jpkg queried package
     * @param instantiate if true then instatiate reported names in package dict
     * @param exclpkgs exclude packages (just when instantiate is false)
     */
    public abstract PyList doDir(PyJavaPackage jpkg,boolean instantiate,boolean exclpkgs);

    /** Basic helper implementation of {@link #doDir}.
     * It merges information from jpkg {@link PyJavaPackage#clsSet} and {@link PyJavaPackage#__dict__}.
     */
    protected PyList basicDoDir(PyJavaPackage jpkg,boolean instantiate,boolean exclpkgs) {
        PyStringMap dict =  jpkg.__dict__;
        PyStringMap cls = jpkg.clsSet;

        if(!instantiate) {
            PyList ret = cls.keys();

            PyList dictKeys = dict.keys();

            for(int i=0; i < dictKeys.__len__(); i++) {
                PyObject name = dictKeys.get(i);
                if (!cls.has_key(name)) {
                    if (exclpkgs && dict.get(name) instanceof PyJavaPackage) continue;
                    ret.append(name);
                }
            }

            return ret;
        }

        PyList clsNames = cls.keys();

        for(int i=0; i < clsNames.__len__(); i++) {
            PyObject name = clsNames.get(i);
            if(!dict.has_key(name)) jpkg.addLazyClass(name.toString());
        }

        return dict.keys();
    }

    /** Helper merging list2 into list1. Returns list1.
     */
    protected PyList merge(PyList list1,PyList list2) {
        for(int i=0; i < list2.__len__() ;i++) {
            PyObject name = list2.get(i);
            list1.append(name);
        }

        return list1;
    }

    public PyObject lookupName(String name) {
        PyObject top = topLevelPackage;
        do {
            int dot = name.indexOf('.');
            String firstName = name;
            String lastName = null;
            if (dot != -1) {
                firstName = name.substring(0,dot);
                lastName = name.substring(dot+1, name.length());
            }
            firstName = firstName.intern();
            top = top.__findattr__(firstName);
            if (top == null) return null;
            // ??pending: test for jpkg/jclass?
            name = lastName;
        } while (name != null);
        return top;
    }

    /** Creates package/updates statically known classes info.
     * Uses {@link PyJavaPackage#addPackage(java.lang.String, java.lang.String) }, {@link PyJavaPackage#addPlaceholders}.
     *
     * @param name package name
     * @param classes comma-separated string
     * @param jarfile involved jarfile; can be null
     * @return created/updated package
     */
    public PyJavaPackage makeJavaPackage(String name,String classes,String jarfile) {
        PyJavaPackage p = topLevelPackage;
        if(name.length() != 0) p=p.addPackage(name,jarfile);

        if (classes != null) p.addPlaceholders(classes);
        
        return p;
    }

    private static final int MAXSKIP = 512;
    
    /** Check that a given stream is a valid Java .class file.
     * And return its access permissions as an int.
     */
    static protected int checkAccess(java.io.InputStream cstream) throws java.io.IOException {
        java.io.DataInputStream istream=new java.io.DataInputStream(cstream);

        int magic = istream.readInt();
        int minor = istream.readShort();
        int major = istream.readShort();
        if (magic != 0xcafebabe) return -1;
        // Check versions???
        //System.out.println("magic: "+magic+", "+major+", "+minor);
        int nconstants = istream.readShort();
        for(int i=1; i<nconstants; i++) {
            int cid = istream.readByte();
            //System.out.println(""+i+" : "+cid);
            switch (cid) {
                case 7: istream.skipBytes(2); break;
                case 9:
                case 10:
                case 11: istream.skipBytes(4); break;
                case 8: istream.skipBytes(2); break;
                case 3:
                case 4: istream.skipBytes(4); break;
                case 5:
                case 6: istream.skipBytes(8); i++; break;
                case 12: istream.skipBytes(4); break;
                case 1:
                //System.out.println("utf: "+istream.readUTF()+";");
                int slength = istream.readUnsignedShort();
                while (slength > MAXSKIP) { // workaround to java1.1 bug
                    istream.skipBytes(MAXSKIP);
                    slength -= MAXSKIP;
                }
                istream.skipBytes(slength);
                break;
                default:
                //System.err.println("unexpected cid: "+cid+", "+i+", "+nconstants);
                //for (int j=0; j<10; j++) { System.err.print(", "+istream.readByte()); }
                //System.err.println();
                return -1;
            }
        }
        return istream.readShort();
    }


}
