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

package org.apache.ignite.idea.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 *
 */
public class IgniteWrongComparationInspection extends AbstractBaseJavaLocalInspectionTool {
    /** */
    private static final Set<String> UNCOMPARABLE_TYPE = new HashSet<String>(Arrays.asList(
        "org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion"
    ));

    /** {@inheritDoc} */
    @Nls
    @NotNull @Override public String getDisplayName() {
        return "Illegal comparation, use equals() instead of '=='";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override public void visitBinaryExpression(PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);

                IElementType tokenType = expression.getOperationTokenType();

                if (tokenType == JavaTokenType.EQEQ || tokenType == JavaTokenType.NE) {
                    PsiExpression lOperand = expression.getLOperand();
                    PsiExpression rOperand = expression.getROperand();

                    if (lOperand != null && rOperand != null) {
                        PsiType type1 = lOperand.getType();
                        PsiType type2 = rOperand.getType();

                        if (type1 != null && type2 != null && !type1.equals(PsiType.NULL) && !type2.equals(PsiType.NULL)
                            && (UNCOMPARABLE_TYPE.contains(type1.getCanonicalText())
                            || UNCOMPARABLE_TYPE.contains(type2.getCanonicalText())))
                            holder.registerProblem(expression, getDisplayName());
                    }
                }
            }
        };
    }
}
