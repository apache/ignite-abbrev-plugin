// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain;

import com.intellij.psi.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.util.GridUtils.*;

/**
 * Defines the insertion point for new methods.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridMethodInsertionPointSelector {
    /** Method position comparator. */
    private final Comparator<PsiMethod> COMP = new Comparator<PsiMethod>() {
        @Override public int compare(PsiMethod o1, PsiMethod o2) {
            return Integer.valueOf(o1.getStartOffsetInParent()).compareTo(o2.getStartOffsetInParent());
        }
    };

    /**
     * Selects the insertion point. New methods
     * should be added after this point.
     *
     * @param psiCls Target class for adding methods.
     * @param mtd Added method.
     * @return The PSI element, after which to insert new method.
     */
    public @Nullable PsiElement select(PsiClass psiCls, PsiMethod mtd) {
        PsiMethod highestMethod = min(
            COMP,
            min(COMP, psiCls.findMethodsByName("readExternal", false)),
            min(COMP, psiCls.findMethodsByName("writeExternal", false)),
            min(COMP, psiCls.findMethodsByName("hashCode", false)),
            min(COMP, psiCls.findMethodsByName("equals", false)),
            min(COMP, psiCls.findMethodsByName("toString", false)));

        if (highestMethod != null)
            return highestMethod.getPrevSibling();

        PsiElement rBrace = psiCls.getRBrace();

        if (rBrace != null)
            return rBrace.getPrevSibling();

        return null;
    }

}
