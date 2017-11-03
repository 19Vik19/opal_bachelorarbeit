/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package fpcf
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.AnalysisModeConfigFactory
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.TypeEscapeProperty
import org.opalj.fpcf.properties.GlobalType
import org.opalj.fpcf.properties.PackageLocalType
import org.opalj.fpcf.properties.MaybePackageLocalType
import org.opalj.fpcf.properties.MaybeEscapeInCallee
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger.error
import org.opalj.tac.DefaultTACAIKey
import org.opalj.util.PerformanceEvaluation.time
//TODO bedeutung..... wie oft aus anderem package überschriebene methode aufgerufen
class TypeEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {

    /**
     * Determines the purity of the given method. The given method must have a body!
     */
    def determineTypeEscape(cf: ClassFile): PropertyComputationResult = {
        //var dependees = Set.empty[EOptionP[Entity, EscapeProperty]]

        if (cf.isAbstract || !cf.isPackageVisible) {
            ImmediateResult(cf, GlobalType)
            // TODO abstract types are not interesting
        } else {
            val constructorsNotAccessible = cf.constructors.forall(cs ⇒ cs.isPackagePrivate || cs.isPrivate)
            if (constructorsNotAccessible) {
                val as = propertyStore.context[AllocationSites]
                val allocations = as.apply(cf.thisType)
                var maybeLocal = false
                for (allocation ← allocations) {
                    val escapeState = propertyStore(allocation, EscapeProperty.key)
                    escapeState match {
                        case EP(_, NoEscape | EscapeInCallee) ⇒
                        case EP(_, MaybeNoEscape | MaybeEscapeInCallee) ⇒
                            maybeLocal = true
                        // /dependees += escapeState
                        case EP(_, _) ⇒ return ImmediateResult(cf, GlobalType)
                        case epk ⇒
                            throw new RuntimeException("Escape information should be present")
                        //dependees += epk
                    }
                }

                if (maybeLocal)
                    ImmediateResult(cf, MaybePackageLocalType)
                else
                    ImmediateResult(cf, PackageLocalType)
                //check escape of allocation
            } else {
                ImmediateResult(cf, GlobalType)
                // TODO the type escapes
            }
        }
    }
}

object TypeEscapeAnalysis extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(TypeEscapeProperty) //TODO use own property

    override def usedProperties: Set[PropertyKind] = Set(EscapeProperty)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        //val analysesManager = project.get(FPCFAnalysesManagerKey)
        //analysesManager.run(SimpleEscapeAnalysis)
        val analysis = new TypeEscapeAnalysis(project)
        propertyStore.scheduleForEntities(project.allClassFiles)(analysis.determineTypeEscape)
        analysis
    }

    def main(args: Array[String]): Unit = {

        val opaConfig = AnalysisModeConfigFactory.createConfig(AnalysisModes.OPA)
        val project = Project(
            org.opalj.bytecode.JRELibraryFolder,
            GlobalLogContext,
            opaConfig.withFallback(org.opalj.ai.BaseConfig)
        )

        implicit val logContext = project.logContext
        time {
            val tacai = project.get(DefaultTACAIKey)
            val exceptions = project.parForeachMethodWithBody() { mi ⇒ tacai(mi.method) }
            if (exceptions.nonEmpty) {
                exceptions.foreach(println)
                return ;
            }
        } { t ⇒ println(s"computing the 3-address code took ${t.toSeconds}") }

        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val propertyStore = project.get(PropertyStoreKey)
        //propertyStore.debug = true
        time {
            SimpleEscapeAnalysis.start(project)
            propertyStore.waitOnPropertyComputationCompletion(useFallbacksForIncomputableProperties = false)
        } { t ⇒ println(s"escape analysis took ${t.toSeconds}") }

        time {
            TypeEscapeAnalysis.start(project)
            propertyStore.waitOnPropertyComputationCompletion(useFallbacksForIncomputableProperties = false)
        } { t ⇒ println(s"type escape analysis took ${t.toSeconds}") }

        val globalType = propertyStore.entities(GlobalType)
        val localType = propertyStore.entities(PackageLocalType)
        val maybeLocalType = propertyStore.entities(MaybePackageLocalType)
        var counter1 = 0
        var counter2 = 0

        time {
            for {
                local ← localType
                cf = local.asInstanceOf[ClassFile]
                mOfClass ← cf.methods
                //cg = project.get(CHACallGraphKey).callGraph
            } {
                val errors = project.parForeachMethod() { m ⇒
                    if (!m.isStatic && !m.isPrivate && !m.isInitializer) {
                        val overriddenMethods = project.overriddenBy(m)
                        if (overriddenMethods.contains(mOfClass)) {
                            if (m.isPublic && m.classFile.isPublic) {
                                counter1 += 1
                            }
                            val errors = project.parForeachMethodWithBody() { mi ⇒
                                for (inst ← mi.method.body.get.instructions) {
                                    inst match {
                                        case INVOKEVIRTUAL(t, n, d) ⇒
                                            if ((n eq m.name) && (d eq m.descriptor) &&
                                                project.classHierarchy.isSubtypeOf(m.classFile.thisType, t).isYes &&
                                                mi.method.classFile.thisType.packageName != cf.thisType.packageName) {
                                                counter2 += 1
                                            }
                                        case _ ⇒
                                    }
                                }
                            }
                            errors.foreach { e ⇒ error("progress", "iterating over methods failed", e) }
                        }
                    }
                }
                errors.foreach { e ⇒ error("progress", "iterating over methods failed", e) }

            }
        } { t ⇒ println(s"generating numbers took ${t.toSeconds}") }

        println(counter1)
        println(counter2)

        println(s"# of local types: ${localType.size}")
        println(s"# of global types: ${globalType.size}")
        println(s"# of maybe local types: ${maybeLocalType.size}")
    }
}
