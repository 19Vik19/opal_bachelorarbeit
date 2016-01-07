/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package bugpicker
package core
package analysis

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{ ClassFile, Method }
import org.opalj.ai.Domain
import org.opalj.br.PC
import org.opalj.ai.collectPCWithOperands
import org.opalj.br.instructions.BinaryArithmeticInstruction
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.instructions.LNEG
import org.opalj.br.instructions.INEG
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.ShiftInstruction
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.ai.AIResult
import org.opalj.ai.domain.ConcreteIntegerValues
import org.opalj.ai.domain.ConcreteLongValues
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.br.instructions.IStoreInstruction
import org.opalj.br.instructions.LStoreInstruction
import org.opalj.ai.domain.TheCode
import org.opalj.ai.ValuesDomain

/**
 * Identifies computations that lead to the same result as the previous computation.
 *
 * @author Michael Eichberg
 */
object UselessReComputationsAnalysis {

    def apply(
        theProject: SomeProject, classFile: ClassFile, method: Method,
        result: AIResult { val domain: TheCode with ConcreteIntegerValues with ConcreteLongValues with ValuesDomain }
        ): Seq[StandardIssue] = {

        import result.domain.ConcreteIntegerValue
        import result.domain.ConcreteLongValue
        import result.domain

        if (!domain.code.localVariableTable.isDefined)

            // This analysis requires debug information to increase the likelihood
            // the we identify the correct local variable re-assignments. Otherwise
            // we are not able to distinguish the reuse of a "register variable"/
            // local variable for a new/different purpose or the situation where
            // the same variable is updated the second time using the same
            // value.
            return Seq.empty

        val operandsArray = result.operandsArray
        val localsArray = result.localsArray
        val code = domain.code

        val methodsWithValueReassignment =
            collectPCWithOperands(domain)(code, operandsArray) {
                case (
                    pc,
                    IStoreInstruction(index),
                    Seq(ConcreteIntegerValue(a), _*)
                    ) if localsArray(pc) != null &&
                    domain.intValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                    code.localVariable(pc, index).map(lv ⇒ lv.startPC < pc).getOrElse(false) ⇒

                    val lv = code.localVariable(pc, index).get

                    StandardIssue(
                        "UselessReevaluation",
                        theProject, classFile, None, Some(method), Some(pc),
                        Some(operandsArray(pc)),
                        Some(localsArray(pc)),
                        "useless (re-)assignment",
                        Some("(Re-)Assigned the same value ("+a+") to the same variable ("+lv.name+")."),
                        Set(IssueCategory.Smell, IssueCategory.Comprehensibility),
                        Set(IssueKind.ConstantComputation),
                        Seq.empty,
                        new Relevance(20)
                    )

                case (
                    pc,
                    LStoreInstruction(index),
                    Seq(ConcreteLongValue(a), _*)
                    ) if localsArray(pc) != null &&
                    domain.longValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                    code.localVariable(pc, index).map(lv ⇒ lv.startPC < pc).getOrElse(false) ⇒

                    val lv = code.localVariable(pc, index).get

                    StandardIssue(
                        "UselessReevaluation",
                        theProject, classFile, None, Some(method), Some(pc),
                        Some(operandsArray(pc)),
                        Some(localsArray(pc)),
                        "useless (re-)assignment",
                        Some("(Re-)Assigned the same value ("+a+") to the same variable ("+lv.name+")."),
                        Set(IssueCategory.Smell, IssueCategory.Comprehensibility),
                        Set(IssueKind.ConstantComputation),
                        Seq.empty,
                        new Relevance(20)
                    )
            }

        methodsWithValueReassignment
    }
}
