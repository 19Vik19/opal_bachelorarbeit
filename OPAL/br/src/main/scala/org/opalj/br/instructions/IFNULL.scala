/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package br
package instructions

/**
 * Branch if reference is null.
 *
 * @author Michael Eichberg
 */
trait IFNULLLike extends IFXNullInstructionLike {

    final def opcode: Opcode = IFNULL.opcode

    final def mnemonic: String = "ifnull"

    final def operator: String = "== null"

    final def condition: RelationalOperator = RelationalOperators.EQ

}

case class IFNULL(branchoffset: Int) extends IFXNullInstruction with IFNULLLike

/**
 * Defines constants and factory methods.
 *
 * @author Malte Limmeroth
 */
object IFNULL {

    final val opcode = 198

    /**
     * Creates LabeledIFNULL instructions with a `Symbol` as the branch target.
     */
    def apply(branchTarget: Symbol): LabeledIFNULL = LabeledIFNULL(branchTarget)

}

case class LabeledIFNULL(
        branchTarget: Symbol
) extends LabeledSimpleConditionalBranchInstruction with IFNULLLike {

    override def resolveJumpTargets(currentIndex: PC, branchoffsets: Map[Symbol, PC]): IFNULL = {
        IFNULL(branchoffsets(branchTarget) - currentIndex)
    }
}
