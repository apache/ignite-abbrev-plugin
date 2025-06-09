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

package org.apache.ignite.idea.inspection.comment;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.impl.source.tree.java.PsiTypeParameterImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.ignite.idea.inspection.abbrev.IgniteAbbreviationConfig;
import org.apache.ignite.idea.util.IgniteUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection that searches for uncommented fields, methods,
 * and classes, and displays warnings for them.
 */
public class IgniteCommentInspection extends BaseJavaLocalInspectionTool {
    /** {@inheritDoc} */
    @NotNull @Override public String getShortName() {
        return "CommentAbsent";
    }

    /** {@inheritDoc} */
    @NotNull @Override public String getDisplayName() {
        return "Comment is absent";
    }

    /** {@inheritDoc} */
    @NotNull @Override public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder,
        final boolean isOnTheFly) {

        final IgniteAbbreviationConfig config = ServiceManager.getService(holder.getProject(),
            IgniteAbbreviationConfig.class);

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
                                        "/** " + camelCaseToTextUnwrapAbbrev(config, field.getName()) + ". */"),
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

                    // Workaround for a noticed accidental error.
                    if (mtd.getParent() == null)
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
                                    if (!mtd.isValid())
                                        return;

                                    StringBuilder sb = new StringBuilder("/**\n");

                                    PsiParameter[] params = mtd.getParameterList().getParameters();

                                    if (params.length > 0) {
                                        for (PsiParameter param : params)
                                            sb.append("* @param ").append(param.getName()).append(' ')
                                                .append(camelCaseToTextUnwrapAbbrev(config, param.getName())).append(".\n");
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
                    else {
                        fix = new LocalQuickFix[] {
                            new LocalQuickFix() {
                                @NotNull @Override public String getName() {
                                    return "Add default comment";
                                }

                                @NotNull @Override public String getFamilyName() {
                                    return "";
                                }

                                @Override public void applyFix(@NotNull Project project,
                                    @NotNull ProblemDescriptor desc) {
                                    StringBuilder sb = new StringBuilder("/**\n");

                                    PsiParameter[] params = mtd.getParameterList().getParameters();

                                    if (params.length > 0) {
                                        for (PsiParameter param : params)
                                            sb.append("* @param ").append(param.getName()).append(' ')
                                                .append(camelCaseToTextUnwrapAbbrev(config, param.getName()))
                                                .append(".\n");
                                    }
                                    else
                                        sb.append("*\n");

//                                    PsiType returnType = mtd.getReturnType();
//
//                                    if (!returnType.equalsToText("void")) {
//                                        sb.append("* @return ")
//                                            .append(camelCaseToTextUnwrapAbbrev(returnType.getCanonicalText()))
//                                            .append(".\n");
//                                    }

                                    sb.append("*/");

                                    PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();

                                    mtd.addBefore(
                                        factory.createDocCommentFromText(sb.toString()),
                                        mtd.getModifierList());
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

                if (nameId != null && !(cls instanceof PsiTypeParameterImpl) && !hasComment(cls) && !isAnonymousClass(cls))
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
                if (cls.getNameIdentifier() == null && cls.getNode().getElementType() !=
                    JavaElementType.ENUM_CONSTANT_INITIALIZER)
                    return true;

                PsiMember parent = PsiTreeUtil.getParentOfType(cls, PsiClass.class, PsiMember.class);

                return parent != null && !(parent instanceof PsiClass); // Classes inside method or field initializer.
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
    private String camelCaseToTextUnwrapAbbrev(final IgniteAbbreviationConfig cfg, String camelCase) {
        return IgniteUtils.transformCamelCase(camelCase, new IgniteUtils.Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                if ("_".equals(part))
                    return "";

                String unw = cfg.getUnwrapping(part);
                String ret = unw != null ? unw : part;

                return idx == 0 ? IgniteUtils.capitalizeFirst(ret.toLowerCase()) : " " + ret.toLowerCase();
            }
        });
    }
}
