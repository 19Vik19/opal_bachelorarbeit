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
package ai
package domain
package l1

import scala.reflect.ClassTag
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.MethodDescriptor

/**
 * Enables the tracing of concrete string values and can, e.g., be used to
 * resolve static "class.forName(...)" calls.
 *
 * @author Michael Eichberg
 */
trait StringValues
        extends ReferenceValues
        with JavaObjectConversion
        with MethodCallsDomain
        with PostEvaluationMemoryManagement {
    domain: CorrelationalDomainSupport with IntegerValuesDomain with TypedValuesFactory with Configuration with ClassHierarchy ⇒

    type DomainStringValue <: StringValue with DomainObjectValue
    val DomainStringValue: ClassTag[DomainStringValue]

    /**
     * @param value `null` if and only if the StringValue is not yet completely initialized!
     */
    protected class StringValue(
        origin: ValueOrigin,
        val value: String,
        t: Timestamp)
            extends SObjectValue(origin, No, true, ObjectType.String, t) {
        this: DomainStringValue ⇒

        override def doJoinWithNonNullValueWithSameOrigin(
            joinPC: PC,
            other: DomainSingleOriginReferenceValue): Update[DomainSingleOriginReferenceValue] = {

            other match {
                case DomainStringValue(that) ⇒
                    if (this.value == that.value) {
                        if (this.t == that.t)
                            NoUpdate
                        else
                            // Strings are immutable, hence, we can still keep the
                            // "reference" to the "value", but we still need to
                            // create a MetaInformationUpdate to make sure that potential
                            // future reference comparisons are reevaluated if necessary.
                            MetaInformationUpdate(that)
                    } else {
                        // We have to drop the concrete information...
                        // Given that the values are different we are no longer able to
                        // derive the concrete value.
                        val newValue = ObjectValue(origin, No, true, ObjectType.String, nextT())
                        StructuralUpdate(newValue)
                    }
                case _ ⇒
                    val result = super.doJoinWithNonNullValueWithSameOrigin(joinPC, other)
                    if (result.isStructuralUpdate) {
                        result
                    } else {
                        // This (string) value and the other value may have a corresponding
                        // abstract representation (w.r.t. the next abstraction level!)
                        // but we still need to drop the concrete information.
                        StructuralUpdate(result.value.update())
                    }
            }
        }

        override def abstractsOver(other: DomainValue): Boolean = {
            if (this eq other)
                return true

            other match {
                case that: StringValue ⇒ that.value == this.value
                case _                 ⇒ false
            }
        }

        override def adapt(target: TargetDomain, vo: ValueOrigin): target.DomainValue =
            // The following method is provided by `CoreDomain` and, hence,
            // all possible target domains are automatically supported.
            target.StringValue(vo, this.value)

        override def equals(other: Any): Boolean = {
            other match {
                case that: StringValue ⇒ that.value == this.value && super.equals(other)
                case _                 ⇒ false
            }
        }

        override protected def canEqual(other: SObjectValue): Boolean =
            other.isInstanceOf[StringValue]

        override def hashCode: Int = super.hashCode * 41 + value.hashCode()

        override def toString(): String = {
            if (value eq null)
                s"""String(origin=$origin;value=<initialization incomplete>;t=$t)"""
            else
                s"""String(origin=$origin;value="$value";t=$t)"""
        }

    }

    object StringValue {
        def unapply(value: StringValue): Option[String] = Some(value.value)
    }

    abstract override def toJavaObject(pc: PC, value: DomainValue): Option[Object] = {
        value match {
            case StringValue(value) ⇒ Some(value)
            case _                  ⇒ super.toJavaObject(pc, value)
        }
    }

    abstract override def toDomainValue(pc: PC, value: Object): DomainReferenceValue = {
        value match {
            case s: String ⇒ StringValue(pc, s)
            case _         ⇒ super.toDomainValue(pc, value)
        }
    }

    abstract override def NewObject(
        origin: ValueOrigin,
        objectType: ObjectType): DomainObjectValue = {

        if (objectType eq ObjectType.String)
            StringValue(origin, null)
        else
            super.NewObject(origin, objectType)
    }

    abstract override def invokespecial(
        pc: PC,
        declaringClass: ObjectType,
        name: String,
        methodDescriptor: MethodDescriptor,
        operands: Operands): MethodCallResult = {

        // In general the compiler creates a sequence comparable to the following
        // (1) new String
        // (2) dup // duplicate the reference to directly use it after initialization
        // (3) invokespecial <init>(...)
        // (4) // do something with the string; e.g., store it in a local variable

        if ((declaringClass eq ObjectType.String) && name == "<init>") {

            val newStringKindValue = operands(methodDescriptor.parametersCount)
            if (newStringKindValue.isInstanceOf[StringValue]) {
                // we need to filter inter-constructor calls (i.e., we don't
                // want to analyze calls between the constructors of the class
                // java.lang.String
                val newStringValue = newStringKindValue.asInstanceOf[StringValue]

                if (methodDescriptor == MethodDescriptor.NoArgsAndReturnVoid) {
                    updateAfterEvaluation(
                        newStringValue,
                        StringValue(newStringValue.origin, "", newStringValue.t))
                    return ComputationWithSideEffectOnly;

                } else if (methodDescriptor == StringValues.ConstructorWithString) {
                    operands.head match {
                        // Let's test if we know the parameter
                        case StringValue(s) ⇒
                            updateAfterEvaluation(
                                newStringValue,
                                StringValue(newStringValue.origin, s, newStringValue.t))
                            return ComputationWithSideEffectOnly

                        case _ ⇒ /* we can do nothing special */
                    }
                }
            }
        }
        super.invokespecial(pc, declaringClass, name, methodDescriptor, operands)
    }

    final override def StringValue(origin: ValueOrigin, value: String): DomainObjectValue =
        StringValue(origin, value, nextT())

    def StringValue(origin: ValueOrigin, value: String, t: Timestamp): DomainStringValue
}

object StringValues {

    val ConstructorWithString = MethodDescriptor(ObjectType.String, VoidType)

}
