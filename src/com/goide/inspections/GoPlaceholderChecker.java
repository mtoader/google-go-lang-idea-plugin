/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goide.inspections;

import com.goide.psi.*;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.goide.GoConstants.TESTING_PATH;
import static com.goide.inspections.GoPlaceholderChecker.PrintfArgType.*;

public class GoPlaceholderChecker {

  // This holds the name of the known formatting functions and position of the string to be formatted
  public static final Map<String, Integer> FORMATTING_FUNCTIONS = ContainerUtil.newHashMap(
    Pair.pair("errorf", 0),
    Pair.pair("fatalf", 0),
    Pair.pair("fprintf", 1),
    Pair.pair("fscanf", 1),
    Pair.pair("logf", 0),
    Pair.pair("panicf", 0),
    Pair.pair("printf", 0),
    Pair.pair("scanf", 0),
    Pair.pair("skipf", 0),
    Pair.pair("sprintf", 0),
    Pair.pair("sscanf", 1));

  public static final List<String> PRINT_FUNCTIONS = ContainerUtil.newArrayList(
    "error",
    "error",
    "fatal",
    "fprint",
    "fprintln",
    "log",
    "panic",
    "panicln",
    "print",
    "println",
    "sprint",
    "sprintln"
  );

  private ProblemsHolder myHolder;
  private GoCallExpr myCallExpr;
  private GoFunctionOrMethodDeclaration myDeclaration;

  public GoPlaceholderChecker(ProblemsHolder holder, GoCallExpr callExpr, GoFunctionOrMethodDeclaration declaration) {
    myHolder = holder;
    myCallExpr = callExpr;
    myDeclaration = declaration;
  }

  // todo: implement ConstEvaluator
  @Nullable
  private static String getValue(@Nullable GoExpression expression) {
    if (expression instanceof GoStringLiteral) {
      return expression.getText();
    }
    if (expression instanceof GoAddExpr) {
      String sum = getValue((GoAddExpr)expression);
      return sum.isEmpty() ? null : sum;
    }

    return null;
  }

  // todo: implement ConstEvaluator
  @NotNull
  private static String getValue(@Nullable GoAddExpr expression) {
    if (expression == null) return "";
    StringBuilder result = new StringBuilder();
    for (GoExpression expr : expression.getExpressionList()) {
      if (expr instanceof GoStringLiteral) {
        result.append(expr.getText());
      }
      else if (expr instanceof GoAddExpr) {
        result.append(getValue(expr));
      }
    }

    return result.toString();
  }

  private static int getPlaceholderPosition(@NotNull GoFunctionOrMethodDeclaration function) {
    Integer position = FORMATTING_FUNCTIONS.get(StringUtil.toLowerCase(function.getName()));
    if (position != null) {
      String importPath = function.getContainingFile().getImportPath(false);
      if ("fmt".equals(importPath) || "log".equals(importPath) || TESTING_PATH.equals(importPath)) {
        return position;
      }
    }
    return -1;
  }

  @Nullable
  private static String resolve(@NotNull GoExpression argument) {
    if (argument instanceof GoStringLiteral) return argument.getText();

    PsiReference reference = argument.getReference();
    PsiElement resolved = reference != null ? reference.resolve() : null;

    if (resolved instanceof GoVarDefinition) {
      return getValue(((GoVarDefinition)resolved).getValue());
    }
    if (resolved instanceof GoConstDefinition) {
      return getValue(((GoConstDefinition)resolved).getValue());
    }

    return null;
  }

  public void checkPrint() {
    // TODO florin: Implement the print function checking
    // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L587
  }

  private class FormatState {
    public char verb;             // the format verb: 'd' for "%d"
    public String format;         // the full format directive from % through verb, "%.3d".
    public String flags;          // the list of # + etc.
    public List<Integer> argNums; // the successive argument numbers that are consumed, adjusted to refer to actual arg in call
    public boolean indexed;       // whether an indexing expression appears: %[1]d.
    public int firstArg;          // Index of first argument after the format in the Printf call.

    // Used only during parse.
    public int argNum;            // Which argument we're expecting to format now.
    public boolean indexPending;  // Whether we have an indexed argument that has not resolved.
    public int nBytes;            // number of bytes of the format string consumed.

    GoExpression myPlaceholder;

    public FormatState(GoExpression placeholder) {
      myPlaceholder = placeholder;
    }

    public void parseFlags() {
      String knownFlags = "#0+- ";
      StringBuilder flags = new StringBuilder(this.flags);
      for (; nBytes < format.length(); nBytes++) {
        if (knownFlags.indexOf(format.charAt(nBytes)) != -1) {
          flags.append(format.charAt(nBytes));
        }
        else {
          this.flags = flags.toString();
          return;
        }
      }
      this.flags = flags.toString();
    }

    private void scanNum() {
      for (; nBytes < format.length(); nBytes++) {
        if (format.charAt(nBytes) < '0' || '9' < format.charAt(nBytes)) {
          return;
        }
      }
    }

    public boolean parseIndex() {
      if (nBytes == format.length() || format.charAt(nBytes) != '[') return true;

      indexed = true;
      nBytes++;
      int start = nBytes;
      scanNum();
      if (nBytes == format.length() || nBytes == start || format.charAt(nBytes) != ']') {
        String message = "Illegal syntax for <code>#ref</code> argument index";
        myHolder.registerProblem(myPlaceholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return false;
      }

      int arg;
      try {
        arg = Integer.parseInt(format.substring(start, nBytes));
      }
      catch (NumberFormatException ignored) {
        String message = "illegal syntax for <code>#ref</code> argument index, ";
        myHolder.registerProblem(myPlaceholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return false;
      }

      nBytes++;
      arg += firstArg - 1;
      argNum = arg;
      indexPending = true;

      return true;
    }

    public boolean parseNum() {
      if (nBytes < format.length() && format.charAt(nBytes) == '*') {
        if (indexPending) {
          indexPending = false;
        }
        nBytes++;
        argNums.add(argNum);
        argNum++;
      }
      else {
        scanNum();
      }

      return true;
    }

    public boolean parsePrecision() {
      if (nBytes < format.length() && format.charAt(nBytes) == '.') {
        flags += '.';
        nBytes++;
        if (!parseIndex()) return false;
        if (!parseNum()) return false;
      }

      return true;
    }
  }

  public void checkPrintf() {
    int placeholderPosition = getPlaceholderPosition(myDeclaration);
    List<GoExpression> arguments = myCallExpr.getArgumentList().getExpressionList();
    if (placeholderPosition < 0 || arguments.size() <= placeholderPosition) return;

    GoExpression placeholder = arguments.get(placeholderPosition);
    if (!isStringPlaceholder(placeholder)) {
      String message = "Value used for formatting text does not appear to be a string";
      myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return;
    }

    String placeholderText = resolve(placeholder);
    if (placeholderText == null) return;

    // We always receive the text with double quotes at the beginning and at the end so remove them
    placeholderText = placeholderText.substring(1, placeholderText.length() - 1);

    if (!hasPlaceholder(placeholderText) && arguments.size() > placeholderPosition) {
      String message = "Value used for formatting text does not appear to contain a placeholder";
      myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return;
    }

    int firstArg = placeholderPosition + 1;
    int argNum = firstArg;
    boolean indexed = false;
    int w;
    for (int i = 0; i < placeholderText.length(); i += w) {
      w = 1;
      if (placeholderText.charAt(i) == '%') {
        FormatState state = parsePrintfVerb(placeholder, placeholderText.substring(i), firstArg, argNum);
        if (state == null) return;
        w = state.format.length();
        if (state.indexed) indexed = true;

        // One error per format is enough
        if (!okPrintfArg(placeholder, arguments, state)) return;

        if (!state.argNums.isEmpty()) {
          Integer maxArg = Collections.max(state.argNums);
          if (maxArg != null) argNum = maxArg + 1;
        }
      }
    }

    if (hasVariadic(myCallExpr.getArgumentList()) && argNum >= arguments.size() - 1) {
      return;
    }

    if (!indexed && argNum != arguments.size()) {
      int expect = argNum - firstArg;
      int numArgs = arguments.size() - firstArg;
      String message = String.format("Got %d placeholder(s) for %d arguments(s)", expect, numArgs);
      myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }

  // The types of expressions a printf verb accepts. It is a bitmask.
  protected enum PrintfArgType {
    argBool(1),
    argInt(2),
    argRune(3),
    argString(4),
    argFloat(5),
    argComplex(6),
    argPointer(7),
    anyType(-1);

    private int myVal;

    PrintfArgType(int val) {
      myVal = val;
    }

    public int getValue() {
      return myVal;
    }
  }


  private static class PrintVerb {
    public char verb;
    public String flags;
    public int type;

    public PrintVerb(char verb, String flags, int type) {
      this.verb = verb;
      this.flags = flags;
      this.type = type;
    }
  }

  @NotNull private static List<PrintVerb> printVerbs = ContainerUtil.newArrayList(
    new PrintVerb('%', "", 0),
    new PrintVerb('b', " -+.0", argInt.getValue() | argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('c', "-", argRune.getValue() | argInt.getValue()),
    new PrintVerb('d', " -+.0", argInt.getValue()),
    new PrintVerb('e', " -+.0", argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('E', " -+.0", argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('f', " -+.0", argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('F', " -+.0", argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('g', " -+.0", argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('G', " -+.0", argFloat.getValue() | argComplex.getValue()),
    new PrintVerb('o', " -+.0#", argInt.getValue()),
    new PrintVerb('p', "-#", argPointer.getValue()),
    new PrintVerb('q', " -+.0#", argRune.getValue() | argInt.getValue() | argString.getValue()),
    new PrintVerb('s', " -+.0", argString.getValue()),
    new PrintVerb('t', "-", argBool.getValue()),
    new PrintVerb('T', "-", anyType.getValue()),
    new PrintVerb('U', "-#", argRune.getValue() | argInt.getValue()),
    new PrintVerb('v', " -+.0#", anyType.getValue()),
    new PrintVerb('x', " -+.0#", argRune.getValue() | argInt.getValue() | argString.getValue()),
    new PrintVerb('X', " -+.0#", argRune.getValue() | argInt.getValue() | argString.getValue())
  );

  private boolean argCanBeChecked(@NotNull GoExpression placeholder, int callArgs, int formatArg, @NotNull FormatState state) {
    int argNum = state.argNums.get(formatArg);
    if (argNum < 0) return false;

    if (argNum == 0) {
      String message = String.format("Index value [0] for <code>#ref</code>(\"%s\"); indexes start at 1", state.format);
      myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    if (argNum < callArgs - 1) return true;
    if (hasVariadic(myCallExpr.getArgumentList())) return false;
    if (argNum < callArgs) return true;

    // There are bad indexes in the format or there are fewer arguments than the format needs
    // This is the argument number relative to the format: Printf("%s", "hi") will give 1 for the "hi"
    int arg = argNum - state.firstArg + 1;
    String message = String.format("Got %d placeholder(s) for %d arguments(s)", arg, callArgs - state.firstArg);
    myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

    return false;
  }

  private static boolean isFunctionValue(GoExpression expression) {
    return expression instanceof GoCallExpr;
  }

  private boolean okPrintfArg(@NotNull GoExpression placeholder, @NotNull List<GoExpression> arguments, @NotNull FormatState state) {
    PrintVerb v = null;
    boolean found = false;

    for (int i = 0; i < printVerbs.size(); i++) {
      v = printVerbs.get(i);
      if (v.verb == state.verb) {
        found = true;
        break;
      }
    }

    if (!found) {
      String message = String.format("Unrecognized printf verb %s in <code>#ref</code> call", state.verb);
      myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    for (int i = 0; i < state.flags.length(); i++) {
      char flag = state.flags.charAt(i);
      if (v.flags.indexOf(flag) == -1) {
        String message = String.format("Unrecognized <code>#ref</code> flag for verb %s: %s call", state.verb, flag);
        myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return false;
      }
    }

    // Verb is good. If len(state.argNums)>trueArgs, we have something like %.*s and all
    // but the final arg must be an integer.
    int trueArgs = state.verb == '%' ? 0 : 1;
    int nargs = state.argNums.size();
    for (int i = 0; i < nargs - trueArgs; i++) {
      if (!argCanBeChecked(placeholder, arguments.size(), i, state)) return false;
      // TODO florin: add argument matching when type comparison can be done
      // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L484
    }

    if (state.verb == '%') return true;

    if (!argCanBeChecked(placeholder, arguments.size(), state.argNums.size() - 1, state)) return false;

    int argNum = state.argNums.get(state.argNums.size() - 1);
    GoExpression expression = arguments.get(argNum);
    if (isFunctionValue(expression) && state.verb != 'p' && state.verb != 'T') {
      String message = "Argument <code>#ref</code> for <code>#loc</code> is a function value, not a function call";
      myHolder.registerProblem(expression, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    // TODO florin: add argument matching when type comparison can be done
    // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L502

    return true;
  }

  private FormatState parsePrintfVerb(@NotNull GoExpression placeholder, @NotNull String format, int firstArg, int argNum) {

    FormatState state = new FormatState(placeholder);
    state.format = format;
    state.flags = "";
    state.argNum = argNum;
    state.argNums = new ArrayList<Integer>();
    state.nBytes = 1;
    state.firstArg = firstArg;

    // There may be flags
    state.parseFlags();
    state.indexPending = false;

    // There may be an index
    if (!state.parseIndex()) return null;

    // There may be a width
    if (!state.parseNum()) return null;

    // There may be a precision
    if (!state.parsePrecision()) return null;

    // Now a verb, possibly prefixed by an index (which we may already have)
    if (!state.indexPending && !state.parseIndex()) return null;

    if (state.nBytes == format.length()) {
      String message = "Missing verb at end of format string in <code>#ref</code> call";
      myHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return null;
    }

    state.verb = state.format.charAt(state.nBytes);
    state.nBytes++;
    if (state.verb != '%') state.argNums.add(state.argNum);
    state.format = state.format.substring(0, state.nBytes);

    return state;
  }

  private static boolean isStringPlaceholder(@NotNull GoExpression argument) {
    GoType goType = argument.getGoType(null);
    GoTypeReferenceExpression typeReferenceExpression = goType != null ? goType.getTypeReferenceExpression() : null;
    return typeReferenceExpression != null && typeReferenceExpression.textMatches("string");
  }

  private static boolean hasPlaceholder(@NotNull String formatString) {
    return StringUtil.contains(formatString, "%");
  }

  private static boolean hasVariadic(@NotNull GoArgumentList argumentList) {
    // TODO Is there any better method to check if we have any unpacked variadic parameter?
    return argumentList.getTripleDot() != null;
  }
}
