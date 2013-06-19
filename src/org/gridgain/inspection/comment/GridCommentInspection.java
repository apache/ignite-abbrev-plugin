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
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.impl.source.tree.java.*;
import com.intellij.psi.javadoc.*;
import org.gridgain.inspection.abbrev.*;
import org.jetbrains.annotations.*;

import static org.gridgain.util.GridStringUtils.*;

/**
 * Inspection that searches for uncommented fields, methods,
 * and classes, and displays warnings for them.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridCommentInspection extends BaseJavaLocalInspectionTool {
    /** Abbreviation rules. */
    private final GridAbbreviationRules abbrevRules = GridAbbreviationRules.getInstance();

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
                final PsiClass cls = field.getContainingClass();

                if (cls == null)
                    return;

                // Don't display warning for anonymous classes.
                if (isAnonymousClass(cls))
                    return;

                if (!hasComment(field)) {
                    holder.registerProblem(
                        field.getNameIdentifier(),
                        getDisplayName(),
                        new LocalQuickFix() {
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
                        },
                        new LocalQuickFix() {
                            @NotNull @Override public String getName() {
                                return "Add default comment";
                            }

                            @NotNull @Override public String getFamilyName() {
                                return "";
                            }

                            @Override public void applyFix(@NotNull Project project,
                                @NotNull ProblemDescriptor descriptor) {
                                PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

                                field.addBefore(
                                    factory.createDocCommentFromText(
                                        "/** " + camelCaseToTextUnwrapAbbrev(field.getName()) + ". */"),
                                    field.getModifierList());
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

                    final PsiClass cls = mtd.getContainingClass();

                    if (cls == null)
                        return;

                    // Don't display warning for anonymous classes.
                    if (isAnonymousClass(cls))
                        return;

                    LocalQuickFix[] fix = null;

                    // If method is a constructor, we can generate default comment.
                    if (mtd.isConstructor()) {
                        fix = new LocalQuickFix[] {
                            new LocalQuickFix() {
                                @NotNull @Override public String getName() {
                                    return "Add default comment";
                                }

                                @NotNull @Override public String getFamilyName() {
                                    return "";
                                }

                                @Override public void applyFix(@NotNull Project project,
                                    @NotNull ProblemDescriptor descriptor) {
                                    StringBuilder sb = new StringBuilder("/**\n");

                                    PsiParameter[] params = mtd.getParameterList().getParameters();

                                    if (params.length > 0) {
                                        for (PsiParameter param : params)
                                            sb.append("* @param ").append(param.getName()).append(' ')
                                                .append(camelCaseToTextUnwrapAbbrev(param.getName())).append(".\n");
                                    }
                                    else
                                        sb.append("* Default constructor")
                                            .append(isExternalizable(cls) ? " (required by Externalizable)" : "")
                                            .append(".\n");

                                    sb.append("*/");

                                    PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

                                    mtd.addBefore(
                                        factory.createDocCommentFromText(sb.toString()),
                                        mtd.getModifierList());
                                }

                                private boolean isExternalizable(PsiClass cls) {
                                    for (PsiClass iface : cls.getInterfaces()) {
                                        if ("java.io.Externalizable".equals(iface.getQualifiedName()))
                                            return true;
                                    }

                                    return false;
                                }
                            }
                        };
                    }
                    // If there is a super method, we can apply {@inheritDoc} fix.
                    else if (mtd.findSuperMethods().length > 0) {
                        fix = new LocalQuickFix[] {
                            new LocalQuickFix() {
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
                            },
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

                                    for (PsiMethod mtd0 : cls.getMethods()) {
                                        if (mtd0 == mtd ||
                                            (!hasComment(mtd0) && mtd0.findSuperMethods().length > 0))
                                            addInheritDoc(mtd0, factory);
                                    }
                                }
                            }
                        };
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
             * Checks if a class is anonymous.
             *
             * @param cls Class to check.
             * @return {@code true} if class is anonymous, {@code false}
             *         otherwise.
             */
            private boolean isAnonymousClass(PsiClass cls) {
                return (cls.getNameIdentifier() == null && cls.getNode().getElementType() !=
                    JavaElementType.ENUM_CONSTANT_INITIALIZER);
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

    /**
     * Converts camel case to simple text, unwrapping abbreviations.
     *
     * @param camelCase Camel case string.
     * @return Resulting text.
     */
    private String camelCaseToTextUnwrapAbbrev(String camelCase) {
        return transformCamelCase(camelCase, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                if ("_".equals(part))
                    return "";

                String unw = abbrevRules.getUnwrapping(part);
                String ret = unw != null ? unw : part;

                return idx == 0 ? capitalizeFirst(ret.toLowerCase()) : " " + ret.toLowerCase();
            }
        });
    }
}
