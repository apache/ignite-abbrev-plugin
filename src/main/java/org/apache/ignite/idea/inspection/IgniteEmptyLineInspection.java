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
import org.jetbrains.annotations.*;

/**
 *
 */
public class IgniteEmptyLineInspection extends BaseJavaLocalInspectionTool {
    /** {@inheritDoc} */
    @Nls @NotNull @Override public String getDisplayName() {
        return "Illegal empty line before first class element";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
        boolean isOnTheFly) {
        return new JavaElementVisitor() {

            private void checkMember(PsiMember member) {
                final PsiElement prevWhileSpace = member.getPrevSibling();

                if (!(prevWhileSpace instanceof PsiWhiteSpace))
                    return;

                PsiElement sibling = prevWhileSpace.getPrevSibling();

                if (!(sibling instanceof LeafPsiElement) || ((LeafPsiElement)sibling).getElementType() != JavaTokenType.LBRACE) {
                    return;
                }

                PsiClass containingClass = member.getContainingClass();

                if (containingClass == null)
                    return;

                if (!(containingClass.getParent() instanceof PsiFile))
                    return;

                int breakLineCnt = 0;

                String spaceText = prevWhileSpace.getText();

                for (int i = 0; i < spaceText.length(); i++) {
                    if (spaceText.charAt(i) == '\n')
                        breakLineCnt++;
                }

                if (breakLineCnt > 1) {
                    holder.registerProblem(prevWhileSpace, getDisplayName(), new LocalQuickFix() {
                        @NotNull @Override public String getName() {
                            return "Remove Illegal line break";
                        }

                        @NotNull @Override public String getFamilyName() {
                            return getName();
                        }

                        @Override public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                            if (!prevWhileSpace.isValid())
                                return;

                            if (!FileModificationService.getInstance().preparePsiElementForWrite(prevWhileSpace))
                                return;

                            LeafElement newWhileSpace = Factory.createSingleLeafElement(JavaTokenType.WHITE_SPACE, "\n",
                                null, prevWhileSpace.getManager());

                            prevWhileSpace.getNode().getTreeParent().replaceChild(prevWhileSpace.getNode(),
                                newWhileSpace);
                        }
                    });
                }
            }

            @Override public void visitField(PsiField field) {
                if (field.getDocComment() != null)
                    checkMember(field);
            }

            @Override public void visitMethod(PsiMethod method) {
                if (method.getDocComment() != null)
                    checkMember(method);
            }
        };
    }
}
