/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.idea;

import com.intellij.psi.*;
import org.apache.ignite.idea.util.IgniteUtils;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Defines the insertion point for new methods.
 */
public class IgniteMethodInsertionPointSelector {
    /** Method position comparator. */
    private final Comparator<PsiMethod> COMP = new Comparator<PsiMethod>() {
        @Override public int compare(PsiMethod o1, PsiMethod o2) {
            return Integer.valueOf(o1.getStartOffsetInParent()).compareTo(o2.getStartOffsetInParent());
        }
    };

    /**
     * Selects the insertion point. New methods
     * should be added after this point.
     *
     * @param psiCls Target class for adding methods.
     * @param mtd Added method.
     * @return The PSI element, after which to insert new method.
     */
    public @Nullable PsiElement select(PsiClass psiCls, PsiMethod mtd) {
        PsiMethod highestMethod = IgniteUtils.min(
            COMP,
            IgniteUtils.min(COMP, psiCls.findMethodsByName("readExternal", false)),
            IgniteUtils.min(COMP, psiCls.findMethodsByName("writeExternal", false)),
            IgniteUtils.min(COMP, psiCls.findMethodsByName("hashCode", false)),
            IgniteUtils.min(COMP, psiCls.findMethodsByName("equals", false)),
            IgniteUtils.min(COMP, psiCls.findMethodsByName("toString", false)));

        if (highestMethod != null)
            return highestMethod.getPrevSibling();

        PsiElement rBrace = psiCls.getRBrace();

        if (rBrace != null)
            return rBrace.getPrevSibling();

        return null;
    }

}
