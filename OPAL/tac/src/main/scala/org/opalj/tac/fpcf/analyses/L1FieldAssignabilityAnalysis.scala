/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.DeclaredMethod
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.br.fpcf.properties.NonAssignable
import org.opalj.br.fpcf.properties.Assignable
import org.opalj.br.fpcf.properties.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.tac.fpcf.properties.cg.Callers

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 */
class L1FieldAssignabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    class State(
            val field:           Field,
            var tacDependees:    Map[Method, EOptionP[Method, TACAI]]                     = Map.empty,
            var callerDependees: Map[DeclaredMethod, EOptionP[DeclaredMethod, Callers]]   = Map.empty,
            var tacPCs:          Map[Method, PCs]                                         = Map.empty,
            var escapeDependees: Set[EOptionP[(Context, DefinitionSite), EscapeProperty]] = Set.empty
    )

    type V = DUVar[ValueInformation]

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit final val typeProvider: TypeProvider = project.get(TypeProviderKey)

    def doDetermineFieldAssignability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒ determineFieldAssignability(field)
            case _ ⇒
                val m = entity.getClass.getName+"is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineFieldAssignability(
        field: Field
    ): ProperPropertyComputationResult = {
        if (field.isFinal) {

            return Result(field, NonAssignable)
        }

        val thisType = field.classFile.thisType

        if (field.isPublic) {

            return Result(field, Assignable);
        }

        val initialClasses =
            if (field.isProtected || field.isPackagePrivate) {
                if (!closedPackages.isClosed(thisType.packageName)) {
                    return Result(field, Assignable);
                }
                project.classesPerPackage(thisType.packageName)
            } else {
                Set(field.classFile)
            }

        val classesHavingAccess: Iterator[ClassFile] =
            if (field.isProtected) {
                if (typeExtensibility(thisType).isYesOrUnknown) {
                    return Result(field, Assignable);
                }
                val subclassesIterator: Iterator[ClassFile] =
                    classHierarchy.allSubclassTypes(thisType, reflexive = false).
                        flatMap { ot ⇒
                            project.classFile(ot).filter(cf ⇒ !initialClasses.contains(cf))
                        }
                initialClasses.iterator ++ subclassesIterator
            } else {
                initialClasses.iterator
            }

        if (classesHavingAccess.exists(_.methods.exists(_.isNative))) {
            return Result(field, Assignable);
        }

        // We now (compared to the simple one) have to analyze the static initializer as
        // the static initializer can be used to initialize a private field of an instance
        // of the class after the reference to the class and (an indirect) reference to the
        // field has become available. Consider the following example:
        // class X implements Y{
        //
        //     private Object o;
        //
        //     public Object getO() { return o; }
        //
        //     private static X instance;
        //     static {
        //         instance = new X();
        //         Z.register(instance);
        //         // when we reach this point o is now (via getO) publically accessible and
        //         // X is properly initialized!
        //         o = new Object(); // o is mutated...
        //     }
        // }

        implicit val state: State = new State(field)

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            (taCode, callers) ← getTACAIAndCallers(method, pcs)
        } {
            if (methodUpdatesField(method, taCode, callers, pcs))
                return Result(field, Assignable);
        }

        returnResult()
    }

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        method:  Method,
        taCode:  TACode[TACMethodParameter, V],
        callers: Callers,
        pcs:     PCs
    )(
        implicit
        state: State
    ): Boolean = {
        val stmts = taCode.stmts
        for (pc ← pcs) {
            val index = taCode.properStmtIndexForPC(pc)
            if (index >= 0) {
                val stmtCandidate = stmts(index)
                if (stmtCandidate.pc == pc) {
                    stmtCandidate match {
                        case _: PutStatic[_] ⇒
                            if (!method.isStaticInitializer) {
                                return true;
                            };
                        case stmt: PutField[V] ⇒
                            val objRef = stmt.objRef.asVar
                            if ((!method.isConstructor ||
                                objRef.definedBy != SelfReferenceParameter) &&
                                !referenceHasNotEscaped(objRef, stmts, method, callers)) {
                                // note that here we assume real three address code (flat hierarchy)

                                // for instance fields it is okay if they are written in the
                                // constructor (w.r.t. the currently initialized object!)

                                // If the field that is written is not the one referred to by the
                                // self reference, it is not effectively final.

                                // However, a method (e.g. clone) may instantiate a new object and
                                // write the field as long as that new object did not yet escape.
                                return true;
                            }
                        case _ ⇒ throw new RuntimeException("unexpected field access");
                    }
                } else {
                    // nothing to do as the put field is dead
                }
            }
        }
        false
    }

    /**
     * Returns TACode and Callers for a method if available, registering dependencies as necessary.
     */
    def getTACAIAndCallers(
        method: Method,
        pcs:    PCs
    )(implicit state: State): Option[(TACode[TACMethodParameter, V], Callers)] = {
        val tacEOptP = propertyStore(method, TACAI.key)
        val tac = if (tacEOptP.hasUBP) tacEOptP.ub.tac else None
        state.tacDependees += method → tacEOptP
        state.tacPCs += method → pcs

        val declaredMethod: DeclaredMethod = declaredMethods(method)
        val callersEOptP = propertyStore(declaredMethod, Callers.key)
        val callers = if (callersEOptP.hasUBP) Some(callersEOptP.ub) else None
        state.callerDependees += declaredMethod -> callersEOptP

        if (tac.isDefined && callers.isDefined) {
            Some((tac.get, callers.get))
        } else None
    }

    def returnResult()(implicit state: State): ProperPropertyComputationResult = {
        if (state.tacDependees.valuesIterator.forall(_.isFinal) &&
            state.callerDependees.valuesIterator.forall(_.isFinal) && state.escapeDependees.isEmpty)
            Result(state.field, EffectivelyNonAssignable)
        else
            InterimResult(
                state.field,
                Assignable,
                NonAssignable,
                state.escapeDependees ++ state.tacDependees.valuesIterator.filter(_.isRefinable) ++
                    state.callerDependees.valuesIterator.filter(_.isRefinable),
                c
            )
    }

    def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        val isNonFinal = eps.pk match {
            case EscapeProperty.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[(Context, DefinitionSite), EscapeProperty]]
                state.escapeDependees = state.escapeDependees.filter(_.e ne eps.e)
                handleEscapeProperty(newEP)
            case TACAI.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = newEP.e
                val pcs = state.tacPCs(method)
                state.tacDependees += method → newEP
                val callersProperty = state.callerDependees(declaredMethods(method))
                if (callersProperty.hasUBP)
                    methodUpdatesField(method, newEP.ub.tac.get, callersProperty.ub, pcs)
                else false
            case Callers.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[DeclaredMethod, Callers]]
                val method = newEP.e.definedMethod
                val pcs = state.tacPCs(method)
                state.callerDependees += newEP.e → newEP
                val tacProperty = state.tacDependees(method)
                if (tacProperty.hasUBP && tacProperty.ub.tac.isDefined)
                    methodUpdatesField(method, tacProperty.ub.tac.get, newEP.ub, pcs)
                else false
        }

        if (isNonFinal)
            Result(state.field, Assignable);
        else
            returnResult()
    }

    /**
     * Checks whether the object reference of a PutField does not escape (except for being
     * returned).
     */
    def referenceHasNotEscaped(
        ref:     V,
        stmts:   Array[Stmt[V]],
        method:  Method,
        callers: Callers
    )(implicit state: State): Boolean = {
        val dm = declaredMethods(method)
        ref.definedBy.forall { defSite ⇒
            if (defSite < 0) false // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) true
                else if (!definition.expr.isNew) false
                else {
                    var hasEscaped = false
                    callers.forNewCalleeContexts(null, dm) { context ⇒
                        val entity = (context, definitionSites(method, definition.pc))
                        val escapeProperty = propertyStore(entity, EscapeProperty.key)
                        hasEscaped ||= handleEscapeProperty(escapeProperty)
                    }
                    !hasEscaped
                }
            }
        }
    }

    /**
     * Handles the influence of an escape property on the field mutability.
     * @return true if the object - on which a field write occurred - escapes, false otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleEscapeProperty(
        ep: EOptionP[(Context, DefinitionSite), EscapeProperty]
    )(implicit state: State): Boolean = ep match {
        case FinalP(NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
            false

        case FinalP(AtMost(_)) ⇒
            true

        case _: FinalEP[_, _] ⇒
            true // Escape state is worse than via return

        case InterimUBP(NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
            state.escapeDependees += ep
            false

        case InterimUBP(AtMost(_)) ⇒
            true

        case _: SomeInterimEP ⇒
            true // Escape state is worse than via return

        case _ ⇒
            state.escapeDependees += ep
            false
    }
}

sealed trait L1FieldAssignabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        TypeExtensibilityKey,
        ClosedPackagesKey,
        FieldAccessInformationKey,
        DefinitionSitesKey,
        TypeProviderKey
    )

    final override def uses: Set[PropertyBounds] = PropertyBounds.lubs(TACAI, EscapeProperty)

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldAssignability)

}

/**
 * Executor for the field mutability analysis.
 */
object EagerL1FieldAssignabilityAnalysis
    extends L1FieldAssignabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1FieldAssignabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldAssignability)
        analysis
    }
}

/**
 * Executor for the lazy field mutability analysis.
 */
object LazyL1FieldAssignabilityAnalysis
    extends L1FieldAssignabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1FieldAssignabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldAssignability.key, analysis.determineFieldAssignability
        )
        analysis
    }
}
