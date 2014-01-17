/*
Copyright 2013 Florin Patan. All rights reserved.
Use of this source code is governed by a MIT-style
license that can be found in the LICENSE file.
*/
package uk.co.cwspencer.ideagdb.debug.go;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by Florin Patan <florinpatan@gmail.com>
 * <p/>
 * 1/14/14
 */
public class GoGdbUtil {
    // TODO properly add only types that work here
    private static final Map<String, Boolean> editingSupport = ImmutableMap.<String, Boolean>builder()
            .put("string", false)
            .build();

    public static String getGoObjectType(String originalType) {
        if (originalType.contains("struct string")) {
            return originalType.replace("struct ", "");
        }

        return originalType;
    }

    public static Boolean supportsEditing(String varType) {
        String goType = getGoObjectType(varType);

        if (!editingSupport.containsKey(goType)) {
            return true;
        }

        return editingSupport.get(goType);
    }
}
