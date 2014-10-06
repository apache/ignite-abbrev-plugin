/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.*;

/**
 *
 */
public class GridBracketInspection extends BaseJavaLocalInspectionTool {

    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            private void checkStatement(@Nullable final PsiStatement branch) {
                if (branch == null)
                    return;

                if (!(branch instanceof PsiBlockStatement))
                    return;

                PsiCodeBlock codeBlock = ((PsiBlockStatement)branch).getCodeBlock();

                final PsiStatement[] statements = codeBlock.getStatements();

                if (statements.length != 1)
                    return;

                if (statements[0].textContains('\n'))
                    return;

                holder.registerProblem(branch, getDisplayName(), new LocalQuickFix() {
                    @NotNull @Override public String getName() {
                        return "Remove unnecessary '{ }'";
                    }

                    @NotNull @Override public String getFamilyName() {
                        return getName();
                    }

                    @Override public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                        branch.replace(statements[0]);
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

            @Override public void visitWhileStatement(PsiWhileStatement statement) {
                checkStatement(statement.getBody());
            }
        };
    }
}
