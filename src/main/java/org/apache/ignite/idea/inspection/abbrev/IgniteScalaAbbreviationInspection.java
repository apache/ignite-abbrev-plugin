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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.JavaRefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValue;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueDeclaration;
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter;
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariableDefinition;
import org.apache.ignite.idea.util.IgniteUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Inspects usages of Ignite abbreviations. */
public class IgniteScalaAbbreviationInspection extends LocalInspectionTool {
    /** Abbreviation exceptions. */
    private final Set<String> abbrExceptions = Set.of("value");

    /** {@inheritDoc} */
    @Override public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession ses) {
        return new ScalaElementVisitor() {
            /** {@inheritDoc} */
            @Override public void visitValueDeclaration(ScValueDeclaration v) {
                Arrays.stream(v.declaredElementsArray()).forEach(this::checkShouldAbbreviate);
            }

            /** {@inheritDoc} */
            @Override public void visitValue(ScValue v) {
                Arrays.stream(v.declaredElementsArray()).forEach(this::checkShouldAbbreviate);
            }

            /** {@inheritDoc} */
            @Override public void visitParameter(ScParameter p) {
                checkShouldAbbreviate(p, p.getNameIdentifier());
            }

            /** {@inheritDoc} */
            @Override public void visitVariableDefinition(ScVariableDefinition v) {
                Arrays.stream(v.declaredElementsArray()).forEach(this::checkShouldAbbreviate);
            }

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private void checkShouldAbbreviate(PsiNamedElement elem) {
                check0(IgniteUtils.camelCaseParts(elem.getName()), elem);
            }

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private void checkShouldAbbreviate(PsiElement elem, PsiIdentifier id) {
                check0(IgniteUtils.camelCaseParts(id.getText()), elem);
            }

            /**
             * Checks that all identifier parts are correctly abbreviated. Registers problem if needed.
             *
             * @param parts Identifier parts.
             * @param elem Checked identifier element.
             */
            private void check0(List<String> parts, PsiElement elem) {
                for (String part : parts) {
                    IgniteAbbreviationConfig cfg = elem.getProject().getService(IgniteAbbreviationConfig.class);

                    if (!abbrExceptions.contains(part) && cfg.getAbbreviation(part) != null) {
                        holder.registerProblem(elem, "Abbreviation should be used",
                            new RenameToFix(cfg.replaceWithAbbreviations(parts)));

                        return;
                    }
                }
            }
        };
    }

    /** Rename quick fix. */
    private static class RenameToFix implements LocalQuickFix, BatchQuickFix {
        /** Logger. */
        private static final Logger LOG = Logger.getInstance(RenameToFix.class);

        /** */
        private final String name;

        /**
         * @param name New name for the identifier.
         */
        public RenameToFix(String name) {
            this.name = name;
        }

        /** {@inheritDoc} */
        @Override public String getName() {
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
        @Override public void applyFix(Project proj, ProblemDescriptor descr) {
            PsiElement element = descr.getPsiElement();

            JavaRefactoringFactory factory = JavaRefactoringFactory.getInstance(proj);
            RenameRefactoring renRefactoring = factory.createRename(element, name, false, false);
            renRefactoring.run();
        }
    }
}
