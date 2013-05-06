// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection.comment;

import com.intellij.codeInsight.daemon.*;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.*;
import com.intellij.psi.javadoc.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Inspection that searches for uncommented fields, methods,
 * and classes, and displays warnings for them.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridCommentInspection extends BaseJavaLocalInspectionTool {
    /** {@inheritDoc} */
    @NotNull @Override public String getGroupDisplayName() {
        return GroupNames.STYLE_GROUP_NAME;
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getDisplayName() {
        return "Comment is absent";
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getShortName() {
        return "CommentAbsent";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
        final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            /** {@inheritDoc} */
            @Override public void visitField(final PsiField field) {
                if (!hasComment(field)) {
                    holder.registerProblem(field.getNameIdentifier(), getDisplayName(), new LocalQuickFix() {
                        @NotNull @Override public String getName() {
                            return "Add empty comment";
                        }

                        @NotNull @Override public String getFamilyName() {
                            return "";
                        }

                        @Override public void applyFix(@NotNull Project project,
                            @NotNull ProblemDescriptor desc) {
                            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

                            field.addBefore(factory.createDocCommentFromText("/** */"), field.getModifierList());
                        }
                    });
                }
            }

            /** {@inheritDoc} */
            @Override public void visitMethod(final PsiMethod mtd) {
                if (!hasComment(mtd)) {
                    PsiIdentifier mtdNameId = mtd.getNameIdentifier();

                    if (mtdNameId == null)
                        return;

                    // Don't display warning for anonymous classes.
                    final PsiClass cls = mtd.getContainingClass();

                    if (cls == null)
                        return;

                    if (cls.getNameIdentifier() == null)
                        return;

                    PsiMethod[] supers = mtd.findSuperMethods();

                    LocalQuickFix[] fix = null;

                    if (supers.length > 0) { // If there is a super method, we can apply {@inheritDoc} fix.
                        // Other overridden/implemented methods with empty comments.
                        final Collection<PsiMethod> similarMtds = new LinkedList<PsiMethod>();

                        for (PsiMethod mtd0 : cls.getMethods()) {
                            if (mtd0 != mtd && !hasComment(mtd0) && mtd0.findSuperMethods().length > 0)
                                similarMtds.add(mtd0);
                        }

                        LocalQuickFix fixOne = new LocalQuickFix() {
                            @NotNull @Override public String getName() {
                                return "Add /** {@inheritDoc} */";
                            }

                            @NotNull @Override public String getFamilyName() {
                                return "";
                            }

                            @Override public void applyFix(@NotNull Project project,
                                @NotNull ProblemDescriptor descriptor) {
                                addInheritDoc(mtd, JavaPsiFacade.getInstance(project).getElementFactory());
                            }
                        };

                        if (!similarMtds.isEmpty()) {
                            fix = new LocalQuickFix[] {
                                new LocalQuickFix() {
                                    @NotNull @Override public String getName() {
                                        return "Add /** {@inheritDoc} */ for all overridden/implemented methods";
                                    }

                                    @NotNull @Override public String getFamilyName() {
                                        return "";
                                    }

                                    @Override public void applyFix(@NotNull Project project,
                                        @NotNull ProblemDescriptor descriptor) {
                                        PsiElementFactory factory =
                                            JavaPsiFacade.getInstance(project).getElementFactory();

                                        for (PsiMethod mtd0 : similarMtds)
                                            addInheritDoc(mtd0, factory);

                                        addInheritDoc(mtd, factory);
                                    }
                                },
                                fixOne
                            };
                        }
                        else
                            fix = new LocalQuickFix[] { fixOne };
                    }

                    holder.registerProblem(mtdNameId, getDisplayName(), fix);
                }
            }

            /** {@inheritDoc} */
            @Override public void visitClass(PsiClass cls) {
                PsiIdentifier nameId = cls.getNameIdentifier();

                if (nameId != null && !(cls instanceof PsiTypeParameterImpl) && !hasComment(cls))
                    holder.registerProblem(nameId, getDisplayName());
            }

            /**
             * Checks if element does have a comment.
             *
             * @param elem Element to check.
             * @return {@code true} if element has a comment, {@code false} otherwise.
             */
            private boolean hasComment(PsiDocCommentOwner elem) {
                PsiDocComment comment = elem.getDocComment();

                return comment != null && comment.getText() != null && !comment.getText().isEmpty();
            }

            /**
             * Adds an inheritDoc comment to a method.
             *
             * @param mtd Method to add comment to.
             * @param factory PSI element factory.
             */
            private void addInheritDoc(PsiMethod mtd, PsiElementFactory factory) {
                mtd.addBefore(
                    factory.createDocCommentFromText("/** {@inheritDoc} */"),
                    mtd.getModifierList());
            }
        };
    }
}
