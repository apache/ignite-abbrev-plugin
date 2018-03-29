// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.apache.ignite.idea.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.util.IncorrectOperationException;
import org.apache.ignite.idea.util.GridUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Tries to bring all getters and setters to GridGain style.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridGetterSetterSubstitutor  extends PsiElementBaseIntentionAction implements IntentionAction {
    /** {@inheritDoc} */
    @NotNull public String getText() {
        return "GridGainify getters and setters";
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
                    GridUtils.unCapitalizeFirst(methodName.substring(3)),
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
        return methodName.length() > 3 &&
            (methodName.startsWith("get") || methodName.startsWith("set")) &&
            Character.isUpperCase(methodName.charAt(3));
    }
}
