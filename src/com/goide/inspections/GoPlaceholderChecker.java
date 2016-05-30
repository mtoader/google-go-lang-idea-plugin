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
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Couple;
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

  private enum PrintVerb {
    Percent(Couple.of('%', Couple.of("", 0))),
    b(Couple.of('b', Couple.of(" -+.0", argInt.getValue() | argFloat.getValue() | argComplex.getValue()))),
    c(Couple.of('c', Couple.of("-", argRune.getValue() | argInt.getValue()))),
    d(Couple.of('d', Couple.of(" -+.0", argInt.getValue()))),
    e(Couple.of('e', Couple.of(" -+.0", argFloat.getValue() | argComplex.getValue()))),
    E(Couple.of('E', Couple.of(" -+.0", argFloat.getValue() | argComplex.getValue()))),
    f(Couple.of('f', Couple.of(" -+.0", argFloat.getValue() | argComplex.getValue()))),
    F(Couple.of('F', Couple.of(" -+.0", argFloat.getValue() | argComplex.getValue()))),
    g(Couple.of('g', Couple.of(" -+.0", argFloat.getValue() | argComplex.getValue()))),
    G(Couple.of('G', Couple.of(" -+.0", argFloat.getValue() | argComplex.getValue()))),
    o(Couple.of('o', Couple.of(" -+.0#", argInt.getValue()))),
    p(Couple.of('p', Couple.of("-#", argPointer.getValue()))),
    q(Couple.of('q', Couple.of(" -+.0#", argRune.getValue() | argInt.getValue() | argString.getValue()))),
    s(Couple.of('s', Couple.of(" -+.0", argString.getValue()))),
    t(Couple.of('t', Couple.of("-", argBool.getValue()))),
    T(Couple.of('T', Couple.of("-", anyType.getValue()))),
    U(Couple.of('U', Couple.of("-#", argRune.getValue() | argInt.getValue()))),
    V(Couple.of('v', Couple.of(" -+.0#", anyType.getValue()))),
    x(Couple.of('x', Couple.of(" -+.0#", argRune.getValue() | argInt.getValue() | argString.getValue()))),
    X(Couple.of('X', Couple.of(" -+.0#", argRune.getValue() | argInt.getValue() | argString.getValue())));

    private Couple myVal;

    PrintVerb(Couple val) {
      myVal = val;
    }

    public Couple getValue() {
      return myVal;
    }

    public char getVerb() {
      return (char)myVal.getFirst();
    }

    @NotNull
    public String getFlags() {
      return (String)((Couple)myVal.getSecond()).getFirst();
    }

    public int getMask() {
      return (int)((Couple)myVal.getSecond()).getSecond();
    }

    @Nullable
    public static PrintVerb getByVerb(char verb) {
      for (PrintVerb v : values()) {
        if (verb == v.getVerb()) return v;
      }
      return null;
    }
  }

  private ProblemsHolder myProblemsHolder;
  private GoCallExpr myCallExpr;
  private GoFunctionOrMethodDeclaration myDeclaration;

  public GoPlaceholderChecker(ProblemsHolder problemsHolder, GoCallExpr callExpr, GoFunctionOrMethodDeclaration declaration) {
    myProblemsHolder = problemsHolder;
    myCallExpr = callExpr;
    myDeclaration = declaration;
  }

  public void checkPrint() {
    // TODO florin: Implement the print function checking
    // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L587
  }

  public void checkPrintf() {
    int placeholderPosition = getPlaceholderPosition(myDeclaration);
    List<GoExpression> arguments = myCallExpr.getArgumentList().getExpressionList();
    if (placeholderPosition < 0 || arguments.size() <= placeholderPosition) return;

    GoExpression placeholder = arguments.get(placeholderPosition);
    if (!GoTypeUtil.isString(placeholder.getGoType(null))) {
      String message = "Value used for formatting text does not appear to be a string";
      myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return;
    }

    String placeholderText = resolve(placeholder);
    if (placeholderText == null) return;

    // We always receive the text with double quotes at the beginning and at the end so remove them
    placeholderText = placeholderText.substring(1, placeholderText.length() - 1);

    if (!hasPlaceholder(placeholderText) && arguments.size() > placeholderPosition) {
      String message = "Value used for formatting text does not appear to contain a placeholder";
      myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return;
    }

    int firstArg = placeholderPosition + 1;
    int argNum = firstArg;
    int maxArgsNum = argNum;
    int w;
    for (int i = 0; i < placeholderText.length(); i += w) {
      w = 1;
      if (placeholderText.charAt(i) == '%') {
        FormatState state = parsePrintfVerb(placeholder, placeholderText.substring(i), firstArg, argNum);
        if (state == null) return;
        w = state.format.length();

        // One error per format is enough
        if (!okPrintfArg(placeholder, arguments, state)) return;

        if (!state.argNums.isEmpty()) {
          int maxArgNum = Collections.max(state.argNums) + 1;
          int lastArgNum = state.argNums.get(state.argNums.size() - 1) + 1;
          if (!state.indexed) {
            if (argNum < maxArgNum) {
              argNum = maxArgNum;
            }
          }
          else {
            argNum = lastArgNum;
          }
          if (maxArgsNum < maxArgNum) maxArgsNum = maxArgNum;
        }
      }
    }

    if (hasVariadic(myCallExpr.getArgumentList()) && maxArgsNum >= arguments.size() - 1) {
      return;
    }

    if (maxArgsNum != arguments.size()) {
      int expect = maxArgsNum - firstArg;
      int numArgs = arguments.size() - firstArg;
      String message = String.format("Got %d placeholder(s) for %d arguments(s)", expect, numArgs);
      myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
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

  private FormatState parsePrintfVerb(@NotNull GoExpression placeholder, @NotNull String format, int firstArg, int argNum) {
    FormatState state = new FormatState(format, argNum, firstArg);

    // There may be flags
    parseFlags(state);

    // There may be an index
    if (!parseIndex(myProblemsHolder, placeholder, state)) return null;

    // There may be a width
    if (!parseNum(state)) return null;

    // There may be a precision
    if (!parsePrecision(myProblemsHolder, placeholder, state)) return null;

    // Now a verb, possibly prefixed by an index (which we may already have)
    if (!state.indexPending && !parseIndex(myProblemsHolder, placeholder, state)) return null;

    if (state.nBytes == format.length()) {
      String message = "Missing verb at end of format string in <code>#ref</code> call";
      myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return null;
    }

    state.verb = state.format.charAt(state.nBytes);
    state.nBytes++;
    if (state.verb != '%') state.argNums.add(state.argNum);
    state.format = state.format.substring(0, state.nBytes);

    return state;
  }

  private boolean okPrintfArg(@NotNull GoExpression placeholder, @NotNull List<GoExpression> arguments, @NotNull FormatState state) {
    PrintVerb v = PrintVerb.getByVerb(state.verb);
    if (v == null) {
      String message = String.format("Unrecognized printf verb %s in <code>#ref</code> call", state.verb);
      myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    for (int i = 0; i < state.flags.length(); i++) {
      char flag = state.flags.charAt(i);
      if (v.getFlags().indexOf(flag) == -1) {
        String message = String.format("Unrecognized <code>#ref</code> flag for verb %s: %s call", state.verb, flag);
        myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
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
      myProblemsHolder.registerProblem(expression, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    // TODO florin: add argument matching when type comparison can be done
    // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L502

    return true;
  }

  private boolean argCanBeChecked(@NotNull GoExpression placeholder, int callArgs, int formatArg, @NotNull FormatState state) {
    int argNum = state.argNums.get(formatArg);
    if (argNum < 0) return false;

    if (argNum == 0) {
      String message = String.format("Index value [0] for <code>#ref</code>(\"%s\"); indexes start at 1", state.format);
      myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    if (argNum < callArgs - 1) return true;
    if (hasVariadic(myCallExpr.getArgumentList())) return false;
    if (argNum < callArgs) return true;

    // There are bad indexes in the format or there are fewer arguments than the format needs
    // This is the argument number relative to the format: Printf("%s", "hi") will give 1 for the "hi"
    int arg = argNum - state.firstArg + 1;
    String message = String.format("Got %d placeholder(s) for %d arguments(s)", arg, callArgs - state.firstArg);
    myProblemsHolder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

    return false;
  }

  private static boolean isFunctionValue(GoExpression expression) {
    return expression instanceof GoCallExpr;
  }

  private static boolean hasPlaceholder(@NotNull String formatString) {
    return StringUtil.contains(formatString, "%");
  }

  private static boolean hasVariadic(@NotNull GoArgumentList argumentList) {
    return argumentList.getTripleDot() != null;
  }

  private static class FormatState {
    public char verb;                  // the format verb: 'd' for "%d"
    public String format;              // the full format directive from % through verb, "%.3d".
    @NotNull public String flags = ""; // the list of # + etc.
    public boolean indexed;            // whether an indexing expression appears: %[1]d.
    public int firstArg;               // Index of first argument after the format in the Printf call.
    public final List<Integer> argNums = new ArrayList<Integer>(); // the successive argument numbers that are consumed, adjusted to refer to actual arg in call

    // Used only during parse.
    public int argNum;            // Which argument we're expecting to format now.
    public boolean indexPending;  // Whether we have an indexed argument that has not resolved.
    public int nBytes = 1;        // number of bytes of the format string consumed.

    public FormatState(String format, int argNum, int firstArg) {
      this.format = format;
      this.argNum = argNum;
      this.firstArg = firstArg;
    }
  }

  private static void parseFlags(@NotNull FormatState state) {
    String knownFlags = "#0+- ";
    StringBuilder flags = new StringBuilder(state.flags);
    while (state.nBytes < state.format.length()) {
      if (knownFlags.indexOf(state.format.charAt(state.nBytes)) != -1) {
        flags.append(state.format.charAt(state.nBytes));
      }
      else {
        state.flags = flags.toString();
        return;
      }
      state.nBytes++;
    }
    state.flags = flags.toString();
  }

  private static void scanNum(@NotNull FormatState state) {
    while (state.nBytes < state.format.length()) {
      if (!StringUtil.isDecimalDigit(state.format.charAt(state.nBytes))) {
        return;
      }
      state.nBytes++;
    }
  }

  private static boolean parseIndex(@NotNull ProblemsHolder holder, @NotNull GoExpression placeholder, @NotNull FormatState state) {
    if (state.nBytes == state.format.length() || state.format.charAt(state.nBytes) != '[') return true;

    state.indexed = true;
    state.nBytes++;
    int start = state.nBytes;
    scanNum(state);
    if (state.nBytes == state.format.length() || state.nBytes == start || state.format.charAt(state.nBytes) != ']') {
      String message = "Illegal syntax for <code>#ref</code> argument index";
      holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    int arg;
    try {
      arg = Integer.parseInt(state.format.substring(start, state.nBytes));
    }
    catch (NumberFormatException ignored) {
      String message = "illegal syntax for <code>#ref</code> argument index, expecting a number";
      holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    state.nBytes++;
    arg += state.firstArg - 1;
    state.argNum = arg;
    state.argNums.add(state.argNum);
    state.indexPending = true;

    return true;
  }

  private static boolean parseNum(@NotNull FormatState state) {
    if (state.nBytes < state.format.length() && state.format.charAt(state.nBytes) == '*') {
      if (state.indexPending) {
        state.indexPending = false;
      }
      state.nBytes++;
      state.argNums.add(state.argNum);
      state.argNum++;
    }
    else {
      scanNum(state);
    }

    return true;
  }

  private static boolean parsePrecision(@NotNull ProblemsHolder holder, @NotNull GoExpression placeholder, @NotNull FormatState state) {
    if (state.nBytes < state.format.length() && state.format.charAt(state.nBytes) == '.') {
      state.flags += '.';
      state.nBytes++;
      if (!parseIndex(holder, placeholder, state)) return false;
      if (!parseNum(state)) return false;
    }

    return true;
  }
}
