/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Mihai Toader, Florin Patan
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

package com.goide.debugger.delve.dlv.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Class representing a single result from a Delve result record.
 */
public class DelveResult {
  /**
   * Name of the variable.
   */
  public String variable;

  /**
   * Value of the variable.
   */
  @NotNull public DelveValue value = new DelveValue();

  /**
   * Constructor.
   *
   * @param variable The name of the variable.
   */
  public DelveResult(String variable) {
    this.variable = variable;
  }

  /**
   * Converts the result to a string.
   *
   * @return A string containing the name of the variable and its value.
   */
  @NotNull
  public String toString() {
    return variable + ": " + value;
  }
}
