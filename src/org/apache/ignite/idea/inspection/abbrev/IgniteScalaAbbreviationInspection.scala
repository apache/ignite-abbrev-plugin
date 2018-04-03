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

package org.apache.ignite.idea.inspection.abbrev

import com.intellij.codeInspection._
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.{PsiElement, PsiElementVisitor, PsiIdentifier, PsiNamedElement}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScValue, ScValueDeclaration, ScVariableDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter

import scala.collection.JavaConversions._
import com.intellij.openapi.project.Project
import com.intellij.refactoring.{JavaRefactoringFactory, RenameRefactoring}
import org.apache.ignite.idea.util.IgniteUtils

/**
 * Abbreviation inspection for Scala language.
 */
class IgniteScalaAbbreviationInspection extends LocalInspectionTool {
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
                check0(IgniteUtils.camelCaseParts(elem.getName), elem)

            /**
             * Checks if given name contains a part that should be abbreviated and registers fix if needed.
             *
             * @param elem Element to check and rename.
             */
            private def checkShouldAbbreviate(elem: PsiElement, id: PsiIdentifier) =
                check0(IgniteUtils.camelCaseParts(id.getText), elem)

            /**
             * Checks that all identifier parts are correctly abbreviated. Registers problem if
             * needed.
             *
             * @param parts Identifier parts.
             * @param elem Checked identifier element.
             */
            private def check0(parts: java.util.List[String], elem: PsiElement): Unit = {
                for (part <- parts) {
                    val config: IgniteAbbreviationConfig = ServiceManager.getService(elem.getProject, classOf[IgniteAbbreviationConfig])

                    if (!abbrExceptions.contains(part) && config.getAbbreviation(part) != null) {
                        holder.registerProblem(elem, "Abbreviation should be used",
                            new RenameToFix(config.replaceWithAbbreviations(parts)))

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
