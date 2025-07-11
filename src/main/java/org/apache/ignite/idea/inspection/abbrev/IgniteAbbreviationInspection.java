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

package org.apache.ignite.idea.inspection.abbrev;

import com.intellij.codeInspection.*;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.refactoring.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.apache.ignite.idea.util.IgniteUtils.*;

/**
 * Inspection that checks variable names for usage of restricted words that
 * need to be abbreviated.
 */
public class IgniteAbbreviationInspection extends BaseJavaLocalInspectionTool {
    /** {@inheritDoc} */
    @NotNull @Override public String getShortName() {
        return "JavaAbbreviationUsage";
    }

    /** {@inheritDoc} */
    @Nls
    @NotNull @Override public String getDisplayName() {
        return "Incorrect Java abbreviation usage";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
        final boolean isOnTheFly) {
        final IgniteAbbreviationConfig cfg = ServiceManager.getService(holder.getProject(), IgniteAbbreviationConfig.class);

        return new JavaElementVisitor() {
            /** {@inheritDoc} */
            @Override public void visitField(PsiField field) {
                boolean isFinal = false;

                boolean isStatic = false;

                if (field instanceof PsiEnumConstant)
                    return;

                for (PsiElement el : field.getChildren()) {
                    if (el instanceof PsiModifierList) {
                        PsiModifierList modifiers = (PsiModifierList)el;

                        isFinal = modifiers.hasExplicitModifier("final");

                        isStatic = modifiers.hasExplicitModifier("static");
                    }

                    if (el instanceof PsiIdentifier && !(isFinal && isStatic))
                        checkShouldAbbreviate(field, el);
                }
            }

            /** {@inheritDoc} */
            @Override public void visitLocalVariable(PsiLocalVariable variable) {
                for (PsiElement el : variable.getChildren()) {
                    if (el instanceof PsiIdentifier)
                        checkShouldAbbreviate(variable, el);
                }
            }

            /** {@inheritDoc} */
            @Override public void visitMethod(PsiMethod mtd) {
                for (PsiParameter par : mtd.getParameterList().getParameters()) {
                    for (PsiElement el : par.getChildren()) {
                        if (el instanceof PsiIdentifier)
                            checkShouldAbbreviate(par, el);
                    }
                }
            }

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param toCheck Element to check and rename.
             * @param el Element to highlight the problem.
             */
            private void checkShouldAbbreviate(PsiNamedElement toCheck, PsiElement el) {
                if (!el.isPhysical())
                    return;

                List<String> nameParts = camelCaseParts(toCheck.getName());

                for (String part : nameParts) {
                    if (cfg.getAbbreviation(part) != null) {
                        holder.registerProblem(el, "Abbreviation should be used",
                            new RenameToFix(cfg.replaceWithAbbreviations(nameParts)));

                        break;
                    }
                }
            }
        };
    }

    /**
     * Renames variable to a given name.
     */
    private class RenameToFix implements LocalQuickFix, BatchQuickFix {
        /** Logger. */
        private static final Logger LOG = Logger.getInstance(RenameToFix.class);

        /** New proposed variable name. */
        private String name;

        /**
         * Creates rename to fix.
         *
         * @param name New variable name.
         */
        private RenameToFix(@NotNull String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        @NotNull public String getName() {
            return "Rename to " + name;
        }

        /** {@inheritDoc} */
        @Override public String getFamilyName() {
            return "Use abbreviation";
        }

        /** {@inheritDoc} */
        @Override public boolean startInWriteAction() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public void applyFix(@NotNull Project project, CommonProblemDescriptor[] descriptors,
            @NotNull List<PsiElement> psiElementsToIgnore, @Nullable Runnable refreshViews) {
            for (CommonProblemDescriptor descriptor : descriptors) {
                QuickFix[] fixes = descriptor.getFixes();

                if (fixes == null || fixes.length == 0) {
                    LOG.warn("No fixes found.");
                    continue;
                }

                Optional<QuickFix> renameFix = Arrays.stream(fixes)
                    .filter(RenameToFix.class::isInstance)
                    .findAny();

                if (renameFix.isEmpty()) {
                    LOG.warn("No rename fix found.");
                    continue;
                }

                renameFix.get().applyFix(project, descriptor);
            }
        }

        /** {@inheritDoc} */
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descr) {
            PsiElement element = descr.getPsiElement().getParent();

            rename(project, element, name);
        }

        /**
         * Renames given element to a given name.
         *
         * @param project Project in which variable is located.
         * @param element Element to rename.
         * @param name New element name.
         */
        private void rename(@NotNull Project project, @NotNull PsiElement element, @NotNull String name) {
            JavaRefactoringFactory factory = JavaRefactoringFactory.getInstance(project);

            RenameRefactoring renRefactoring = factory.createRename(element, name, false, false);

            renRefactoring.run();
        }
    }
}
