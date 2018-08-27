package flatten;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import ast.*;

interface Translator {
  Statechart translate(Statechart S) throws Exception;
}

public class Flattener implements Translator {
  private Translator globaliser = new Globaliser();
  private Translator xt1        = new XT1();
  private Translator xt2        = new XT2();
  private Translator xt3        = new XT3();

  public Statechart translate(Statechart S) throws Exception {
    return 
      this.xt3.translate(
      this.xt2.translate(
      this.xt1.translate(
      this.globaliser.translate(S)
      )
      )
    );
  }
}

class Globaliser implements Translator {

  Map<Declaration, Declaration> globalDeclarations = new HashMap<Declaration, Declaration>();
  public Statechart translate(Statechart S) throws Exception {
    this.makeGlobalDeclarations(S);
    
    return this.globaliseStatechart(S);
  }

  private void makeGlobalDeclarations(State state) {
    for(Declaration dec : state.declarations) {
      String underscoredName = this.underscoreFullName(dec.getFullVName());
      while(this.globalDeclarations.get(underscoredName) != null) {
        underscoredName += "1";
      }
      Declaration newdec = new Declaration(
        underscoredName,
        dec.typeName,
        dec.input);
      this.globalDeclarations.put(dec, newdec);
    }
    for(State s : state.states) {
      this.makeGlobalDeclarations(s);
    }
  }

  private Statechart globaliseStatechart(Statechart statechart) throws Exception {
    DeclarationList declarations = new DeclarationList();
    for(Declaration declaration : this.globalDeclarations.keySet()) {
      declarations.add(this.globalDeclarations.get(declaration));
    }
    List<State> newStates = new ArrayList<State>();
    for(State s : statechart.states) {
      newStates.add(this.globaliseState(s));
    }
    List<Transition> newTransitions = new ArrayList<Transition>();
    for(Transition t : statechart.transitions) {
      newTransitions.add(this.globaliseTransition(t));
    }

    return new Statechart(
      statechart.name,
      statechart.types,
      statechart.events,
      declarations,
      this.globaliseStatement(statechart.entry),
      this.globaliseStatement(statechart.exit),
      statechart.functionDeclarations,
      newStates,
      newTransitions
    );
  }


  private State globaliseState(State state) throws Exception {
    List<State> newStates = new ArrayList<State>();
    for(State s : state.states) {
      newStates.add(this.globaliseState(s));
    }
    List<Transition> newTransitions = new ArrayList<Transition>();
    for(Transition t : state.transitions) {
      newTransitions.add(this.globaliseTransition(t));
    }
    return new State(
      state.name,
      new DeclarationList(),
      this.globaliseStatement(state.entry),
      this.globaliseStatement(state.exit),
      newStates,
      newTransitions
    );
  }

  private Transition globaliseTransition(Transition t) throws Exception {
    return new Transition(t.name,
      t.sourceName,
      t.destinationName,
      t.trigger,
      this.globaliseExpression(t.guard),
      this.globaliseStatement(t.action)
    );
  }

  private Statement globaliseStatement(Statement s) throws Exception {
    if(s instanceof AssignmentStatement) {
      return globaliseAssignmentStatement((AssignmentStatement)s);
    }
    else if(s instanceof StatementList) {
      return globaliseStatementList((StatementList)s);
    }
    else if(s instanceof ExpressionStatement) {
      return globaliseExpressionStatement((ExpressionStatement)s);
    }
    else if(s instanceof IfStatement) {
      return globaliseIfStatement((IfStatement)s);
    }
    else if(s instanceof WhileStatement) {
      return globaliseWhileStatement((WhileStatement)s);
    }
    else {
      throw new Exception("Globaliser.globaliseStatement : unknown error for " + s);
    }
  }

  private ExpressionStatement globaliseExpressionStatement(ExpressionStatement s) throws Exception {
    return new ExpressionStatement(this.globaliseExpression(s.expression));
  }

  private IfStatement globaliseIfStatement(IfStatement s) throws Exception {
    return new IfStatement(
      this.globaliseExpression(s.condition),
      this.globaliseStatement(s.then_body),
      this.globaliseStatement(s.else_body)
    );
  }

  private WhileStatement globaliseWhileStatement(WhileStatement s) throws Exception {
    return new WhileStatement(
      this.globaliseExpression(s.condition),
      this.globaliseStatement(s.body)
    );
  }

  private AssignmentStatement globaliseAssignmentStatement(AssignmentStatement s) throws Exception {
    Name oldName = s.lhs;
    Declaration oldDeclaration = oldName.getDeclaration();
    Declaration newDeclaration = globalDeclarations.get(oldDeclaration);
    if(newDeclaration == null) {
      throw new Exception("No global declaration for " + newDeclaration.vname + " found.");
    }
    Name newName = new Name(newDeclaration.vname);

    Expression oldExpression = s.rhs;
    Expression newExpression = this.globaliseExpression(oldExpression);

    return new AssignmentStatement(newName, newExpression);
  }

  private Expression globaliseExpression(Expression e) throws Exception {
    if(e instanceof BinaryExpression) {
      return this.globaliseBinaryExpression((BinaryExpression)e);
    }
    else if(e instanceof BooleanConstant) {
      return this.globaliseBooleanConstant((BooleanConstant)e);
    }
    else if(e instanceof FunctionCall) {
      return this.globaliseFunctionCall((FunctionCall)e);
    }
    else if(e instanceof IntegerConstant) {
      return this.globaliseIntegerConstant((IntegerConstant)e);
    }
    else if(e instanceof Name) {
      return this.globaliseName((Name)e);
    }
    else if(e instanceof StringLiteral) {
      return this.globaliseStringLiteral((StringLiteral)e);
    }
    else {
      throw new Exception("Globaliser.globaliseExpression : unknown error for " + e);
    }

  }
  private BinaryExpression globaliseBinaryExpression(BinaryExpression e) throws Exception {
    return new BinaryExpression(
      this.globaliseExpression(e.left),
      this.globaliseExpression(e.right),
      e.operator
    );
  }

  private BooleanConstant globaliseBooleanConstant(BooleanConstant e) throws Exception {
    return e;
  }

  private FunctionCall globaliseFunctionCall (FunctionCall e) throws Exception {
    List<Expression> newArgumentList = new ArrayList<Expression>();
    for(Expression arg : e.argumentList) {
      newArgumentList.add(this.globaliseExpression(arg));
    }
    return new FunctionCall(e.name, newArgumentList);
  }

  private IntegerConstant globaliseIntegerConstant(IntegerConstant e) throws Exception {
    return e;
  }

  private Name globaliseName(Name e) throws Exception {
    System.out.println("Globalising name = " + e);
    Declaration declaration = e.getDeclaration();
    System.out.println("Declaration = " + declaration);
    String fullVName = declaration.getFullVName();
    System.out.println("full variable name = " + fullVName);
    Declaration globalisedName = this.globalDeclarations.get(declaration);
    System.out.println("globalised name = " + globalisedName);
    return new Name(globalisedName.vname);
  }

  private StringLiteral globaliseStringLiteral(StringLiteral e) throws Exception {
    return e;
  }

  private UnaryExpression globaliseUnaryExpression(UnaryExpression e) throws Exception {
    return new UnaryExpression(
      this.globaliseExpression(e.expression),
      e.operator
    );
  }

  private StatementList globaliseStatementList(StatementList oldStatementList) throws Exception {
    StatementList newStatementList = new StatementList();

    for(Statement statement : oldStatementList.getStatements()) {
      newStatementList.add(globaliseStatement(statement));
    }
    return newStatementList;
  }

  private String underscoreFullName(String name) {
    String underscoredName = "";

    for(char c : name.toCharArray()) {
      if(c == '.') {
        underscoredName += "_";
      }
      else {
        underscoredName += c;
      }
    }
    return underscoredName;
  }
}

// Translator for transitions with non-atomic sources
class XT1 implements Translator {
  public Statechart translate(Statechart S) {
    return S;
  }
}

// Translator for transitions with non-atomic destination
class XT2 implements Translator {
  public Statechart translate(Statechart S) {
    return S;
  }
}

// Translator for for transitions with atomic source and destination.
class XT3 implements Translator {
  public Statechart translate(Statechart S) {
    return S;
  }
}
