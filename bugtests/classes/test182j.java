
public abstract class test182j {
    public String tstPublic() {
       return "tstPublic";
    }

    protected String tstProtected() {
       return "tstProtected";
    }

    final protected String tstFinalProtected() {
       return "tstFinalProtected";
    }

    final public String tstFinalPublic() {
       return "tstFinalPublic";
    }



    public String tstOverridePublic() {
       return "tstOverridePublic";
    }

    protected String tstOverrideProtected() {
       return "tstOverrideProtected";
    }

    final protected String tstOverrideFinalProtected() {
       return "tstOverrideFinalProtected";
    }

    final public String tstOverrideFinalPublic() {
       return "tstOverrideFinalPublic";
    }


    abstract public String tstAbstractPublic();

    abstract protected String tstAbstractProtected();

    abstract public String tstOverrideAbstractPublic();

    abstract protected String tstOverrideAbstractProtected();

}