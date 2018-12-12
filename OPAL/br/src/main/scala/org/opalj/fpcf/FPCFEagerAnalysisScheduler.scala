/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.analyses.SomeProject

/**
 * Factory for FPCF analyses which should be directly started/scheduled.
 *
 * @author Michael Eichberg
 */
trait FPCFEagerAnalysisScheduler extends AbstractFPCFAnalysisScheduler {

    final override def computationType: ComputationType = EagerComputation

    final override def derivesLazily: Option[PropertyBounds] = None

    final override def schedule(ps: PropertyStore, i: InitializationData): Unit = {
        start(ps.context(classOf[SomeProject]), ps, i)
    }

    /**
     * Starts the analysis for the given `project`. This method is typically implicitly
     * called by the [[FPCFAnalysesManager]].
     */
    def start(p: SomeProject, i: InitializationData): FPCFAnalysis = {
        start(p, p.get(PropertyStoreKey), i)
    }

    /**
     * Called when a schedule is executed and when this analysis shall register itself
     * with the property store using [[PropertyStore.scheduleEagerComputationForEntity]] or
     * a variant thereof.
     * This method is typically implicitly called by the [[org.opalj.fpcf.FPCFAnalysesManager]].
     *
     * @note This analysis must not call `registerTriggeredComputation` or
     *       `registerLazyPropertyComputation`.
     */
    def start(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis
}

/**
 * A simple eager analysis scheduler for those analyses that do not perform special initialization
 * steps.
 */
// TODO Rename => Simple...
trait BasicFPCFEagerAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    final override type InitializationData = Null
    def init(p: SomeProject, ps: PropertyStore): Null = null
    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}
    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
