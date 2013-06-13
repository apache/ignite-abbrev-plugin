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

import static org.gridgain.util.GridStringUtils.*;

/**
 * Intention action for generating GridGain-style getter and setter.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridGetterSetterGenerator extends PsiElementBaseIntentionAction implements IntentionAction {
    /** Abbreviation rules. */
    private final GridAbbreviationRules abbrevRules = GridAbbreviationRules.getInstance();

    /** Generate getter flag. */
    private final boolean genGetter;

    /** Generate setter flag. */
    private final boolean genSetter;

    /**
     * Default constructor.
     */
    public GridGetterSetterGenerator() {
        this(true, true);
    }

    /**
     * @param genGetter Generate getter?
     * @param genSetter Generate setter?
     */
    public GridGetterSetterGenerator(boolean genGetter, boolean genSetter) {
        this.genGetter = genGetter;
        this.genSetter = genSetter;

        if (!genGetter && !genSetter)
            throw new IllegalArgumentException("At least one of genGetter or genSetter flags should be true.");
    }

    /** {@inheritDoc} */
    @NotNull public String getText() {
        if (genGetter && genSetter)
            return "Generate GridGain-style getter and setter";
        else if (genGetter)
            return "Generate GridGain-style getter";
        else if (genSetter)
            return "Generate GridGain-style setter";

        throw new AssertionError("At least one of genGetter or genSetter flags should be true.");
    }

    /** {@inheritDoc} */
    @NotNull public String getFamilyName() {
        return getText();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        if (!element.isWritable()) return false;

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

        String methodName = methodName(fieldName);

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

            psiCls.addBefore(codeStyleMan.reformat(psiGetter), psiCls.getRBrace());
        }

        if (genSetter) {
            PsiModifierList psiFieldModifiers = psiField.getModifierList();

            // Generate setter if field is not final.
            if (psiFieldModifiers == null || !psiFieldModifiers.hasExplicitModifier("final")) {
                PsiMethod psiSetter = psiFactory.createMethod(methodName, PsiType.VOID);

                String paramName = paramName(fieldName);

                psiSetter.getParameterList().add(psiFactory.createParameter(paramName, psiField.getType()));

                PsiCodeBlock psiSetterBody = psiSetter.getBody();

                assert psiSetterBody != null;

                psiSetterBody.add(psiFactory.createStatementFromText(
                    (fieldName.equals(paramName) ? "this." : "") + fieldName + " = " + paramName + ';', null));

                psiSetter.addBefore(psiFactory.createDocCommentFromText(
                    "/**\n* @param " + paramName + " New " + unCapitalizeFirst(comment) + "\n*/"),
                    psiSetter.getModifierList());

                psiCls.addBefore(codeStyleMan.reformat(psiSetter), psiCls.getRBrace());
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
    private String methodName(String fieldName) {
        return transformCamelCase(fieldName, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                if ("_".equals(part))
                    return "";

                String unw = abbrevRules.getUnwrapping(part);
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
    private String paramName(String fieldName) {
        return transformCamelCase(fieldName, new Closure2<String, Integer, String>() {
            @Override public String apply(String part, Integer idx) {
                if ("_".equals(part))
                    return "";

                String abbr = abbrevRules.getAbbreviation(part);
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
