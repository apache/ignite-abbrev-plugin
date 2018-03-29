/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
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
