package org.gridgain.intention;// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

import com.intellij.codeInsight.intention.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.*;
import com.intellij.psi.javadoc.*;
import com.intellij.psi.util.*;
import com.intellij.util.*;
import org.gridgain.inspection.abbrev.*;
import org.jetbrains.annotations.*;

import java.io.*;

import static org.gridgain.util.GridStringUtils.*;

/**
 * Intention action for generating GridGain-style getter and setter.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridGetterSetterGenerator extends PsiElementBaseIntentionAction implements IntentionAction {
    /** Abbreviation rules. */
    private final GridAbbreviationRules abbrevRules;

    public GridGetterSetterGenerator() {
        String ggHome = System.getenv("GRIDGAIN_HOME");

        if (ggHome == null)
            abbrevRules = GridAbbreviationRules.getInstance(null);
        else {
            File abbrevFile = new File(new File(ggHome), "idea" + File.separatorChar + "abbreviation.properties");

            if (!abbrevFile.exists() || !abbrevFile.isFile())
                abbrevRules = GridAbbreviationRules.getInstance(null);
            else
                abbrevRules = GridAbbreviationRules.getInstance(abbrevFile);
        }
    }

    /** {@inheritDoc} */
    @NotNull public String getText() {
        return "Generate GridGain-style getter and setter";
    }

    /** {@inheritDoc} */
    @NotNull public String getFamilyName() {
        return getText();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!element.isWritable()) return false;

        if (element instanceof PsiJavaToken) {
            final PsiJavaToken token = (PsiJavaToken)element;

            if (token.getTokenType() == JavaTokenType.IDENTIFIER && token.getParent() instanceof PsiField) {
                PsiField psiField = PsiTreeUtil.getParentOfType(element, PsiField.class, true);
                PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

                if (psiField == null || psiCls == null)
                    return false;

                // Only applicable if method with field name does not exist.
                return psiCls.findMethodsByName(psiField.getName(), true).length == 0;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {
        PsiField psiField = PsiTreeUtil.getParentOfType(element, PsiField.class, true);
        PsiClass psiCls = PsiTreeUtil.getParentOfType(element, PsiClass.class, true);

        if (psiField == null || psiCls == null)
            return;

        final PsiElementFactory psiFactory = JavaPsiFacade.getInstance(project).getElementFactory();

        String fieldName = psiField.getName();
        PsiDocComment psiFieldDoc = psiField.getDocComment();

        String docText = psiDocToText(psiFieldDoc).trim();

        String methodName = methodName(fieldName);

        String comment = !docText.isEmpty() ? docText : camelCaseToText(methodName).trim() + '.';

        // Generate getter.
        PsiMethod psiGetter = psiFactory.createMethod(methodName, psiField.getType());

        PsiCodeBlock psiGetterBody = psiGetter.getBody();

        assert psiGetterBody != null;

        psiGetterBody.add(psiFactory.createStatementFromText("return " + fieldName + ';', null));

        psiGetter.addBefore(
            psiFactory.createDocCommentFromText(
                "/**\n* @return " + capitalizeFirst(comment) + "\n*/"),
            psiGetter.getModifierList());

        CodeStyleManager codeStyleMan = CodeStyleManager.getInstance(project);

        psiCls.addBefore(codeStyleMan.reformat(psiGetter), psiCls.getRBrace());

        PsiModifierList psiFieldModifiers = psiField.getModifierList();

        // Generate setter if field is not final.
        if (psiFieldModifiers == null || !psiFieldModifiers.hasExplicitModifier("final")) {
            PsiMethod psiSetter = psiFactory.createMethod(methodName, PsiType.VOID);

            psiSetter.getParameterList().add(psiFactory.createParameter(fieldName, psiField.getType()));

            PsiCodeBlock psiSetterBody = psiSetter.getBody();

            assert psiSetterBody != null;

            psiSetterBody.add(psiFactory.createStatementFromText(
                "this." + fieldName + " = " + fieldName + ';', null));

            psiSetter.addBefore(psiFactory.createDocCommentFromText(
                "/**\n* @param " + fieldName + " New " + unCapitalizeFirst(comment) + "\n*/"),
                psiSetter.getModifierList());

            psiCls.addBefore(codeStyleMan.reformat(psiSetter), psiCls.getRBrace());
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
    private String methodName(String fieldName) {
        return transformCamelCase(fieldName, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                String unw = abbrevRules.getUnwrapping(part);
                String ret = unw != null ? unw : part;

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
