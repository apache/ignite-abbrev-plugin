/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
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
public class GridEmptyLineInspection extends BaseJavaLocalInspectionTool {
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
