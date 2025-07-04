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

package org.apache.ignite.idea.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.ignite.idea.IgniteMethodInsertionPointSelector;
import org.apache.ignite.idea.inspection.abbrev.IgniteAbbreviationConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.idea.util.IgniteUtils.Closure2;
import static org.apache.ignite.idea.util.IgniteUtils.capitalizeFirst;
import static org.apache.ignite.idea.util.IgniteUtils.transformCamelCase;
import static org.apache.ignite.idea.util.IgniteUtils.unCapitalizeFirst;

/**
 * Intention action for generating Apache Ignite style getter and setter.
 */
public class IgniteGetterSetterGenerator extends PsiElementBaseIntentionAction implements IntentionAction {
    /** Generate getter flag. */
    private final boolean genGetter;

    /** Generate setter flag. */
    private final boolean genSetter;

    /** Insertion point selector. */
    private final IgniteMethodInsertionPointSelector insPtSel = new IgniteMethodInsertionPointSelector();

    /**
     * Default constructor.
     */
    public IgniteGetterSetterGenerator() {
        this(true, true);
    }

    /**
     * @param genGetter Generate getter?
     * @param genSetter Generate setter?
     */
    public IgniteGetterSetterGenerator(boolean genGetter, boolean genSetter) {
        this.genGetter = genGetter;
        this.genSetter = genSetter;

        if (!genGetter && !genSetter)
            throw new IllegalArgumentException("At least one of genGetter or genSetter flags should be true.");
    }

    /** {@inheritDoc} */
    @NotNull public String getText() {
        if (genGetter && genSetter)
            return "Generate Apache Ignite style getter and setter";
        else if (genGetter)
            return "Generate Apache Ignite style getter";
        else if (genSetter)
            return "Generate Apache Ignite style setter";

        throw new AssertionError("At least one of genGetter or genSetter flags should be true.");
    }

    /** {@inheritDoc} */
    @NotNull public String getFamilyName() {
        return getText();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiField psiField = null;

        if (element instanceof PsiField)
            psiField = (PsiField)element;
        else if (element instanceof PsiJavaToken) {
            final PsiJavaToken token = (PsiJavaToken)element;

            if (token.getTokenType() == JavaTokenType.IDENTIFIER && token.getParent() instanceof PsiField)
                psiField = PsiTreeUtil.getParentOfType(element, PsiField.class, true);
        }

        if (psiField == null)
            return false;

        PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

        if (psiCls == null)
            return false;

        PsiElementFactory psiFactory = JavaPsiFacade.getInstance(project).getElementFactory();

        boolean ret = true;

        if (genGetter)
            ret = psiCls.findMethodBySignature(
                psiFactory.createMethod(psiField.getName(), psiField.getType()),
                true
            ) == null;

        if (ret && genSetter) {
            PsiModifierList psiFieldModifiers = psiField.getModifierList();

            // If field is not final.
            if (psiFieldModifiers == null || !psiFieldModifiers.hasExplicitModifier("final")) {
                PsiMethod mtd = psiFactory.createMethod(psiField.getName(), PsiType.VOID);

                mtd.getParameterList().add(psiFactory.createParameter(psiField.getName(), psiField.getType()));

                ret = psiCls.findMethodBySignature(mtd, true) == null;
            }
            else
                ret = false;
        }

        return ret;
    }

    /** {@inheritDoc} */
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {
        PsiField psiField = element instanceof PsiField ?
            (PsiField)element :
            PsiTreeUtil.getParentOfType(element, PsiField.class, true);
        PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

        if (psiField == null || psiCls == null)
            return;

        final PsiElementFactory psiFactory = JavaPsiFacade.getInstance(project).getElementFactory();

        String fieldName = psiField.getName();
        PsiDocComment psiFieldDoc = psiField.getDocComment();

        String docText = psiDocToText(psiFieldDoc).trim();

        IgniteAbbreviationConfig abbreviationConfig = ServiceManager.getService(project, IgniteAbbreviationConfig.class);

        String methodName = methodName(abbreviationConfig, fieldName);

        String comment = !docText.isEmpty() ? docText : camelCaseToText(methodName).trim() + '.';

        CodeStyleManager codeStyleMan = CodeStyleManager.getInstance(project);

        // Generate getter.
        if (genGetter) {
            PsiMethod psiGetter = psiFactory.createMethod(methodName, psiField.getType());

            PsiCodeBlock psiGetterBody = psiGetter.getBody();

            assert psiGetterBody != null;

            psiGetterBody.add(psiFactory.createStatementFromText("return " + fieldName + ';', null));

            psiGetter.addBefore(
                psiFactory.createDocCommentFromText(
                    "/**\n* @return " + capitalizeFirst(comment) + "\n*/"),
                psiGetter.getModifierList());

            psiCls.addAfter(
                codeStyleMan.reformat(psiGetter),
                insPtSel.select(psiCls, psiGetter));
        }

        if (genSetter) {
            PsiModifierList psiFieldModifiers = psiField.getModifierList();

            // Generate setter if field is not final.
            if (psiFieldModifiers == null || !psiFieldModifiers.hasExplicitModifier("final")) {
                PsiMethod psiSetter = psiFactory.createMethod(methodName, PsiType.VOID);

                String paramName = paramName(abbreviationConfig, fieldName);

                psiSetter.getParameterList().add(psiFactory.createParameter(paramName, psiField.getType()));

                PsiCodeBlock psiSetterBody = psiSetter.getBody();

                assert psiSetterBody != null;

                psiSetterBody.add(psiFactory.createStatementFromText(
                    (fieldName.equals(paramName) ? "this." : "") + fieldName + " = " + paramName + ';', null));

                psiSetter.addBefore(psiFactory.createDocCommentFromText(
                    "/**\n* @param " + paramName + " New " + unCapitalizeFirst(comment) + "\n*/"),
                    psiSetter.getModifierList());

                psiCls.addAfter(
                    codeStyleMan.reformat(psiSetter),
                    insPtSel.select(psiCls, psiSetter));
            }
        }
    }

    /** {@inheritDoc} */
    public boolean startInWriteAction() {
        return true;
    }

    /**
     * Reads a text from {@link PsiDocComment}.
     *
     * @param doc Doc comment object to read a text from.
     * @return Comment text.
     */
    private String psiDocToText(@Nullable PsiDocComment doc) {
        if (doc == null)
            return "";

        StringBuilder sb = new StringBuilder();

        for (PsiElement elem : doc.getDescriptionElements()) {
            if (elem instanceof PsiDocToken)
                sb.append(elem.getText());
        }

        return sb.toString();
    }

    /**
     * Transforms a field name to getter-setter method name.
     *
     * @param fieldName Field name.
     * @return Method name.
     */
    private String methodName(final IgniteAbbreviationConfig cfg, String fieldName) {
        return transformCamelCase(fieldName, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                if ("_".equals(part))
                    return "";

                String unw = cfg.getUnwrapping(part);
                String ret = unw != null ? unw : part;

                return idx > 0 ? capitalizeFirst(ret) : ret.toLowerCase();
            }
        });
    }

    /**
     * Transforms a field name to setter parameter name.
     *
     * @param fieldName Field name.
     * @return Parameter name.
     */
    private String paramName(final IgniteAbbreviationConfig cfg, String fieldName) {
        return transformCamelCase(fieldName, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                if ("_".equals(part))
                    return "";

                String abbr = cfg.getAbbreviation(part);
                String ret = abbr != null ? abbr : part;

                return idx > 0 ? capitalizeFirst(ret) : ret.toLowerCase();
            }
        });
    }

    /**
     * Transforms a camel case string to a space-separated text.
     *
     * @param camelCase Camel case string.
     * @return Space-separated text.
     */
    private String camelCaseToText(String camelCase) {
        return transformCamelCase(camelCase, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                return part.toLowerCase() + ' ';
            }
        });
    }

}
