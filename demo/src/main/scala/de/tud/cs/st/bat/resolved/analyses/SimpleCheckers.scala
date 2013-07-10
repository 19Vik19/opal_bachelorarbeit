/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st
package bat.resolved
package analyses

import util.debug.{ Counting, PerformanceEvaluation }
import reader.Java7Framework
import de.tud.cs.st.bat.resolved.BooleanType
import de.tud.cs.st.bat.resolved.IntegerType
import de.tud.cs.st.bat.resolved.Method
import de.tud.cs.st.bat.resolved.MethodDescriptor
import de.tud.cs.st.bat.resolved.ObjectType
import de.tud.cs.st.util.debug.nsToSecs
import de.tud.cs.st.bat.resolved.analyses.ReportableAnalysisResult

/**
  * Demonstrates how to implement two very simple checkers using BAT.
  *
  * @author Michael Eichberg
  */
object SimpleCheckers extends AnalysisExecutor {

    def description: String = "Finds violations of the equals-hashCode contract and co-variant equals methods."

    def copyright: String = "(c) 2011-2013 Michael Eichberg (eichberg@informatik.tu-darmstadt.de)"

    def analyze(project: Project): ReportableAnalysisResult = {

        val CountingPerformanceEvaluator = new PerformanceEvaluation with Counting
        import CountingPerformanceEvaluator._

        var problemCount = 0

        time('Overall) {
            for (
                classFile ← project.classFiles
            ) {
                time('EqHcChecker) {
                    var definesEqualsMethod = false
                    var definesHashCodeMethod = false
                    for (method ← classFile.methods) method match {
                        case Method(_, "equals", MethodDescriptor(Seq(ObjectType.Object), BooleanType), _) ⇒
                            definesEqualsMethod = true
                        case Method(_, "hashCode", MethodDescriptor(Seq(), IntegerType), _) ⇒
                            definesHashCodeMethod = true
                        case _ ⇒
                    }

                    if (definesEqualsMethod != definesHashCodeMethod) {
                        problemCount += 1
                        println(
                            "\tThe class: "+
                                classFile.thisClass.className+
                                " does not satisfy java.lang.Object's equals-hashCode contract.")
                    }
                }
                time('CovEqChecker) {
                    var definesEqualsMethod = false
                    var definesCovariantEqualsMethod = false
                    for (method ← classFile.methods) method match {
                        case Method(_, "equals", MethodDescriptor(Seq(ot), BooleanType), _) ⇒
                            if (ot == ObjectType.Object)
                                definesEqualsMethod = true
                            else
                                definesCovariantEqualsMethod = true
                        case _ ⇒
                    }
                    if (definesCovariantEqualsMethod && !definesEqualsMethod) {
                        problemCount += 1
                        println(
                            "\tThe class: "+
                                classFile.thisClass.className+
                                " defines a covariant equals method, but does not also define the standard equals method.")
                    }
                }
            }
        }
        import de.tud.cs.st.util.debug.nsToSecs
        println("Equals-HashCode Checker: "+nsToSecs(getTime('EqHcChecker)))
        println("Covariant Equals Checker: "+nsToSecs(getTime('CovEqChecker)))
        println("Analyzed all classes in: "+nsToSecs(getTime('Overall)))
        println("Number of identified violations: "+problemCount)

        null // FIXME we have to report something meaningful!
    }
}