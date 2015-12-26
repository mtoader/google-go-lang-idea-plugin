// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.goide.gotemplate.psi.GoTemplateCompositeElementType;
import com.goide.gotemplate.psi.GoTemplateTokenType;
import com.goide.gotemplate.psi.impl.*;

public interface GoTemplateTypes {

  IElementType AND_EXPR = new GoTemplateCompositeElementType("AND_EXPR");
  IElementType BLOCK_STATEMENT = new GoTemplateCompositeElementType("BLOCK_STATEMENT");
  IElementType CONDITIONAL_EXPR = new GoTemplateCompositeElementType("CONDITIONAL_EXPR");
  IElementType ELSE_STATEMENT = new GoTemplateCompositeElementType("ELSE_STATEMENT");
  IElementType END_STATEMENT = new GoTemplateCompositeElementType("END_STATEMENT");
  IElementType EXPRESSION = new GoTemplateCompositeElementType("EXPRESSION");
  IElementType FIELD_CHAIN = new GoTemplateCompositeElementType("FIELD_CHAIN");
  IElementType FIELD_CHAIN_EXPR = new GoTemplateCompositeElementType("FIELD_CHAIN_EXPR");
  IElementType IDENT_LITERAL = new GoTemplateCompositeElementType("IDENT_LITERAL");
  IElementType IF_STATEMENT = new GoTemplateCompositeElementType("IF_STATEMENT");
  IElementType LITERAL = new GoTemplateCompositeElementType("LITERAL");
  IElementType LITERAL_EXPR = new GoTemplateCompositeElementType("LITERAL_EXPR");
  IElementType LOCAL_IDENT_LITERAL = new GoTemplateCompositeElementType("LOCAL_IDENT_LITERAL");
  IElementType NUMBER_LITERAL = new GoTemplateCompositeElementType("NUMBER_LITERAL");
  IElementType OR_EXPR = new GoTemplateCompositeElementType("OR_EXPR");
  IElementType PARENTHESES_EXPR = new GoTemplateCompositeElementType("PARENTHESES_EXPR");
  IElementType PIPE = new GoTemplateCompositeElementType("PIPE");
  IElementType PIPELINE = new GoTemplateCompositeElementType("PIPELINE");
  IElementType PIPELINE_STATEMENT = new GoTemplateCompositeElementType("PIPELINE_STATEMENT");
  IElementType RANGE_ASSIGN = new GoTemplateCompositeElementType("RANGE_ASSIGN");
  IElementType RANGE_STATEMENT = new GoTemplateCompositeElementType("RANGE_STATEMENT");
  IElementType STATEMENT_LIST = new GoTemplateCompositeElementType("STATEMENT_LIST");
  IElementType STRING_LITERAL = new GoTemplateCompositeElementType("STRING_LITERAL");
  IElementType TEMPLATE_STATEMENT = new GoTemplateCompositeElementType("TEMPLATE_STATEMENT");
  IElementType WITH_STATEMENT = new GoTemplateCompositeElementType("WITH_STATEMENT");

  IElementType ASSIGN = new GoTemplateTokenType("=");
  IElementType BIT_OR = new GoTemplateTokenType("|");
  IElementType BLOCK = new GoTemplateTokenType("block");
  IElementType COMMA = new GoTemplateTokenType(",");
  IElementType COMMENT = new GoTemplateTokenType("");
  IElementType COND_AND = new GoTemplateTokenType("&&");
  IElementType COND_OR = new GoTemplateTokenType("||");
  IElementType DOT = new GoTemplateTokenType(".");
  IElementType ELSE = new GoTemplateTokenType("else");
  IElementType END = new GoTemplateTokenType("end");
  IElementType EQ = new GoTemplateTokenType("==");
  IElementType GREATER = new GoTemplateTokenType(">");
  IElementType GREATER_OR_EQUAL = new GoTemplateTokenType(">=");
  IElementType IDENT = new GoTemplateTokenType("IDENT");
  IElementType IF = new GoTemplateTokenType("if");
  IElementType LBRACE = new GoTemplateTokenType("{");
  IElementType LDOUBLE_BRACE = new GoTemplateTokenType("{{");
  IElementType LESS = new GoTemplateTokenType("<");
  IElementType LESS_OR_EQUAL = new GoTemplateTokenType("<=");
  IElementType LPAREN = new GoTemplateTokenType("(");
  IElementType NOT = new GoTemplateTokenType("!");
  IElementType NOT_EQ = new GoTemplateTokenType("!=");
  IElementType NUMBER = new GoTemplateTokenType("NUMBER");
  IElementType RANGE = new GoTemplateTokenType("range");
  IElementType RAW_STRING = new GoTemplateTokenType("RAW_STRING");
  IElementType RBRACE = new GoTemplateTokenType("}");
  IElementType RDOUBLE_BRACE = new GoTemplateTokenType("}}");
  IElementType RPAREN = new GoTemplateTokenType(")");
  IElementType STRING = new GoTemplateTokenType("STRING");
  IElementType TEMPLATE = new GoTemplateTokenType("template");
  IElementType TEXT = new GoTemplateTokenType("TEXT");
  IElementType VAR_ASSIGN = new GoTemplateTokenType(":=");
  IElementType VAR_IDENT = new GoTemplateTokenType("VAR_IDENT");
  IElementType WITH = new GoTemplateTokenType("with");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == AND_EXPR) {
        return new GoTemplateAndExprImpl(node);
      }
      else if (type == BLOCK_STATEMENT) {
        return new GoTemplateBlockStatementImpl(node);
      }
      else if (type == CONDITIONAL_EXPR) {
        return new GoTemplateConditionalExprImpl(node);
      }
      else if (type == ELSE_STATEMENT) {
        return new GoTemplateElseStatementImpl(node);
      }
      else if (type == END_STATEMENT) {
        return new GoTemplateEndStatementImpl(node);
      }
      else if (type == EXPRESSION) {
        return new GoTemplateExpressionImpl(node);
      }
      else if (type == FIELD_CHAIN) {
        return new GoTemplateFieldChainImpl(node);
      }
      else if (type == FIELD_CHAIN_EXPR) {
        return new GoTemplateFieldChainExprImpl(node);
      }
      else if (type == IDENT_LITERAL) {
        return new GoTemplateIdentLiteralImpl(node);
      }
      else if (type == IF_STATEMENT) {
        return new GoTemplateIfStatementImpl(node);
      }
      else if (type == LITERAL) {
        return new GoTemplateLiteralImpl(node);
      }
      else if (type == LITERAL_EXPR) {
        return new GoTemplateLiteralExprImpl(node);
      }
      else if (type == LOCAL_IDENT_LITERAL) {
        return new GoTemplateLocalIdentLiteralImpl(node);
      }
      else if (type == NUMBER_LITERAL) {
        return new GoTemplateNumberLiteralImpl(node);
      }
      else if (type == OR_EXPR) {
        return new GoTemplateOrExprImpl(node);
      }
      else if (type == PARENTHESES_EXPR) {
        return new GoTemplateParenthesesExprImpl(node);
      }
      else if (type == PIPE) {
        return new GoTemplatePipeImpl(node);
      }
      else if (type == PIPELINE) {
        return new GoTemplatePipelineImpl(node);
      }
      else if (type == PIPELINE_STATEMENT) {
        return new GoTemplatePipelineStatementImpl(node);
      }
      else if (type == RANGE_ASSIGN) {
        return new GoTemplateRangeAssignImpl(node);
      }
      else if (type == RANGE_STATEMENT) {
        return new GoTemplateRangeStatementImpl(node);
      }
      else if (type == STATEMENT_LIST) {
        return new GoTemplateStatementListImpl(node);
      }
      else if (type == STRING_LITERAL) {
        return new GoTemplateStringLiteralImpl(node);
      }
      else if (type == TEMPLATE_STATEMENT) {
        return new GoTemplateTemplateStatementImpl(node);
      }
      else if (type == WITH_STATEMENT) {
        return new GoTemplateWithStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
