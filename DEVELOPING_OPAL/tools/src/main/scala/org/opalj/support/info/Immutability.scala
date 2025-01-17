/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.immutable.SortedSet

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import org.opalj.ai.domain
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.Project.JavaClassFileReader
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.CompileTimeConstancy
import org.opalj.br.fpcf.properties.CompileTimeConstantField
import org.opalj.br.fpcf.properties.CompileTimeVaryingField
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaHeapObject
import org.opalj.br.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameter
import org.opalj.br.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameterAndReturn
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.EscapeViaStaticField
import org.opalj.br.fpcf.properties.GlobalEscape
import org.opalj.br.fpcf.properties.StaticDataUsage
import org.opalj.br.fpcf.properties.UsesConstantDataOnly
import org.opalj.br.fpcf.properties.UsesNoStaticData
import org.opalj.br.fpcf.properties.UsesVaryingData
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.ForNameClasses
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.LoadedClasses
import org.opalj.br.fpcf.properties.cg.NoCallees
import org.opalj.br.fpcf.properties.cg.NoCalleesDueToNotReachableMethod
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.properties.cg.NoForNameClasses
import org.opalj.br.fpcf.properties.cg.NoInstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoLoadedClasses
import org.opalj.br.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.properties.cg.OnlyVMCallersAndWithUnknownContext
import org.opalj.br.fpcf.properties.cg.OnlyVMLevelCallers
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoMethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableType
import org.opalj.br.fpcf.properties.immutability.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.MutableType
import org.opalj.br.fpcf.properties.immutability.NonAssignable
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableType
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.log.LogContext
import org.opalj.tac.cg.CallGraph
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.cg.XTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.LazyL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.properties.NoTACAI
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Determines the assignability of fields and the immutability of fields, classes and types and provides several
 * setting options for evaluation.
 *
 * @author Tobias Roth
 */
object Immutability {

    sealed trait Analyses
    case object Assignability extends Analyses
    case object Fields extends Analyses
    case object Classes extends Analyses
    case object Types extends Analyses
    case object All extends Analyses

    def evaluate(
        cp:                                File,
        analysis:                          Analyses,
        numThreads:                        Int,
        projectDir:                        Option[String],
        libDir:                            Option[String],
        resultsFolder:                     Path,
        withoutJDK:                        Boolean,
        isLibrary:                         Boolean,
        level:                             Int,
        withoutConsiderLazyInitialization: Boolean,
        configurationName:                 Option[String],
        times:                             Int,
        callgraphKey:                      CallGraphKey
    ): BasicReport = {

        val classFiles = projectDir match {
            case Some(dir) => JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      => JavaClassFileReader().ClassFiles(cp)
        }

        val libFiles = libDir match {
            case Some(dir) => JavaClassFileReader().ClassFiles(cp.toPath.resolve(dir).toFile)
            case None      => Iterable.empty
        }

        val JDKFiles =
            if (withoutJDK) Iterable.empty
            else JavaClassFileReader().ClassFiles(JRELibraryFolder)

        implicit var config: Config =
            if (isLibrary)
                ConfigFactory.load("LibraryProject.conf")
            else
                ConfigFactory.load("CommandLineProject.conf")

        config = config.withValue(
            "org.opalj.fpcf.analyses.L3FieldAssignabilityAnalysis.considerLazyInitialization",
            ConfigValueFactory.fromAnyRef(!withoutConsiderLazyInitialization)
        )

        var projectTime: Seconds = Seconds.None
        var analysisTime: Seconds = Seconds.None
        val callGraphTime: Seconds = Seconds.None

        val project = time {
            Project(
                classFiles,
                libFiles ++ JDKFiles,
                libraryClassFilesAreInterfacesOnly = false,
                Iterable.empty
            )
        } { t => projectTime = t.toSeconds }

        val allProjectClassTypes = project.allProjectClassFiles.iterator.map(_.thisType).toSet

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.iterator.flatMap { _.fields }.toSet

        val dependencies: List[FPCFAnalysisScheduler] =
            List(
                EagerFieldAccessInformationAnalysis,
                LazyL2FieldAssignabilityAnalysis,
                LazyFieldImmutabilityAnalysis,
                LazyClassImmutabilityAnalysis,
                LazyTypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazySimpleEscapeAnalysis
            )

        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
            if (level == 0) {
                Set[Class[_ <: AnyRef]](classOf[domain.l0.PrimitiveTACAIDomain])
            } else if (level == 1)
                Set[Class[_ <: AnyRef]](classOf[domain.l1.DefaultDomainWithCFGAndDefUse[URL]])
            else if (level == 2)
                Set[Class[_ <: AnyRef]](classOf[domain.l2.DefaultPerformInvocationsDomainWithCFG[URL]])
            else
                throw new Exception(s"The level $level does not exist")
        }

        project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) => {
                implicit val lg: LogContext = project.logContext
                if (numThreads == 0) {
                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                } else {
//                    org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                    org.opalj.fpcf.par.PKECPropertyStore.MaxThreads = numThreads
                    org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
            }
        )

        val propertyStore = project.get(PropertyStoreKey)
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        callgraphKey.requirements(project)

        val allDependencies = callgraphKey.allCallGraphAnalyses(project) ++ dependencies

        time {
            analysesManager.runAll(
                allDependencies,
                {
                    (css: List[ComputationSpecification[FPCFAnalysis]]) =>
                        analysis match {
                            case Assignability =>
                                if (css.contains(LazyL2FieldAssignabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f =>
                                        propertyStore.force(f, FieldAssignability.key)
                                    )
                            case Fields =>
                                if (css.contains(LazyFieldImmutabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f =>
                                        propertyStore.force(f, FieldImmutability.key)
                                    )
                            case Classes =>
                                if (css.contains(LazyClassImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => propertyStore.force(c, ClassImmutability.key))
                            case Types =>
                                if (css.contains(LazyTypeImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => propertyStore.force(c, TypeImmutability.key))
                            case All =>
                                if (css.contains(LazyL2FieldAssignabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f => {
                                        import org.opalj.br.fpcf.properties.immutability
                                        propertyStore.force(f, immutability.FieldAssignability.key)
                                    })
                                if (css.contains(LazyFieldImmutabilityAnalysis))
                                    allFieldsInProjectClassFiles.foreach(f =>
                                        propertyStore.force(f, FieldImmutability.key)
                                    )
                                if (css.contains(LazyClassImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => {
                                        import org.opalj.br.fpcf.properties.immutability
                                        propertyStore.force(c, immutability.ClassImmutability.key)
                                    })
                                if (css.contains(LazyTypeImmutabilityAnalysis))
                                    allProjectClassTypes.foreach(c => propertyStore.force(c, TypeImmutability.key))
                        }
                }
            )
        } { t => analysisTime = t.toSeconds }

        println("TEST START")

        val cg = new CallGraph()(propertyStore, project.get(TypeIteratorKey))

        println("NoTACAI: " + propertyStore.finalEntities(NoTACAI).toSeq.size)
        println("TACAI: " + propertyStore.entities(TACAI.key).toSeq.size)
        println("")
        println("CallGraph Callers: " + propertyStore.entities(Callers.key).toSeq.size)
        println("CallGraph edges: " + cg.numEdges)
        println("CallGraph reachable methods: " + cg.reachableMethods().size)
        println("NoCallers: " + propertyStore.finalEntities(NoCallers).toSeq.size)
        println(
            "OnlyCallersWithUnknownContext: " + propertyStore.finalEntities(OnlyCallersWithUnknownContext).toSeq.size
        )
        println("OnlyVMLevelCallers: " + propertyStore.finalEntities(OnlyVMLevelCallers).toSeq.size)
        println("OnlyVMCallersAndWithUnknownContext: " + propertyStore.finalEntities(
            OnlyVMCallersAndWithUnknownContext
        ).toSeq.size)
        println("")
        println("CallGraph Callees: " + propertyStore.entities(Callees.key).toSeq.size)
        println("NoCallees: " + propertyStore.finalEntities(NoCallees).toSeq.size)
        println("NoCalleesDueToNotReachableMethod: " + propertyStore.finalEntities(
            NoCalleesDueToNotReachableMethod
        ).toSeq.size)
        println("")
        println("LoadedClasses: " + propertyStore.entities(LoadedClasses.key).toSeq.size)
        println("NoLoadedClasses: " + propertyStore.finalEntities(NoLoadedClasses).toSeq.size)
        println("")
        println("ForNameClasses: " + propertyStore.entities(ForNameClasses.key).toSeq.size)
        println("NoForNameClasses: " + propertyStore.finalEntities(NoForNameClasses).toSeq.size)
        println("")
        println("InstantiatedTypes: " + propertyStore.entities(InstantiatedTypes.key).toSeq.size)
        println("NoInstantiatedTypes: " + propertyStore.finalEntities(NoInstantiatedTypes).toSeq.size)
        println("")
        println("")
        println("MethodFieldReadAccessInformation: " + propertyStore.entities(
            MethodFieldReadAccessInformation.key
        ).toSeq.size)
        println("NoMethodFieldReadAccessInformation: " + propertyStore.finalEntities(
            NoMethodFieldReadAccessInformation
        ).toSeq.size)
        println("")
        println("MethodFieldReadAccessInformation: " + propertyStore.entities(
            MethodFieldReadAccessInformation.key
        ).toSeq.size)
        println("NoMethodFieldReadAccessInformation: " + propertyStore.finalEntities(
            NoMethodFieldReadAccessInformation
        ).toSeq.size)
        println("")
        println("FieldReadAccessInformation: " + propertyStore.entities(FieldReadAccessInformation.key).toSeq.size)
        println("NoFieldReadAccessInformation: " + propertyStore.finalEntities(NoFieldReadAccessInformation).toSeq.size)
        println("")
        println("FieldWriteAccessInformation: " + propertyStore.entities(FieldWriteAccessInformation.key).toSeq.size)
        println(
            "NoFieldWriteAccessInformation: " + propertyStore.finalEntities(NoFieldWriteAccessInformation).toSeq.size
        )
        println("")
        println("")
        println("EscapeProperty: " + propertyStore.entities(EscapeProperty.key).toSeq.size)
        println("EscapeInCallee: " + propertyStore.finalEntities(EscapeInCallee).toSeq.size)
        println("EscapeViaParameter: " + propertyStore.finalEntities(EscapeViaParameter).toSeq.size)
        println("EscapeViaReturn: " + propertyStore.finalEntities(EscapeViaReturn).toSeq.size)
        println("EscapeViaAbnormalReturn: " + propertyStore.finalEntities(EscapeViaAbnormalReturn).toSeq.size)
        println("EscapeViaParameterAndReturn: " + propertyStore.finalEntities(EscapeViaParameterAndReturn).toSeq.size)
        println("EscapeViaParameterAndAbnormalReturn: " + propertyStore.finalEntities(
            EscapeViaParameterAndAbnormalReturn
        ).toSeq.size)
        println("EscapeViaNormalAndAbnormalReturn: " + propertyStore.finalEntities(
            EscapeViaNormalAndAbnormalReturn
        ).toSeq.size)
        println("EscapeViaParameterAndNormalAndAbnormalReturn: " + propertyStore.finalEntities(
            EscapeViaParameterAndNormalAndAbnormalReturn
        ).toSeq.size)
        println("GlobalEscape: " + propertyStore.finalEntities(GlobalEscape).toSeq.size)
        println("EscapeViaHeapObject: " + propertyStore.finalEntities(EscapeViaHeapObject).toSeq.size)
        println("EscapeViaStaticField: " + propertyStore.finalEntities(EscapeViaStaticField).toSeq.size)
        println("")
        println("")
        println("FieldAssignability: " + propertyStore.entities(FieldAssignability.key).toSeq.size)
        println("NonAssignable: " + propertyStore.finalEntities(NonAssignable).toSeq.size)
        println("EffectivelyNonAssignable: " + propertyStore.finalEntities(EffectivelyNonAssignable).toSeq.size)
        println("LazilyInitialized: " + propertyStore.finalEntities(LazilyInitialized).toSeq.size)
        println("UnsafelyLazilyInitialized: " + propertyStore.finalEntities(UnsafelyLazilyInitialized).toSeq.size)
        println("Assignable: " + propertyStore.finalEntities(Assignable).toSeq.size)
        println("")
        println("CompileTimeConstancy: " + propertyStore.entities(CompileTimeConstancy.key).toSeq.size)
        println("CompileTimeConstantField: " + propertyStore.finalEntities(CompileTimeConstantField).toSeq.size)
        println("CompileTimeVaryingField: " + propertyStore.finalEntities(CompileTimeVaryingField).toSeq.size)
        println("")
        println("StaticDataUsage: " + propertyStore.entities(StaticDataUsage.key).toSeq.size)
        println("UsesNoStaticData: " + propertyStore.finalEntities(UsesNoStaticData).toSeq.size)
        println("UsesConstantDataOnly: " + propertyStore.finalEntities(UsesConstantDataOnly).toSeq.size)
        println("UsesVaryingData: " + propertyStore.finalEntities(UsesVaryingData).toSeq.size)
        println("")
        println("")
        println("FieldImmutability: " + propertyStore.entities(FieldImmutability.key).toSeq.size)
        println("TransitivelyImmutableField: " + propertyStore.finalEntities(TransitivelyImmutableField).toSeq.size)
        println(
            "NonTransitivelyImmutableField: " + propertyStore.finalEntities(NonTransitivelyImmutableField).toSeq.size
        )
        println("MutableField: " + propertyStore.finalEntities(MutableField).toSeq.size)
        println("")
        println("ClassImmutability: " + propertyStore.entities(ClassImmutability.key).toSeq.size)
        println("TransitivelyImmutableClass: " + propertyStore.finalEntities(TransitivelyImmutableClass).toSeq.size)
        println(
            "NonTransitivelyImmutableClass: " + propertyStore.finalEntities(NonTransitivelyImmutableClass).toSeq.size
        )
        println("MutableClass: " + propertyStore.finalEntities(MutableClass).toSeq.size)
        println("")
        println("TypeImmutability: " + propertyStore.entities(TypeImmutability.key).toSeq.size)
        println("TransitivelyImmutableType: " + propertyStore.finalEntities(TransitivelyImmutableType).toSeq.size)
        println("NonTransitivelyImmutableType: " + propertyStore.finalEntities(NonTransitivelyImmutableType).toSeq.size)
        println("MutableType: " + propertyStore.finalEntities(MutableType).toSeq.size)
        println("")

        println("TEST ENDE")

        val stringBuilderResults: StringBuilder = new StringBuilder()

        val fieldAssignabilityGroupedResults = propertyStore
            .entities(FieldAssignability.key)
            .filter(field => allFieldsInProjectClassFiles.contains(field.e.asInstanceOf[Field]))
            .iterator.to(Iterable)
            .groupBy(_.asFinal.p)

        def unpackFieldEPS(eps: EPS[Entity, OrderedProperty]): String = {
            if (!eps.e.isInstanceOf[Field])
                throw new Exception(s"${eps.e} is not a field")
            val field = eps.e.asInstanceOf[Field]
            val fieldName = field.name
            val packageName = field.classFile.thisType.packageName.replace("/", ".")
            val className = field.classFile.thisType.simpleName
            packageName + "." + className + "." + fieldName
        }

        val assignableFields =
            fieldAssignabilityGroupedResults
                .getOrElse(Assignable, Iterator.empty)
                .toSeq
                .map(unpackFieldEPS)
                .sortWith(_ < _)

        val unsafelyLazilyInitializedFields =
            fieldAssignabilityGroupedResults
                .getOrElse(UnsafelyLazilyInitialized, Iterator.empty)
                .toSeq
                .map(unpackFieldEPS)
                .sortWith(_ < _)

        val lazilyInitializedFields =
            fieldAssignabilityGroupedResults
                .getOrElse(LazilyInitialized, Iterator.empty)
                .toSeq
                .map(unpackFieldEPS)
                .sortWith(_ < _)

        val effectivelyNonAssignableFields = fieldAssignabilityGroupedResults
            .getOrElse(EffectivelyNonAssignable, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)
        val nonAssignableFields = fieldAssignabilityGroupedResults
            .getOrElse(NonAssignable, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        if (analysis == All || analysis == Assignability) {
            stringBuilderResults.append(
                s"""
                | Assignable Fields:
                | ${assignableFields.map(_ + " | Assignable Field ").mkString("\n")}
                |
                | Lazy Initialized Not Thread Safe Field:
                | ${
                        unsafelyLazilyInitializedFields
                            .map(_ + " | Lazy Initialized Not Thread Safe Field")
                            .mkString("\n")
                    }
                |
                | Lazy Initialized Thread Safe Field:
                | ${
                        lazilyInitializedFields
                            .map(_ + " | Lazy Initialized Thread Safe Field")
                            .mkString("\n")
                    }
                |
                |
                | effectively non assignable Fields:
                |                | ${
                        effectivelyNonAssignableFields
                            .map(_ + " | effectively non assignable ")
                            .mkString("\n")
                    }

                | non assignable Fields:
                | ${
                        nonAssignableFields
                            .map(_ + " | non assignable")
                            .mkString("\n")
                    }
                |
                |""".stripMargin
            )
        }

        val fieldGroupedResults = propertyStore
            .entities(FieldImmutability.key)
            .filter(eps => allFieldsInProjectClassFiles.contains(eps.e.asInstanceOf[Field]))
            .iterator.to(Iterable)
            .groupBy {
                _.asFinal.p match {
                    case DependentlyImmutableField(_) => DependentlyImmutableField(SortedSet.empty)
                    case default                      => default
                }
            }

        val mutableFields = fieldGroupedResults
            .getOrElse(MutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        val nonTransitivelyImmutableFields = fieldGroupedResults
            .getOrElse(NonTransitivelyImmutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        val dependentlyImmutableFields = fieldGroupedResults // .collect { case (DependentlyImmutableField(_), rhs) => rhs }
            .getOrElse(DependentlyImmutableField(SortedSet.empty), Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        val transitivelyImmutableFields = fieldGroupedResults
            .getOrElse(TransitivelyImmutableField, Iterator.empty)
            .toSeq
            .map(unpackFieldEPS)
            .sortWith(_ < _)

        if (analysis == All || analysis == Fields) {
            stringBuilderResults.append(
                s"""
                | Mutable Fields:
                | ${mutableFields.map(_ + " | Mutable Field ").mkString("\n")}
                |
                | Non Transitively Immutable Fields:
                | ${nonTransitivelyImmutableFields.map(_ + " | Non Transitively Immutable Field ").mkString("\n")}
                |
                | Dependently Immutable Fields:
                | ${
                        dependentlyImmutableFields
                            .map(_ + " | Dependently Immutable Field ")
                            .mkString("\n")
                    }
                |
                | Transitively Immutable Fields:
                | ${transitivelyImmutableFields.map(_ + " | Transitively Immutable Field ").mkString("\n")}
                |
                |""".stripMargin
            )
        }

        val classGroupedResults = propertyStore
            .entities(ClassImmutability.key)
            .filter(eps => allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType]))
            .iterator.to(Iterable)
            .groupBy {
                _.asFinal.p match {
                    case DependentlyImmutableClass(_) => DependentlyImmutableClass(SortedSet.empty)
                    case default                      => default
                }
            }

        def unpackClass(eps: EPS[Entity, OrderedProperty]): String = {
            val classFile = eps.e.asInstanceOf[ObjectType]
            val className = classFile.simpleName
            s"${classFile.packageName.replace("/", ".")}.$className"
        }

        val mutableClasses =
            classGroupedResults
                .getOrElse(MutableClass, Iterator.empty)
                .toSeq
                .map(unpackClass)
                .sortWith(_ < _)

        val nonTransitivelyImmutableClasses =
            classGroupedResults
                .getOrElse(NonTransitivelyImmutableClass, Iterator.empty)
                .toSeq
                .map(unpackClass)
                .sortWith(_ < _)

        val dependentlyImmutableClasses =
            classGroupedResults
                .getOrElse(DependentlyImmutableClass(SortedSet.empty), Iterator.empty)
                .toSeq
                .map(unpackClass)
                .sortWith(_ < _)

        val transitivelyImmutables = classGroupedResults.getOrElse(TransitivelyImmutableClass, Iterator.empty)

        val allInterfaces =
            project.allProjectClassFiles.filter(_.isInterfaceDeclaration).map(_.thisType).toSet

        val transitivelyImmutableClassesInterfaces = transitivelyImmutables
            .filter(eps => allInterfaces.contains(eps.e.asInstanceOf[ObjectType]))
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val transitivelyImmutableClasses = transitivelyImmutables
            .filter(eps => !allInterfaces.contains(eps.e.asInstanceOf[ObjectType]))
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        if (analysis == All || analysis == Classes) {
            stringBuilderResults.append(
                s"""
                | Mutable Classes:
                | ${mutableClasses.map(_ + " | Mutable Class ").mkString("\n")}
                |
                | Non Transitively Immutable Classes:
                | ${nonTransitivelyImmutableClasses.map(_ + " | Non Transitively Immutable Class ").mkString("\n")}
                |
                | Dependently Immutable Classes:
                | ${
                        dependentlyImmutableClasses
                            .map(_ + " | Dependently Immutable Class ")
                            .mkString("\n")
                    }
                |
                | Transitively Immutable Classes:
                | ${transitivelyImmutableClasses.map(_ + " | Transitively Immutable Classes ").mkString("\n")}
                |
                | Transitively Immutable Interfaces:
                | ${
                        transitivelyImmutableClassesInterfaces
                            .map(_ + " | Transitively Immutable Interfaces ")
                            .mkString("\n")
                    }
                |""".stripMargin
            )
        }

        val typeGroupedResults = propertyStore
            .entities(TypeImmutability.key)
            .filter(eps => allProjectClassTypes.contains(eps.e.asInstanceOf[ObjectType]))
            .iterator.to(Iterable)
            .groupBy {
                _.asFinal.p match {
                    case DependentlyImmutableType(_) => DependentlyImmutableType(SortedSet.empty)
                    case default                     => default
                }
            }
        ()

        val mutableTypes = typeGroupedResults
            .getOrElse(MutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val nonTransitivelyImmutableTypes = typeGroupedResults
            .getOrElse(NonTransitivelyImmutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val dependentlyImmutableTypes = typeGroupedResults
            .getOrElse(DependentlyImmutableType(SortedSet.empty), Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        val transitivelyImmutableTypes = typeGroupedResults
            .getOrElse(TransitivelyImmutableType, Iterator.empty)
            .toSeq
            .map(unpackClass)
            .sortWith(_ < _)

        if (analysis == All || analysis == Types) {
            stringBuilderResults.append(
                s"""
                | Mutable Types:
                | ${mutableTypes.map(_ + " | Mutable Type ").mkString("\n")}
                |
                | Non-Transitively Immutable Types:
                | ${nonTransitivelyImmutableTypes.map(_ + " | Non-Transitively Immutable Types ").mkString("\n")}
                |
                | Dependently Immutable Types:
                | ${dependentlyImmutableTypes.map(_ + " | Dependently Immutable Types ").mkString("\n")}
                |
                | Transitively Immutable Types:
                | ${transitivelyImmutableTypes.map(_ + " | Transitively Immutable Types ").mkString("\n")}
                |""".stripMargin
            )
        }

        val stringBuilderNumber: StringBuilder = new StringBuilder

        if (analysis == Assignability || analysis == All) {
            stringBuilderNumber.append(
                s"""
                | Assignable Fields: ${assignableFields.size}
                | Unsafely Lazily Initialized  Fields: ${unsafelyLazilyInitializedFields.size}
                | lazily Initialized Fields: ${lazilyInitializedFields.size}
                | Effectively Non Assignable Fields: ${effectivelyNonAssignableFields.size}
                | Non Assignable Fields: ${nonAssignableFields.size}
                | Fields: ${allFieldsInProjectClassFiles.size}
                |""".stripMargin
            )
        }

        if (analysis == Fields || analysis == All) {
            stringBuilderNumber.append(
                s"""
                | Mutable Fields: ${mutableFields.size}
                | Non Transitively Immutable Fields: ${nonTransitivelyImmutableFields.size}
                | Dependently Immutable Fields: ${dependentlyImmutableFields.size}
                | Transitively Immutable Fields: ${transitivelyImmutableFields.size}
                | Fields: ${allFieldsInProjectClassFiles.size}
                | Fields with primitive Types / java.lang.String: ${
                        allFieldsInProjectClassFiles.count(field =>
                            !field.fieldType.isReferenceType || field.fieldType == ObjectType.String
                        )
                    }
                |""".stripMargin
            )
        }

        if (analysis == Classes || analysis == All) {
            stringBuilderNumber.append(
                s"""
                | Mutable Classes: ${mutableClasses.size}
                | Non Transitively Immutable Classes: ${nonTransitivelyImmutableClasses.size}
                | Dependently Immutable Classes: ${dependentlyImmutableClasses.size}
                | Transitively Immutable Classes: ${transitivelyImmutableClasses.size}
                | Classes: ${allProjectClassTypes.size - transitivelyImmutableClassesInterfaces.size}
                |
                | Transitively Immutable Interfaces: ${transitivelyImmutableClassesInterfaces.size}
                |
                |""".stripMargin
            )
        }

        if (analysis == Types || analysis == All)
            stringBuilderNumber.append(
                s"""
                | Mutable Types: ${mutableTypes.size}
                | Non Transitively Immutable Types: ${nonTransitivelyImmutableTypes.size}
                | Dependently Immutable Types: ${dependentlyImmutableTypes.size}
                | Transitively immutable Types: ${transitivelyImmutableTypes.size}
                | Types: ${allProjectClassTypes.size}
                |""".stripMargin
            )

        val totalTime = projectTime + callGraphTime + analysisTime

        stringBuilderNumber.append(
            s"""
            | running ${analysis.toString} analysis
            | took:
            |   $totalTime seconds total time
            |   $projectTime seconds project time
            |   $callGraphTime seconds callgraph time
            |   $analysisTime seconds analysis time
            |""".stripMargin
        )

        println(
            s"""
            |
            | ${stringBuilderNumber.toString()}
            |
            | time results:
            |
            | $numThreads Threads :: took $analysisTime seconds analysis time
            |
            | results folder: $resultsFolder
            |
            | CofigurationName: $configurationName
            |
            |  level: $level
            |
            |propertyStore: ${propertyStore.getClass}
            |
            |""".stripMargin
        )

        val fileNameExtension = {
            {
                if (withoutConsiderLazyInitialization) {
                    println("withoutConsiderLazyInitialization")
                    "_withoutConsiderLazyInitialization"
                } else ""
            } + {
                if (isLibrary) {
                    println("is Library")
                    "_isLibrary_"
                } else ""
            } + {
                if (numThreads == 0) ""
                else s"_${numThreads}threads"
            } + s"_l$level"
        }

        if (resultsFolder != null) {

            val calender = Calendar.getInstance()
            calender.add(Calendar.ALL_STYLES, 1)
            val date = calender.getTime
            val simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss")

            val file = new File(
                s"$resultsFolder/${configurationName.getOrElse("")}_${times}_" +
                    s"${analysis.toString}_${simpleDateFormat.format(date)}_$fileNameExtension.txt"
            )

            println(s"filepath: ${file.getAbsolutePath}")
            val bw = new BufferedWriter(new FileWriter(file))

            try {
                bw.write(
                    s""" ${stringBuilderResults.toString()}
                    |
                    | ${stringBuilderNumber.toString()}
                    |
                    | level: $level
                    |
                    | jdk folder: $JRELibraryFolder
                    |
                    | callGraph $CallGraphKey
                    |
                    |""".stripMargin
                )

                bw.close()
            } catch {
                case _: IOException => println(s"could not write file: ${file.getName}")
            } finally {
                bw.close()
            }

        }

        println(s"propertyStore: ${propertyStore.getClass.toString}")

        println(s"jdk folder: $JRELibraryFolder")

        BasicReport(stringBuilderNumber.toString())
    }

    def main(args: Array[String]): Unit = {
        import org.opalj.tac.cg.AllocationSiteBasedPointsToCallGraphKey
        import org.opalj.tac.cg.CHACallGraphKey
        import org.opalj.tac.cg.RTACallGraphKey

        def usage: String = {
            s"""
            | Usage: Immutability
            | -cp <JAR file/Folder containing class files> OR -JDK
            | -projectDir <project directory>
            | -libDir <library directory>
            | -isLibrary
            | -adHocCHA
            | [-JDK] (running with the JDK)
            | [-analysis <imm analysis that should be executed: FieldAssignability, Fields, Classes, Types, All>]
            | [-threads <threads that should be max used>]
            | [-resultFolder <folder for the result files>]
            | [-closedWorld] (uses closed world assumption, i.e. no class can be extended)
            | [-noJDK] (running without the JDK)
            | [-callGraph <CHA|RTA|XTA|PointsTo> (Default: RTA)
            | [-level] <0|1|2> (domain level  Default: 2)
            | [-withoutConsiderGenericity]
            | [-withoutConsiderLazyInitialization]
            | [-times <1...n>] (times of execution. n is a natural number)
            |""".stripMargin
        }

        var i = 0
        var cp: File = null
        var resultFolder: Path = null
        var numThreads = 0
        // var timeEvaluation: Boolean = false
        // var threadEvaluation: Boolean = false
        var projectDir: Option[String] = None
        var libDir: Option[String] = None
        var withoutJDK: Boolean = false
        var closedWorldAssumption = false
        var isLibrary = false
        var callGraphName: Option[String] = None
        var level = 2
        var withoutConsiderLazyInitialization = false
        var times = 1
        var multiProjects = false
        var configurationName: Option[String] = None

        def readNextArg(): String = {
            i = i + 1
            if (i < args.length) {
                args(i)
            } else {
                println(usage)
                throw new IllegalArgumentException(s"missing argument: ${args(i - 1)}")
            }
        }

        var analysis: Analyses = All

        while (i < args.length) {
            args(i) match {

                case "-analysis" =>
                    val result = readNextArg()
                    if (result == "All")
                        analysis = All
                    else if (result == "FieldAssignability")
                        analysis = Assignability
                    else if (result == "Fields")
                        analysis = Fields
                    else if (result == "Classes")
                        analysis = Classes
                    else if (result == "Types")
                        analysis = Types
                    else {
                        println(usage)
                        throw new IllegalArgumentException(s"unknown parameter: $result")
                    }
                case "-threads"                           => numThreads = readNextArg().toInt
                case "-cp"                                => cp = new File(readNextArg())
                case "-resultFolder"                      => resultFolder = FileSystems.getDefault.getPath(readNextArg())
                case "-projectDir"                        => projectDir = Some(readNextArg())
                case "-libDir"                            => libDir = Some(readNextArg())
                case "-closedWorld"                       => closedWorldAssumption = true
                case "-isLibrary"                         => isLibrary = true
                case "-noJDK"                             => withoutJDK = true
                case "-callGraph"                         => callGraphName = Some(readNextArg())
                case "-level"                             => level = Integer.parseInt(readNextArg())
                case "-times"                             => times = Integer.parseInt(readNextArg())
                case "-withoutConsiderLazyInitialization" => withoutConsiderLazyInitialization = true
                case "-multi"                             => multiProjects = true
                case "-analysisName"                      => configurationName = Some(readNextArg())
                case "-JDK" =>
                    cp = JRELibraryFolder
                    withoutJDK = true

                case unknown =>
                    println(usage)
                    throw new IllegalArgumentException(s"unknown parameter: $unknown")
            }
            i += 1
        }
        if (!(0 <= level && level <= 2))
            throw new Exception(s"not a domain level: $level")

        val callGraphKey = callGraphName match {
            case Some("CHA")        => CHACallGraphKey
            case Some("PointsTo")   => AllocationSiteBasedPointsToCallGraphKey
            case Some("RTA") | None => RTACallGraphKey
            case Some("XTA")        => XTACallGraphKey
            case Some(a) =>
                Console.println(s"unknown call graph analysis: $a")
                Console.println(usage)
                return;
        }

        var nIndex = 1
        while (nIndex <= times) {
            if (multiProjects) {
                for (subp <- cp.listFiles()) {
                    evaluate(
                        subp,
                        analysis,
                        numThreads,
                        projectDir,
                        libDir,
                        resultFolder,
                        withoutJDK,
                        isLibrary || (subp eq JRELibraryFolder),
                        level,
                        withoutConsiderLazyInitialization,
                        configurationName,
                        nIndex,
                        callGraphKey
                    )
                }
            } else {
                evaluate(
                    cp,
                    analysis,
                    numThreads,
                    projectDir,
                    libDir,
                    resultFolder,
                    withoutJDK,
                    isLibrary,
                    level,
                    withoutConsiderLazyInitialization,
                    configurationName,
                    nIndex,
                    callGraphKey
                )
            }
            nIndex = nIndex + 1
        }
    }
}
