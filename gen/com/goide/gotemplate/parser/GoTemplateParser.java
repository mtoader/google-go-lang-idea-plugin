// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.goide.gotemplate.GoTemplateTypes.*;
import static com.goide.gotemplate.parser.GoTemplateParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class GoTemplateParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == AND_EXPR) {
      r = Expression(b, 0, 0);
    }
    else if (t == BLOCK_STATEMENT) {
      r = BlockStatement(b, 0);
    }
    else if (t == CONDITIONAL_EXPR) {
      r = Expression(b, 0, 1);
    }
    else if (t == ELSE_STATEMENT) {
      r = ElseStatement(b, 0);
    }
    else if (t == END_STATEMENT) {
      r = EndStatement(b, 0);
    }
    else if (t == EXPRESSION) {
      r = Expression(b, 0, -1);
    }
    else if (t == FIELD_CHAIN) {
      r = FieldChain(b, 0);
    }
    else if (t == FIELD_CHAIN_EXPR) {
      r = FieldChainExpr(b, 0);
    }
    else if (t == IDENT_LITERAL) {
      r = IdentLiteral(b, 0);
    }
    else if (t == IF_STATEMENT) {
      r = IfStatement(b, 0);
    }
    else if (t == LITERAL) {
      r = Literal(b, 0);
    }
    else if (t == LITERAL_EXPR) {
      r = LiteralExpr(b, 0);
    }
    else if (t == LOCAL_IDENT_LITERAL) {
      r = LocalIdentLiteral(b, 0);
    }
    else if (t == NUMBER_LITERAL) {
      r = NumberLiteral(b, 0);
    }
    else if (t == OR_EXPR) {
      r = Expression(b, 0, -1);
    }
    else if (t == PARENTHESES_EXPR) {
      r = ParenthesesExpr(b, 0);
    }
    else if (t == PIPE) {
      r = Pipe(b, 0);
    }
    else if (t == PIPELINE) {
      r = Pipeline(b, 0);
    }
    else if (t == PIPELINE_STATEMENT) {
      r = PipelineStatement(b, 0);
    }
    else if (t == RANGE_ASSIGN) {
      r = RangeAssign(b, 0);
    }
    else if (t == RANGE_STATEMENT) {
      r = RangeStatement(b, 0);
    }
    else if (t == STATEMENT_LIST) {
      r = StatementList(b, 0);
    }
    else if (t == STRING_LITERAL) {
      r = StringLiteral(b, 0);
    }
    else if (t == TEMPLATE_STATEMENT) {
      r = TemplateStatement(b, 0);
    }
    else if (t == WITH_STATEMENT) {
      r = WithStatement(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return File(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(AND_EXPR, CONDITIONAL_EXPR, EXPRESSION, FIELD_CHAIN_EXPR,
      LITERAL_EXPR, OR_EXPR, PARENTHESES_EXPR),
    create_token_set_(BLOCK_STATEMENT, ELSE_STATEMENT, END_STATEMENT, IF_STATEMENT,
      PIPELINE_STATEMENT, RANGE_STATEMENT, TEMPLATE_STATEMENT, WITH_STATEMENT),
  };

  /* ********************************************************** */
  // '{{' 'block' Pipeline '}}' StatementList EndStatement
  public static boolean BlockStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "BlockStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, BLOCK);
    p = r; // pin = 2
    r = r && report_error_(b, Pipeline(b, l + 1));
    r = p && report_error_(b, consumeToken(b, RDOUBLE_BRACE)) && r;
    r = p && report_error_(b, StatementList(b, l + 1)) && r;
    r = p && EndStatement(b, l + 1) && r;
    exit_section_(b, l, m, BLOCK_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '{{' 'else' '}}' StatementList EndStatement
  public static boolean ElseStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ElseStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, ELSE);
    p = r; // pin = 2
    r = r && report_error_(b, consumeToken(b, RDOUBLE_BRACE));
    r = p && report_error_(b, StatementList(b, l + 1)) && r;
    r = p && EndStatement(b, l + 1) && r;
    exit_section_(b, l, m, ELSE_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '{{' 'end' '}}'
  public static boolean EndStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "EndStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, END);
    p = r; // pin = 2
    r = r && consumeToken(b, RDOUBLE_BRACE);
    exit_section_(b, l, m, END_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '.' IDENT FieldChain?
  public static boolean FieldChain(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldChain")) return false;
    if (!nextTokenIs(b, DOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && consumeToken(b, IDENT);
    r = r && FieldChain_2(b, l + 1);
    exit_section_(b, m, FIELD_CHAIN, r);
    return r;
  }

  // FieldChain?
  private static boolean FieldChain_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldChain_2")) return false;
    FieldChain(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // StatementList
  static boolean File(PsiBuilder b, int l) {
    return StatementList(b, l + 1);
  }

  /* ********************************************************** */
  // IDENT|'.'
  public static boolean IdentLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IdentLiteral")) return false;
    if (!nextTokenIs(b, "<ident literal>", DOT, IDENT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<ident literal>");
    r = consumeToken(b, IDENT);
    if (!r) r = consumeToken(b, DOT);
    exit_section_(b, l, m, IDENT_LITERAL, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '{{' 'if' Pipeline '}}' StatementList (EndStatement|ElseStatement)
  public static boolean IfStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, IF);
    p = r; // pin = 2
    r = r && report_error_(b, Pipeline(b, l + 1));
    r = p && report_error_(b, consumeToken(b, RDOUBLE_BRACE)) && r;
    r = p && report_error_(b, StatementList(b, l + 1)) && r;
    r = p && IfStatement_5(b, l + 1) && r;
    exit_section_(b, l, m, IF_STATEMENT, r, p, null);
    return r || p;
  }

  // EndStatement|ElseStatement
  private static boolean IfStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "IfStatement_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = EndStatement(b, l + 1);
    if (!r) r = ElseStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LocalIdentLiteral | NumberLiteral | IdentLiteral | StringLiteral
  public static boolean Literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<literal>");
    r = LocalIdentLiteral(b, l + 1);
    if (!r) r = NumberLiteral(b, l + 1);
    if (!r) r = IdentLiteral(b, l + 1);
    if (!r) r = StringLiteral(b, l + 1);
    exit_section_(b, l, m, LITERAL, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // VAR_IDENT
  public static boolean LocalIdentLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LocalIdentLiteral")) return false;
    if (!nextTokenIs(b, VAR_IDENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VAR_IDENT);
    exit_section_(b, m, LOCAL_IDENT_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // NUMBER
  public static boolean NumberLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "NumberLiteral")) return false;
    if (!nextTokenIs(b, NUMBER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NUMBER);
    exit_section_(b, m, NUMBER_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // '|' Expression+ Pipe?
  public static boolean Pipe(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pipe")) return false;
    if (!nextTokenIs(b, BIT_OR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, BIT_OR);
    p = r; // pin = 1
    r = r && report_error_(b, Pipe_1(b, l + 1));
    r = p && Pipe_2(b, l + 1) && r;
    exit_section_(b, l, m, PIPE, r, p, null);
    return r || p;
  }

  // Expression+
  private static boolean Pipe_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pipe_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = Expression(b, l + 1, -1);
    int c = current_position_(b);
    while (r) {
      if (!Expression(b, l + 1, -1)) break;
      if (!empty_element_parsed_guard_(b, "Pipe_1", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // Pipe?
  private static boolean Pipe_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pipe_2")) return false;
    Pipe(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // Expression Expression*  Pipe?
  public static boolean Pipeline(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pipeline")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<pipeline>");
    r = Expression(b, l + 1, -1);
    p = r; // pin = 1
    r = r && report_error_(b, Pipeline_1(b, l + 1));
    r = p && Pipeline_2(b, l + 1) && r;
    exit_section_(b, l, m, PIPELINE, r, p, null);
    return r || p;
  }

  // Expression*
  private static boolean Pipeline_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pipeline_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!Expression(b, l + 1, -1)) break;
      if (!empty_element_parsed_guard_(b, "Pipeline_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // Pipe?
  private static boolean Pipeline_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "Pipeline_2")) return false;
    Pipe(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '{{' Pipeline '}}'
  public static boolean PipelineStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PipelineStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && Pipeline(b, l + 1);
    p = r; // pin = 2
    r = r && consumeToken(b, RDOUBLE_BRACE);
    exit_section_(b, l, m, PIPELINE_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // Expression (',' Expression)? ':='
  public static boolean RangeAssign(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RangeAssign")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<range assign>");
    r = Expression(b, l + 1, -1);
    r = r && RangeAssign_1(b, l + 1);
    r = r && consumeToken(b, VAR_ASSIGN);
    exit_section_(b, l, m, RANGE_ASSIGN, r, false, null);
    return r;
  }

  // (',' Expression)?
  private static boolean RangeAssign_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RangeAssign_1")) return false;
    RangeAssign_1_0(b, l + 1);
    return true;
  }

  // ',' Expression
  private static boolean RangeAssign_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RangeAssign_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && Expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '{{' 'range' RangeAssign? Pipeline '}}' StatementList EndStatement
  public static boolean RangeStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RangeStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, RANGE);
    p = r; // pin = 2
    r = r && report_error_(b, RangeStatement_2(b, l + 1));
    r = p && report_error_(b, Pipeline(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, RDOUBLE_BRACE)) && r;
    r = p && report_error_(b, StatementList(b, l + 1)) && r;
    r = p && EndStatement(b, l + 1) && r;
    exit_section_(b, l, m, RANGE_STATEMENT, r, p, null);
    return r || p;
  }

  // RangeAssign?
  private static boolean RangeStatement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RangeStatement_2")) return false;
    RangeAssign(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // StatementsGroup*
  public static boolean StatementList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StatementList")) return false;
    Marker m = enter_section_(b, l, _NONE_, "<statement list>");
    int c = current_position_(b);
    while (true) {
      if (!StatementsGroup(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "StatementList", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, STATEMENT_LIST, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // TEXT+ | IfStatement | BlockStatement | TemplateStatement | RangeStatement | WithStatement | PipelineStatement
  static boolean StatementsGroup(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StatementsGroup")) return false;
    if (!nextTokenIs(b, "", LDOUBLE_BRACE, TEXT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = StatementsGroup_0(b, l + 1);
    if (!r) r = IfStatement(b, l + 1);
    if (!r) r = BlockStatement(b, l + 1);
    if (!r) r = TemplateStatement(b, l + 1);
    if (!r) r = RangeStatement(b, l + 1);
    if (!r) r = WithStatement(b, l + 1);
    if (!r) r = PipelineStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // TEXT+
  private static boolean StatementsGroup_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StatementsGroup_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, TEXT);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, TEXT)) break;
      if (!empty_element_parsed_guard_(b, "StatementsGroup_0", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // RAW_STRING|STRING
  public static boolean StringLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "StringLiteral")) return false;
    if (!nextTokenIs(b, "<string literal>", RAW_STRING, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<string literal>");
    r = consumeToken(b, RAW_STRING);
    if (!r) r = consumeToken(b, STRING);
    exit_section_(b, l, m, STRING_LITERAL, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '{{' 'template' Pipeline '}}'
  public static boolean TemplateStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "TemplateStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, TEMPLATE);
    p = r; // pin = 2
    r = r && report_error_(b, Pipeline(b, l + 1));
    r = p && consumeToken(b, RDOUBLE_BRACE) && r;
    exit_section_(b, l, m, TEMPLATE_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '{{' 'with' Pipeline '}}' StatementList EndStatement
  public static boolean WithStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WithStatement")) return false;
    if (!nextTokenIs(b, LDOUBLE_BRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LDOUBLE_BRACE);
    r = r && consumeToken(b, WITH);
    p = r; // pin = 2
    r = r && report_error_(b, Pipeline(b, l + 1));
    r = p && report_error_(b, consumeToken(b, RDOUBLE_BRACE)) && r;
    r = p && report_error_(b, StatementList(b, l + 1)) && r;
    r = p && EndStatement(b, l + 1) && r;
    exit_section_(b, l, m, WITH_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '==' | '!=' | '<' | '<=' | '>' | '>='
  static boolean rel_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rel_op")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQ);
    if (!r) r = consumeToken(b, NOT_EQ);
    if (!r) r = consumeToken(b, LESS);
    if (!r) r = consumeToken(b, LESS_OR_EQUAL);
    if (!r) r = consumeToken(b, GREATER);
    if (!r) r = consumeToken(b, GREATER_OR_EQUAL);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // Expression root: Expression
  // Operator priority table:
  // 0: BINARY(OrExpr)
  // 1: BINARY(AndExpr)
  // 2: BINARY(ConditionalExpr)
  // 3: ATOM(FieldChainExpr)
  // 4: ATOM(LiteralExpr)
  // 5: ATOM(ParenthesesExpr)
  public static boolean Expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = FieldChainExpr(b, l + 1);
    if (!r) r = LiteralExpr(b, l + 1);
    if (!r) r = ParenthesesExpr(b, l + 1);
    p = r;
    r = r && Expression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean Expression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "Expression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 0 && consumeTokenSmart(b, COND_OR)) {
        r = Expression(b, l, 0);
        exit_section_(b, l, m, OR_EXPR, r, true, null);
      }
      else if (g < 1 && consumeTokenSmart(b, COND_AND)) {
        r = Expression(b, l, 1);
        exit_section_(b, l, m, AND_EXPR, r, true, null);
      }
      else if (g < 2 && rel_op(b, l + 1)) {
        r = Expression(b, l, 2);
        exit_section_(b, l, m, CONDITIONAL_EXPR, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  // (LiteralExpr|ParenthesesExpr)? FieldChain
  public static boolean FieldChainExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldChainExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<field chain expr>");
    r = FieldChainExpr_0(b, l + 1);
    r = r && FieldChain(b, l + 1);
    exit_section_(b, l, m, FIELD_CHAIN_EXPR, r, false, null);
    return r;
  }

  // (LiteralExpr|ParenthesesExpr)?
  private static boolean FieldChainExpr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldChainExpr_0")) return false;
    FieldChainExpr_0_0(b, l + 1);
    return true;
  }

  // LiteralExpr|ParenthesesExpr
  private static boolean FieldChainExpr_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "FieldChainExpr_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = LiteralExpr(b, l + 1);
    if (!r) r = ParenthesesExpr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // Literal
  public static boolean LiteralExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "LiteralExpr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<literal expr>");
    r = Literal(b, l + 1);
    exit_section_(b, l, m, LITERAL_EXPR, r, false, null);
    return r;
  }

  // '(' Pipeline ')'
  public static boolean ParenthesesExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ParenthesesExpr")) return false;
    if (!nextTokenIsFast(b, LPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeTokenSmart(b, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, Pipeline(b, l + 1));
    r = p && consumeToken(b, RPAREN) && r;
    exit_section_(b, l, m, PARENTHESES_EXPR, r, p, null);
    return r || p;
  }

}
