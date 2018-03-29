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
import com.intellij.psi.impl.source.codeStyle.*;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.tree.*;
import org.jetbrains.annotations.*;

/**
 *
 */
public class GridPlublicInterfaceMethodsInspection extends BaseJavaLocalInspectionTool {

    /** {@inheritDoc} */
    @Nls
    @NotNull @Override public String getDisplayName() {
        return "\"public\" modifier in interface methods";
    }

    /** {@inheritDoc} */
    @Deprecated
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override public void visitField(PsiField field) {
                super.visitField(field);

                checkMember(field);
            }

            @Override public void visitMethod(final PsiMethod method) {
                super.visitMethod(method);

                checkMember(method);
            }

            private void checkMember(final PsiMember member) {
                PsiClass cls = member.getContainingClass();

                if (!cls.isInterface() || cls.isAnnotationType() || cls.getContainingClass() != null)
                    return;

                final PsiModifierList modifierList = member.getModifierList();

                if (modifierList == null)
                    return;

                if (modifierList.hasExplicitModifier(PsiModifier.PUBLIC))
                    return;

                holder.registerProblem(((PsiNameIdentifierOwner)member).getNameIdentifier(),
                    "\"public\" modifier must be present in interface methods/fields",
                    new LocalQuickFix() {
                    @NotNull @Override public String getName() {
                        return "Add \"public\"";
                    }

                    @NotNull @Override public String getFamilyName() {
                        return getName();
                    }

                    @Override public void applyFix(@NotNull Project project,
                        @NotNull ProblemDescriptor descriptor) {
                        if (!FileModificationService.getInstance().preparePsiElementForWrite(modifierList))
                            return;

                        if (member instanceof PsiMethod)
                            CodeEditUtil.markToReformat(((PsiMethod)member).getParameterList().getNode(), true);

                        modifierList.setModifierProperty(PsiModifier.PRIVATE, false);
                        modifierList.setModifierProperty(PsiModifier.PROTECTED, false);

                        IElementType type = JavaTokenType.PUBLIC_KEYWORD;
                        CompositeElement treeElement = (CompositeElement)modifierList.getNode();

                        if (treeElement.findChildByType(type) == null) {
                            TreeElement keyword = Factory.createSingleLeafElement(type, PsiModifier.PUBLIC, null,
                                member.getManager());
                            treeElement.addInternal(keyword, keyword, null, null);
                        }
                    }
                });
            }

            private boolean hasLineBreak(@NotNull PsiElement element, boolean forward) {
                PsiElement e = element;

                while (true) {
                    PsiElement next = forward ? e.getNextSibling() : e.getPrevSibling();

                    if (next == null) {
                        e = e.getParent();

                        if (e == null)
                            return true;

                        continue;
                    }

                    e = next;

                    if (e.getTextLength() == 0)
                        continue;

                    if (e instanceof PsiWhiteSpace || e instanceof PsiComment) {
                        if (e.textContains('\n'))
                            return true;
                    }
                    else {
                        return false;
                    }
                }
            }

            private void checkSameLineWithMethodName(@NotNull PsiAnnotation ann) {
                if (ann != null && hasLineBreak(ann, true)) {
                    holder.registerProblem(ann, "Annotation @" + ann.getNameReferenceElement().getReferenceName()
                        + " must be on the same line with the method name");
                }
            }
        };
    }
}
