/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.fp
package analyses

import scala.language.postfixOps
import java.net.URL
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.ClassFile
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ARRAYLENGTH
import org.opalj.br.instructions.LALOAD
import org.opalj.br.instructions.IALOAD
import org.opalj.br.instructions.CALOAD
import org.opalj.br.instructions.BALOAD
import org.opalj.br.instructions.BASTORE
import org.opalj.br.instructions.CASTORE
import org.opalj.br.instructions.IASTORE
import org.opalj.br.instructions.LASTORE
import org.opalj.br.instructions.SASTORE
import org.opalj.br.instructions.SALOAD
import org.opalj.br.instructions.DALOAD
import org.opalj.br.instructions.FALOAD
import org.opalj.br.instructions.FASTORE
import org.opalj.br.instructions.DASTORE
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.ClassFile
import org.opalj.fp.SourceElementsPropertyStoreKey

sealed trait Mutability extends Property {
    final val key = Mutability.Key // All instances have to share the SAME key!
}

private object Mutability {
    final val Key = PropertyKey.create("Mutability", NonFinal)
}

case object EffectivelyFinal extends Mutability

case object NonFinal extends Mutability

object MutablityAnalysis {

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineMutabilityOfNonFinalPrivateStaticFields(
        entity: AnyRef)(
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {
        if (!entity.isInstanceOf[ClassFile])
            return Impossible;

        val classFile = entity.asInstanceOf[ClassFile]
        val thisType = classFile.thisType

        val psnfFields = classFile.fields.filter(f ⇒ f.isPrivate && f.isStatic && !f.isFinal).toSet
        var effectivelyFinalFields = psnfFields
        if (psnfFields.isEmpty)
            return Empty;

        val concreteStaticMethods = classFile.methods filter { m ⇒
            m.isStatic && !m.isStaticInitializer && !m.isNative
        }
        concreteStaticMethods foreach { m ⇒
            m.body.get foreach { (pc, instruction) ⇒
                instruction match {
                    case PUTSTATIC(`thisType`, fieldName, fieldType) ⇒
                        // we don't need to lookup the field in the
                        // the class hierarchy since we are only concerned about private
                        // fiels so far... so we don't have to do a
                        // classHierarchy.resolveFieldReference(thisType, fieldName, fieldType, project).get
                        classFile.findField(fieldName) foreach { f ⇒ effectivelyFinalFields -= f }
                    case _ ⇒ /*Nothing to do*/
                }
            }
        }

        /*DEBUGGING*/ if (psnfFields.nonEmpty)
            /*DEBUGGING*/ println(psnfFields.map(f ⇒ f.toJava(thisType) + effectivelyFinalFields.contains(f)).toSeq.sorted.mkString("\n"))

        Result(
            psnfFields map { f ⇒
                if (effectivelyFinalFields.contains(f))
                    (f, EffectivelyFinal)
                else
                    (f, NonFinal)
            }
        )
    }

    def analyze(implicit project: Project[URL]): Unit = {
        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
        projectStore <<= determineMutabilityOfNonFinalPrivateStaticFields
    }
}
