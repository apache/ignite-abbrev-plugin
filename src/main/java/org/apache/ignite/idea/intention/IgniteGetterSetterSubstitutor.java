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

package org.apache.ignite.idea.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.util.IncorrectOperationException;
import org.apache.ignite.idea.util.IgniteUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Tries to bring all getters and setters to Apache Ignite style.
 */
public class IgniteGetterSetterSubstitutor extends PsiElementBaseIntentionAction implements IntentionAction {
    /** {@inheritDoc} */
    @NotNull public String getText() {
        return "Ignitify getters and setters";
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getFamilyName() {
        return getText();
    }

    /** {@inheritDoc} */
    @Override public boolean startInWriteAction() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor,
        @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    /** {@inheritDoc} */
    @Override public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (element instanceof PsiJavaToken) {
            final PsiJavaToken token = (PsiJavaToken)element;

            if (token.getTokenType() == JavaTokenType.IDENTIFIER && token.getParent() instanceof PsiClass) {
                PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

                if (psiCls == null)
                    return false;

                for (PsiMethod psiMethod : psiCls.getMethods()) {
                    if (methodNameMatches(psiMethod.getName()))
                        return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {
        PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

        if (psiCls == null)
            return;

        JavaRefactoringFactory rFactory = JavaRefactoringFactory.getInstance(project);

        for (PsiMethod psiMethod : psiCls.getMethods()) {
            String methodName = psiMethod.getName();

            if (methodNameMatches(methodName)) {
                rFactory.createRename(
                    psiMethod,
                    IgniteUtils.unCapitalizeFirst(methodName.substring(3)),
                    false,
                    false)
                .run();
            }
        }
    }

    /**
     * Checks if method can be Ignitified.
     *
     * @param methodName Method name.
     * @return {@code true} if matches, {@code false} otherwise.
     */
    private boolean methodNameMatches(String methodName) {
        return methodName.length() > 3 &&
            (methodName.startsWith("get") || methodName.startsWith("set")) &&
            Character.isUpperCase(methodName.charAt(3));
    }
}
