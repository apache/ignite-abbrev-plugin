// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection.abbrev

import com.intellij.codeInspection._
import com.intellij.psi.{PsiIdentifier, PsiNamedElement, PsiElement, PsiElementVisitor}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScValue, ScValueDeclaration, ScVariableDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter

import scala.collection.JavaConversions._
import org.gridgain.util.GridUtils
import com.intellij.openapi.project.Project
import com.intellij.refactoring.{RenameRefactoring, JavaRefactoringFactory}

/**
 * Abbreviation inspection for Scala language.
 *
 * @author @java.author
 * @version @java.version
 */
class GridScalaAbbreviationInspection extends LocalInspectionTool {
    /** Abbreviation rules. */
    val abbrRules = GridAbbreviationRules.getInstance

    /** Abbreviation exceptions. */
    val abbrExceptions = Set("value")

    override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean,
        ses: LocalInspectionToolSession): PsiElementVisitor = {
        new ScalaElementVisitor {
            override def visitValueDeclaration(v: ScValueDeclaration) =
                v.declaredElementsArray.foreach(checkShouldAbbreviate)

            override def visitValue(v: ScValue) =
                v.declaredElementsArray.foreach(checkShouldAbbreviate)

            override def visitParameter(p: ScParameter) =
                checkShouldAbbreviate(p, p.getNameIdentifier())

            override def visitVariableDefinition(v: ScVariableDefinition) =
                v.declaredElementsArray.foreach(checkShouldAbbreviate)

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private def checkShouldAbbreviate(elem: PsiNamedElement) =
                check0(GridUtils.camelCaseParts(elem.getName), elem)

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private def checkShouldAbbreviate(elem: PsiElement, id: PsiIdentifier) =
                check0(GridUtils.camelCaseParts(id.getText), elem)

            /**
             * Checks that all identifier parts are correctly abbreviated. Registers problem if
             * needed.
             *
             * @param parts Identifier parts.
             * @param elem Checked identifier element.
             */
            private def check0(parts: java.util.List[String], elem: PsiElement): Unit = {
                for (part <- parts) {
                    if (!abbrExceptions.contains(part) && abbrRules.getAbbreviation(part) != null) {
                        holder.registerProblem(elem, "Abbreviation should be used",
                            new RenameToFix(abbrRules.replaceWithAbbreviations(parts)))

                        return
                    }
                }
            }
        }
    }

    /**
     * Rename quick fix.
     *
     * @param name New name for the identifier.
     */
    private class RenameToFix(val name: String) extends java.lang.Object with LocalQuickFix {
        def getName: String = "Rename to " + name

        def getFamilyName: String = ""

        def applyFix(proj: Project, descr: ProblemDescriptor) = {
            val element: PsiElement = descr.getPsiElement

            val factory: JavaRefactoringFactory = JavaRefactoringFactory.getInstance(proj)

            val renRefactoring: RenameRefactoring = factory.createRename(element, name, false, false)

            renRefactoring.run()
        }
    }
}
