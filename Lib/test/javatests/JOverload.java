package javatests;

public class JOverload {
// ov_posprecXX
  public String ov_posprec1(int a,long b) { return "(int,long)"; }
  public String ov_posprec1(long a,int b) { return "(long,int)"; }

  public String ov_posprec2(long a,int b) { return "(long,int)"; }


// ov_scalXX
  public String ov_scal1(long a) { return "(long)"; }
  public String ov_scal1(int a) { return "(int)"; }
  public String ov_scal1(short a) { return "(short)"; }
  public String ov_scal1(char a) { return "(char)"; }
  public String ov_scal1(byte a) { return "(byte)"; }
  public String ov_scal1(double a) { return "(double)"; }
  public String ov_scal1(float a) { return "(float)"; }
  public String ov_scal1(boolean a) { return "(boolean)"; }
  public String ov_scal1(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal1(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal1(Foo a) { return "(Foo)"; }
  public String ov_scal1(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal1(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal1(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal2(int a) { return "(int)"; }
  public String ov_scal2(short a) { return "(short)"; }
  public String ov_scal2(char a) { return "(char)"; }
  public String ov_scal2(byte a) { return "(byte)"; }
  public String ov_scal2(double a) { return "(double)"; }
  public String ov_scal2(float a) { return "(float)"; }
  public String ov_scal2(boolean a) { return "(boolean)"; }
  public String ov_scal2(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal2(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal2(Foo a) { return "(Foo)"; }
  public String ov_scal2(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal2(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal2(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal3(short a) { return "(short)"; }
  public String ov_scal3(char a) { return "(char)"; }
  public String ov_scal3(byte a) { return "(byte)"; }
  public String ov_scal3(double a) { return "(double)"; }
  public String ov_scal3(float a) { return "(float)"; }
  public String ov_scal3(boolean a) { return "(boolean)"; }
  public String ov_scal3(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal3(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal3(Foo a) { return "(Foo)"; }
  public String ov_scal3(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal3(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal3(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal4(char a) { return "(char)"; }
  public String ov_scal4(byte a) { return "(byte)"; }
  public String ov_scal4(double a) { return "(double)"; }
  public String ov_scal4(float a) { return "(float)"; }
  public String ov_scal4(boolean a) { return "(boolean)"; }
  public String ov_scal4(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal4(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal4(Foo a) { return "(Foo)"; }
  public String ov_scal4(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal4(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal4(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal5(byte a) { return "(byte)"; }
  public String ov_scal5(double a) { return "(double)"; }
  public String ov_scal5(float a) { return "(float)"; }
  public String ov_scal5(boolean a) { return "(boolean)"; }
  public String ov_scal5(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal5(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal5(Foo a) { return "(Foo)"; }
  public String ov_scal5(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal5(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal5(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal6(double a) { return "(double)"; }
  public String ov_scal6(float a) { return "(float)"; }
  public String ov_scal6(boolean a) { return "(boolean)"; }
  public String ov_scal6(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal6(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal6(Foo a) { return "(Foo)"; }
  public String ov_scal6(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal6(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal6(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal7(float a) { return "(float)"; }
  public String ov_scal7(boolean a) { return "(boolean)"; }
  public String ov_scal7(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal7(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal7(Foo a) { return "(Foo)"; }
  public String ov_scal7(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal7(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal7(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal8(boolean a) { return "(boolean)"; }
  public String ov_scal8(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal8(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal8(Foo a) { return "(Foo)"; }
  public String ov_scal8(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal8(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal8(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal9(java.lang.String a) { return "(java.lang.String)"; }
  public String ov_scal9(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal9(Foo a) { return "(Foo)"; }
  public String ov_scal9(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal9(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal9(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal10(SubFoo a) { return "(SubFoo)"; }
  public String ov_scal10(Foo a) { return "(Foo)"; }
  public String ov_scal10(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal10(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal10(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal11(Foo a) { return "(Foo)"; }
  public String ov_scal11(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal11(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal11(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal12(java.lang.Class a) { return "(java.lang.Class)"; }
  public String ov_scal12(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal12(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal13(java.io.Serializable a) { return "(java.io.Serializable)"; }
  public String ov_scal13(java.lang.Object a) { return "(java.lang.Object)"; }

  public String ov_scal14(java.lang.Object a) { return "(java.lang.Object)"; }


}
