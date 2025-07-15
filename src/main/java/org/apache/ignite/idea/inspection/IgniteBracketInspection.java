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

import com.intellij.codeInsight.*;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.tree.*;
import org.jetbrains.annotations.*;

/**
 *
 */
public class IgniteBracketInspection extends AbstractBaseJavaLocalInspectionTool {

    /** {@inheritDoc} */
    @Nls
    @NotNull @Override public String getDisplayName() {
        return "Illegal '{ }' for one line statement";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            private void checkStatement(@Nullable final PsiStatement branch) {
                if (branch == null)
                    return;

                if (!(branch instanceof PsiBlockStatement))
                    return;

                PsiCodeBlock codeBlock = ((PsiBlockStatement)branch).getCodeBlock();

                PsiStatement statement = null;

                for (PsiElement e = codeBlock.getFirstChild(); e != null; e = e.getNextSibling()) {
                    if (e instanceof PsiWhiteSpace)
                        continue;

                    if (e instanceof LeafPsiElement) {
                        IElementType tokenType = ((LeafPsiElement)e).getElementType();

                        if (tokenType == JavaTokenType.LBRACE || tokenType == JavaTokenType.RBRACE)
                            continue;
                    }

                    if (e instanceof PsiStatement) {
                        if (statement != null)
                            return;

                        statement = (PsiStatement)e;

                        continue;
                    }

                    return;
                }

                if (statement == null || statement.textContains('\n'))
                    return;

                final PsiStatement finalStatement = statement;

                holder.registerProblem(branch, getDisplayName(), new LocalQuickFix() {
                    @NotNull @Override public String getName() {
                        return "Remove unnecessary '{ }'";
                    }

                    @NotNull @Override public String getFamilyName() {
                        return getName();
                    }

                    @Override public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                        if (!branch.isValid() || !finalStatement.isValid())
                            return;

                        if (!FileModificationService.getInstance().preparePsiElementForWrite(branch))
                            return;

                        branch.replace(finalStatement);
                    }
                });
            }

            @Override public void visitIfStatement(PsiIfStatement statement) {
                checkStatement(statement.getThenBranch());
                checkStatement(statement.getElseBranch());
            }

            @Override public void visitForStatement(PsiForStatement statement) {
                checkStatement(statement.getBody());
            }

            @Override public void visitForeachStatement(PsiForeachStatement statement) {
                checkStatement(statement.getBody());
            }

            @Override public void visitWhileStatement(PsiWhileStatement statement) {
                checkStatement(statement.getBody());
            }
        };
    }
}
