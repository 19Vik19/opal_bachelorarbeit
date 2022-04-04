/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1ClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.LazyL1TypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.LazyL3FieldAssignabilityAnalysis
import org.opalj.ai.domain.l2
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey

/**
 * Tests the field immutability analyses
 *
 * @author Tobias Roth
 */
class FieldImmutabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability")
    }

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ ⇒
            Set[Class[_ <: AnyRef]](classOf[l2.DefaultPerformInvocationsDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)
    }

    describe("no analysis is scheduled") {

        val as = executeAnalyses(Set.empty)

        as.propertyStore.shutdown()

        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }

    describe("the org.opalj.fpcf.analyses.L0FieldMutabilityAnalysis is executed") {

        val as = executeAnalyses(
            Set(
                EagerL0FieldImmutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis
            )
        )

        as.propertyStore.shutdown()

        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }

    describe("the org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis is executed") {

        val as = executeAnalyses(
            Set(
                EagerL1FieldImmutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyInterProceduralEscapeAnalysis
            )
        )
        as.propertyStore.shutdown()

        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }

    describe("the org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis is executed") {

        val as = executeAnalyses(
            Set(
                EagerL2FieldImmutabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                LazyInterProceduralEscapeAnalysis
            )
        )

        as.propertyStore.shutdown()

        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }

    describe("the org.opalj.fpcf.analyses.L0FieldImmutabilityAnalysis is executed") {
        val as = executeAnalyses(
            Set(
                LazyL3FieldAssignabilityAnalysis,
                LazyUnsoundPrematurelyReadFieldsAnalysis,
                LazyL2PurityAnalysis,
                EagerL0FieldImmutabilityAnalysis,
                LazyL1ClassImmutabilityAnalysis,
                LazyL1TypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                LazyVirtualCallAggregatingEscapeAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
    }
}
