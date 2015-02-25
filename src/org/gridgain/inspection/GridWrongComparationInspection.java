/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 *
 */
public class GridWrongComparationInspection extends BaseJavaLocalInspectionTool {
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
                    PsiType type1 = expression.getLOperand().getType();
                    PsiType type2 = expression.getROperand().getType();

                    if (!type1.equals(PsiType.NULL) && !type2.equals(PsiType.NULL)
                        && (UNCOMPARABLE_TYPE.contains(type1.getCanonicalText())
                        || UNCOMPARABLE_TYPE.contains(type2.getCanonicalText())))
                        holder.registerProblem(expression, getDisplayName());
                }
            }
        };
    }
}
