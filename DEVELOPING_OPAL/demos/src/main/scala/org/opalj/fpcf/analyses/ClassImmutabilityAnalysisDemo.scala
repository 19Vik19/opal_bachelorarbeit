/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.util.Calendar

import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.LazyL3FieldAssignabilityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import java.io.IOException
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis

/**
 * Runs the EagerL1ClassImmutabilityAnalysis as well as analyses needed for improving the result.
 *
 * @author Tobias Roth
 */
object ClassImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "determines the immutability of classes"

    override def description: String = "identifies classes that are immutable in a shallow or deep way"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(
                    LazyL2PurityAnalysis,
                    LazyL3FieldAssignabilityAnalysis,
                    LazyL0FieldImmutabilityAnalysis,
                    LazyTypeImmutabilityAnalysis,
                    EagerClassImmutabilityAnalysis,
                    LazyStaticDataUsageAnalysis,
                    LazyL0CompileTimeConstancyAnalysis,
                    LazyInterProceduralEscapeAnalysis,
                    LazyReturnValueFreshnessAnalysis,
                    LazyFieldLocalityAnalysis
                )
                ._1

            propertyStore.waitOnPhaseCompletion()

        } { t ⇒
            analysisTime = t.toSeconds
        }

        val allProjectClassTypes = project.allProjectClassFiles.map(_.thisType).toSet

        val groupedResults = propertyStore.entities(ClassImmutability.key).
            filter(x ⇒ allProjectClassTypes.contains(x.asInstanceOf[ObjectType])).toTraversable.groupBy(_.e)

        val order = (eps1: EPS[Entity, ClassImmutability], eps2: EPS[Entity, ClassImmutability]) ⇒
            eps1.e.toString < eps2.e.toString

        val mutableClasses =
            groupedResults(MutableClass).toSeq.sortWith(order)

        val shallowImmutableClasses =
            groupedResults(NonTransitivelyImmutableClass).toSeq.sortWith(order)

        val dependentImmutableClasses =
            groupedResults(DependentlyImmutableClass).toSeq.sortWith(order)

        val deepImmutables = groupedResults(TransitivelyImmutableClass).toSeq.sortWith(order)

        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val deepImmutableClassesInterfaces = deepImmutables
            .filter(eps ⇒ allInterfaces.contains(eps.asInstanceOf[ObjectType])).sortWith(order)

        val deepImmutableClasses =
            deepImmutables
                .filter(eps ⇒ !allInterfaces.contains(eps.asInstanceOf[ObjectType])).sortWith(order)

        val output =
            s"""
             | Mutable Classes:
             | ${mutableClasses.mkString(" | Mutable Class\n")}
             |
             | Shallow Immutable Classes:
             | ${shallowImmutableClasses.mkString(" | Shallow Immutable Class\n")}
             |
             | Dependent Immutable Classes:
             | ${dependentImmutableClasses.mkString(" | Dependent Immutable Class\n")}
             |
             | Deep Immutable Classes:
             | ${deepImmutableClasses.mkString(" | Deep Immutable Classes\n")}
             |
             | Deep Immutable Class Interfaces:
             | ${deepImmutableClassesInterfaces.mkString(" | Deep Immutable Classes Interfaces\n")}
             |
             |
             | Mutable Classes: ${mutableClasses.size}
             | Shallow Immutable Classes: ${shallowImmutableClasses.size}
             | Dependent Immutable Classes: ${dependentImmutableClasses.size}
             | Deep Immutable Classes: ${deepImmutableClasses.size}
             | Deep Immutable Classes Interfaces: ${deepImmutableClassesInterfaces.size}
             | Deep Immutables: ${deepImmutables.size}
             |
             | sum: ${
                mutableClasses.size + shallowImmutableClasses.size + dependentImmutableClasses.size +
                    deepImmutableClasses.size + deepImmutableClassesInterfaces.size
            }
             | analysis took : $analysisTime seconds
             |"""".stripMargin

        val file = new File(s"${Calendar.getInstance().formatted("dd_MM_yyyy_hh_mm_ss")}.txt")

        val bw = new BufferedWriter(new FileWriter(file))

        try {
            bw.write(output)
            bw.close()
        } catch {
            case e: IOException ⇒ println(
                s""" Could not write file: ${file.getName}
                   | ${e.getMessage}
                   |""".stripMargin
            )
        } finally {
            bw.close()
        }

        output
    }
}
