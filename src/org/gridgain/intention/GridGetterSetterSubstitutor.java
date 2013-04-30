// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.intention;

import com.intellij.codeInsight.intention.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.psi.util.*;
import com.intellij.refactoring.*;
import com.intellij.util.*;
import org.gridgain.util.*;
import org.jetbrains.annotations.*;

/**
 * Tries to bring all getters and setters to GridGain style.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridGetterSetterSubstitutor  extends PsiElementBaseIntentionAction implements IntentionAction {
    /** {@inheritDoc} */
    @NotNull public String getText() {
        return "GridGainify getters and setters (alpha, use with care!)";
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getFamilyName() {
        return getText();
    }

    /** {@inheritDoc} */
    @Override public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!element.isWritable()) return false;

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
                    GridStringUtils.unCapitalizeFirst(methodName.substring(3)),
                    false,
                    false)
                .run();
            }
        }
    }

    /**
     * Checks if method can be GridGainified.
     *
     * @param methodName Method name.
     * @return {@code true} if matches, {@code false} otherwise.
     */
    private boolean methodNameMatches(String methodName) {
        // TODO: also check the 4-th letter
        return methodName.length() > 3 && (methodName.startsWith("get") || methodName.startsWith("set"));
    }
}
