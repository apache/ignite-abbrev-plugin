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
import org.jetbrains.annotations.*;

/**
 *
 */
public class GridAnnotationInspection extends BaseJavaLocalInspectionTool {

    /** {@inheritDoc} */
    @Nls
    @NotNull @Override public String getDisplayName() {
        return "Annotation formatting";
    }

    /** {@inheritDoc} */
    @Deprecated
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override public void visitMethod(PsiMethod method) {
                super.visitMethod(method);

                PsiModifierList modifierList = method.getModifierList();

                if (modifierList == null)
                    return;

                for (PsiElement e = modifierList.getFirstChild(); e != null; e = e.getNextSibling()) {
                    if (!(e instanceof PsiAnnotation))
                        continue;

                    PsiAnnotation ann = (PsiAnnotation)e;

                    String name = ann.getQualifiedName();

                    if (Nullable.class.getName().equals(name))
                        checkSameLineWithMethodName(ann);
                    else if (NotNull.class.getName().equals(name)) {
                        // No-op.
                    }
                    else if (Override.class.getName().equals(name))
                        checkSameLineWithMethodName(ann);
                    else {
                        if (!hasLineBreak(ann, true) || !hasLineBreak(ann, false)) {
                            holder.registerProblem(ann, "Annotation @" + ann.getNameReferenceElement().getReferenceName()
                                + " must be on the separated line");
                        }
                    }
                }
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
