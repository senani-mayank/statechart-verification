package ast;

public class Declaration {
  public final String vname;
  public final String typeName;
  private DeclarationList declarationList;
  public final boolean input; // whether the declaration is input variable or not.

  private Type type;

  public Declaration(String vname, String typeName, boolean input) {
    this.vname    = vname;
    this.typeName = typeName;
    this.input    = input;
  }

  public State getState() {
    return this.declarationList.getState();
  }

  public String getFullVName() {
    return this.getState().getFullName() + this.vname;
  }

  public Type getType() {
    return this.type;
  }

  public String toString() {
    String s = this.vname;
    if(this.input) {
      s += " #";
    }
    s += " : " + this.typeName + ";";
    return s;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setDeclarationList(DeclarationList declarationList) {
    this.declarationList = declarationList;
  }
}
