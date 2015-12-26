package com.goide.gotemplate.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;

import java.util.*;
import static com.goide.gotemplate.GoTemplateTypes.*;
import static com.intellij.psi.TokenType.BAD_CHARACTER;

%%

%{
  private int textBegin = 0;
  public _GoTemplateLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _GoTemplateLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+

LETTER = [:letter:] | "_"
DIGIT =  [:digit:]

HEX_DIGIT = [0-9A-Fa-f]
INT_DIGIT = [0-9]
OCT_DIGIT = [0-7]

NUM_INT = "0" | ([1-9] {INT_DIGIT}*)
NUM_HEX = ("0x" | "0X") {HEX_DIGIT}+
NUM_OCT = "0" {OCT_DIGIT}+

FLOAT_EXPONENT = [eE] [+-]? {DIGIT}+
NUM_FLOAT = ( ( ({DIGIT}+ "." {DIGIT}*) | ({DIGIT}* "." {DIGIT}+) ) {FLOAT_EXPONENT}?) | ({DIGIT}+ {FLOAT_EXPONENT})

IDENT = {LETTER} ({LETTER} | {DIGIT} )*

STR =      "\""
ESCAPES = [abfnrtv]
ANY_CHAR = (.|[\n])
TEXT = [^{]*

%state ST_ACTION
%%

<YYINITIAL> {
         {TEXT}                          { return TEXT; }
        \{\{\*(.)*\*\}\}                 { return COMMENT; }
        "{{"                  { yybegin(ST_ACTION);return LDOUBLE_BRACE; }
        "{"                              { return TEXT; }
}


<ST_ACTION> {
  {STR} ( [^\"\\\n\r] | "\\" ("\\" | {STR} | {ESCAPES} | [0-8xuU] ) )* {STR}? { return STRING; }
  "`" [^`]* "`"?                                                              { return RAW_STRING; }
  {WHITE_SPACE}         { return com.intellij.psi.TokenType.WHITE_SPACE; }

  "{{"                  { return LDOUBLE_BRACE; }
  "}}"                  { yybegin(YYINITIAL);return RDOUBLE_BRACE; }
  "("                   { return LPAREN; }
  ")"                   { return RPAREN; }
  "=="                  { return EQ; }
  "="                   { return ASSIGN; }
  "!="                  { return NOT_EQ; }
  "!"                   { return NOT; }
  "||"                  { return COND_OR; }
  "&&"                  { return COND_AND; }
  "|"                   { return BIT_OR; }
  "<="                  { return LESS_OR_EQUAL; }
  "<"                   { return LESS; }
  ">="                  { return GREATER_OR_EQUAL; }
  ">"                   { return GREATER; }
  ":="                  { return VAR_ASSIGN; }
  "."                   { return DOT; }
  ","                   { return COMMA; }

  "if"                  { return IF; }
  "end"                 { return END; }
  "else"                { return ELSE; }
  "block"               { return BLOCK; }
  "template"            { return TEMPLATE; }
  "range"               { return RANGE; }
  "with"                { return WITH; }

  "$"{IDENT}            { return VAR_IDENT; }
  "$"                   { return VAR_IDENT; }
  {IDENT}               { return IDENT; }


  {NUM_FLOAT}"i"                           {  return NUMBER; }
  {NUM_FLOAT}                              {  return NUMBER; }
  {DIGIT}+"i"                              {  return NUMBER; }
  {NUM_OCT}                                {  return NUMBER; }
  {NUM_HEX}                                {  return NUMBER; }
  {NUM_INT}                                {  return NUMBER; }
}

<YYINITIAL,ST_ACTION> {
   {ANY_CHAR} { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
