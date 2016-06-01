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

import com.goide.inspections.GoPlaceholderChecker.Placeholder;
import com.goide.inspections.GoPlaceholderChecker.PrintVerb;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class GoPlaceholderCountInspection extends GoInspectionBase {

  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitCallExpr(@NotNull GoCallExpr o) {
        PsiReference psiReference = o.getExpression().getReference();
        PsiElement resolved = psiReference != null ? psiReference.resolve() : null;
        if (!(resolved instanceof GoFunctionOrMethodDeclaration)) return;

        String functionName = StringUtil.toLowerCase(((GoFunctionOrMethodDeclaration)resolved).getName());
        if (functionName == null) return;

        if (GoPlaceholderChecker.isFormattingFunction(functionName)) {
          checkPrintf(holder, o, (GoFunctionOrMethodDeclaration)resolved);
        }
        else if (GoPlaceholderChecker.isPrintingFunction(functionName)) {
          checkPrint(holder, o, (GoFunctionOrMethodDeclaration)resolved);
        }
      }
    };
  }

  private static void checkPrint(
    @NotNull ProblemsHolder holder,
    @NotNull GoCallExpr callExpr,
    @NotNull GoFunctionOrMethodDeclaration declaration) {

    List<GoExpression> arguments = callExpr.getArgumentList().getExpressionList();
    if (arguments.isEmpty()) return;

    GoExpression firstArg = arguments.get(0);
    if (GoTypeUtil.isString(firstArg.getGoType(null))) {
      String firstArgText = GoPlaceholderChecker.resolve(firstArg);
      if (firstArgText != null) {
        if (GoPlaceholderChecker.hasPlaceholder(firstArgText)) {
          String message = "Possible formatting directive in <code>#ref</code>";
          holder.registerProblem(firstArg, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          return;
        }
      }
    }

    // TODO florin: Check first argument for os.Std* output
    // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L617

    String declarationName = declaration.getName();
    boolean isLn = declarationName != null && declarationName.endsWith("ln");
    for (GoExpression argument : arguments) {
      GoType goType = argument.getGoType(null);
      if (isLn && GoTypeUtil.isString(goType)) {
        String argText = GoPlaceholderChecker.resolve(argument);
        if (argText != null && argText.endsWith("\\n")) {
          String message = "Function already ends with new line";
          holder.registerProblem(argument, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
      }
      else if (GoTypeUtil.isFunction(goType)) {
        String message = "Argument <code>#ref</code> is not a function call";
        if (argument instanceof GoCallExpr) {
          message = "Final return type of <code>#ref</code> is a function not a function call";
        }
        holder.registerProblem(argument, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      }
    }
  }

  private static void checkPrintf(
    @NotNull ProblemsHolder holder,
    @NotNull GoCallExpr callExpr,
    @NotNull GoFunctionOrMethodDeclaration declaration) {

    int placeholderPosition = GoPlaceholderChecker.getPlaceholderPosition(declaration);
    List<GoExpression> arguments = callExpr.getArgumentList().getExpressionList();
    if (arguments.isEmpty()) return;
    int callArgsNum = arguments.size();
    if (placeholderPosition < 0 || callArgsNum <= placeholderPosition) return;

    GoExpression placeholder = arguments.get(placeholderPosition);
    if (!GoTypeUtil.isString(placeholder.getGoType(null))) {
      String message = "Value used for formatting text does not appear to be a string";
      holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return;
    }

    String placeholderText = GoPlaceholderChecker.resolve(placeholder);
    if (placeholderText == null) return;

    if (!GoPlaceholderChecker.hasPlaceholder(placeholderText) && callArgsNum > placeholderPosition) {
      String message = "Value used for formatting text does not appear to contain a placeholder";
      holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return;
    }

    callArgsNum--;
    List<Placeholder> placeholders = GoPlaceholderChecker.parsePrintf(placeholderText);
    for (Placeholder fmtPlaceholder : placeholders) {
      if (!checkPrintfArgument(holder, placeholder, callExpr, arguments, callArgsNum, placeholderPosition, fmtPlaceholder)) return;
    }

    if (hasErrors(holder, placeholder, placeholders)) return;
    // TODO florin add a min argument to see if we are skipping any argument from the formatting string
    int maxArgsNum = computeMaxArgsNum(placeholders, placeholderPosition);

    if (GoPsiImplUtil.hasVariadic(callExpr.getArgumentList()) && maxArgsNum >= callArgsNum) {
      return;
    }

    if (maxArgsNum != callArgsNum) {
      int expect = maxArgsNum - placeholderPosition;
      int numArgs = callArgsNum - placeholderPosition;
      String message = String.format("Got %d placeholder(s) for %d arguments(s)", expect, numArgs);
      holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }

    // TODO florin: check if all arguments are strings and offer to replace with Println and string concat
  }

  private static int computeMaxArgsNum(@NotNull List<Placeholder> placeholders, int firstArg) {
    int maxArgsNum = 0;
    for (Placeholder placeholder : placeholders) {
      List<Integer> arguments = placeholder.getArguments();
      if (arguments.isEmpty()) continue;
      int max = Collections.max(arguments);
      if (maxArgsNum < max) {
        maxArgsNum = max;
      }
    }

    return maxArgsNum + firstArg;
  }

  private static boolean hasErrors(
    @NotNull ProblemsHolder holder,
    @NotNull GoExpression placeholder,
    @NotNull List<Placeholder> placeholders) {

    for (Placeholder p : placeholders) {
      Placeholder.State state = p.getState();
      if (state == Placeholder.State.MISSING_VERB_AT_END) {
        String message = "Missing verb at end of format string in <code>#ref</code> call";
        holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return true;
      }

      if (state == Placeholder.State.ARGUMENT_INDEX_NOT_NUMERIC) {
        String message = "Illegal syntax for <code>#ref</code> argument index, expecting a number";
        holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return true;
      }
    }

    return false;
  }

  private static boolean checkPrintfArgument(
    @NotNull ProblemsHolder holder,
    @NotNull GoExpression placeholder,
    @NotNull GoCallExpr callExpr,
    @NotNull List<GoExpression> arguments,
    int callArgsNum,
    int firstArg,
    @NotNull Placeholder fmtPlaceholder) {

    PrintVerb v = fmtPlaceholder.getVerb();
    if (v == null) {
      String message = "Unrecognized formatting verb in <code>#ref</code> call";
      holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    String flags = fmtPlaceholder.getFlags();
    for (int i = 0; i < flags.length(); i++) {
      char flag = flags.charAt(i);
      if (v.getFlags().indexOf(flag) == -1) {
        String message = String.format("Unrecognized <code>#ref</code> flag for verb %s: %s call", v.getVerb(), flag);
        holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        return false;
      }
    }

    List<Integer> args = fmtPlaceholder.getArguments();
    // Verb is good. If len(state.argNums)>trueArgs, we have something like %.*s and all
    // but the final arg must be an integer.
    int trueArgs = v == PrintVerb.Percent ? 0 : 1;
    int nargs = args.size();
    for (int i = 0; i < nargs - trueArgs; i++) {
      if (!checkArgumentIndex(holder, placeholder, callExpr, fmtPlaceholder, callArgsNum)) return false;
      // TODO florin: add argument matching when type comparison can be done
      // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L484
    }

    if (v == PrintVerb.Percent) return true;

    if (!checkArgumentIndex(holder, placeholder, callExpr, fmtPlaceholder, callArgsNum)) return false;

    int argNum = args.get(args.size() - 1);
    GoExpression expression = arguments.get(argNum + firstArg - 1);
    if (GoTypeUtil.isFunction(expression.getGoType(null)) && v != PrintVerb.p && v != PrintVerb.T) {
      String message = "Argument <code>#ref</code> is not a function call";
      if (expression instanceof GoCallExpr) {
        message = "Final return type of <code>#ref</code> is a function not a function call";
      }
      holder.registerProblem(expression, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    // TODO florin: add argument matching when type comparison can be done
    // Ref code: https://github.com/golang/go/blob/79f7ccf2c3931745aeb97c5c985b6ac7b44befb4/src/cmd/vet/print.go#L502

    return true;
  }

  private static boolean checkArgumentIndex(
    @NotNull ProblemsHolder holder,
    @NotNull GoExpression placeholder,
    @NotNull GoCallExpr callExpr,
    @NotNull Placeholder fmtPlaceholder,
    int callArgsNum) {

    int argNum = fmtPlaceholder.getPosition();
    if (argNum < 0) return false;

    if (argNum == 0) {
      holder.registerProblem(placeholder, "Index value [0] is not allowed", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }

    if (argNum <= callArgsNum) return true;
    if (GoPsiImplUtil.hasVariadic(callExpr.getArgumentList())) return false;
    if (argNum <= callArgsNum) return true;

    // There are bad indexes in the format or there are fewer arguments than the format needs
    // This is the argument number relative to the format: Printf("%s", "hi") will give 1 for the "hi"
    int arg = fmtPlaceholder.getPosition();
    String message = String.format("Got %d placeholder(s) for %d arguments(s)", arg, callArgsNum);
    holder.registerProblem(placeholder, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    return false;
  }
}
