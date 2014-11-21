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

import scala.collection.immutable.SortedSet

import org.opalj.util.{ Answer, Yes, No, Unknown }
import org.opalj.br._

/**
 * This domain enables the tracking of long values using sets. The cardinality of
 * the set can be configured to facilitate different needs with regard to the
 * desired precision. Often, a very small cardinality (e.g., 2 or 8) may be
 * completely sufficient and a large cardinality does not significantly add to the
 * overall precision.
 *
 * @author Michael Eichberg
 * @author David Becker
 */
trait LongSetValues extends LongValuesDomain with ConcreteLongValues {
    domain: IntegerRangeValuesFactory with CorrelationalDomain with Configuration with VMLevelExceptionsFactory ⇒

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Determines the maximum number of values captured by an Long set.
     *
     * In many cases a rather (4-16) small number is completely sufficient to
     * capture typically variability.
     */
    protected def maxCardinalityOfLongSets: Int = 8

    /**
     * Abstracts over all values with computational type `long`.
     */
    sealed trait LongValue extends Value with IsLongValue { this: DomainValue ⇒

        final def computationalType: ComputationalType = ComputationalTypeLong

    }

    /**
     * Represents an (unknown) long value.
     *
     * Models the top value of this domain's lattice.
     */
    trait ALongValue extends LongValue { this: DomainValue ⇒ }

    /**
     * Represents a set of long values.
     */
    abstract class LongSet extends LongValue { this: DomainValue ⇒

        val values: SortedSet[Long]

    }

    /**
     * Creates a new LongSet value containing the given value.
     */
    def LongSet(value: Long): DomainValue = LongSet(SortedSet(value))

    /**
     * Creates a new LongSet value using the given set.
     */
    def LongSet(values: SortedSet[Long]): DomainValue

    /**
     * Extractor for `LongSet` values.
     */
    object LongSet {
        def unapply(v: LongSet): Option[SortedSet[Long]] = Some(v.values)
    }

    // -----------------------------------------------------------------------------------
    //
    // COMPUTATIONS RELATED TO LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // QUESTIONS ABOUT VALUES
    //

    @inline final override def longValue[T](
        value: DomainValue)(
            f: Long ⇒ T)(
                orElse: ⇒ T): T =
        value match {
            case LongSet(values) if values.size == 1 ⇒ f(values.head)
            case _                                   ⇒ orElse
        }

    @inline final override def longValueOption(value: DomainValue): Option[Long] =
        value match {
            case LongSet(values) if values.size == 1 ⇒ Some(values.head)
            case _                                   ⇒ None
        }

    @inline protected final def withLongValuesOrElse[T](
        value1: DomainValue, value2: DomainValue)(
            f: (Long, Long) ⇒ T)(
                orElse: ⇒ T): T = {
        longValue(value1) {
            v1 ⇒ longValue(value2) { v2 ⇒ f(v1, v2) } { orElse }
        } {
            orElse
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // UNARY EXPRESSIONS
    //
    /*override*/ def lneg(pc: PC, value: DomainValue) =
        value match {
            case LongSet(values) ⇒ LongSet(values.map(-_))
            case _               ⇒ LongValue(vo = pc)
        }

    //
    // RELATIONAL OPERATORS
    //

    /*override*/ def lcmp(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        left match {
            case (LongSet(leftValues)) ⇒
                right match {
                    case (LongSet(rightValues)) ⇒
                        val lb =
                            if (leftValues.head < rightValues.last)
                                -1
                            else if (leftValues.head == rightValues.last)
                                0
                            else
                                1
                        val ub =
                            if (leftValues.last > rightValues.head)
                                1
                            else if (leftValues.last == rightValues.head)
                                0
                            else
                                -1
                        IntegerRange(pc, lb, ub)
                    case _ ⇒
                        if (leftValues.size == 1) {
                            if (leftValues.head == Long.MinValue)
                                IntegerRange(pc, -1, 0)
                            else if (leftValues.head == Long.MaxValue)
                                IntegerRange(pc, 0, 1)
                            else
                                IntegerRange(pc, -1, 1)
                        } else
                            IntegerRange(pc, -1, 1)
                }
            case _ ⇒
                right match {
                    case (LongSet(rightValues)) ⇒
                        if (rightValues.size == 1) {
                            if (rightValues.head == Long.MinValue)
                                IntegerRange(pc, 0, 1)
                            else if (rightValues.head == Long.MaxValue)
                                IntegerRange(pc, -1, 0)
                            else
                                IntegerRange(pc, -1, 1)
                        } else
                            IntegerRange(pc, -1, 1)
                    case _ ⇒
                        IntegerRange(pc, -1, 1)
                }
        }
    }

    //
    // BINARY EXPRESSIONS
    //

    /*override*/ def ladd(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                val results =
                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
                        leftValue + rightValue
                    }
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(vo = pc)
            case _ ⇒
                LongValue(vo = pc)
        }
    }

    /*override*/ def lsub(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        (left, right) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue - rightValue
                }
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(vo = pc)
            case _ ⇒
                LongValue(vo = pc)
        }
    }

    /*override*/ def lmul(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        left match {
            case (LongSet(leftValues)) ⇒
                if (leftValues.size == 1 && leftValues.head == 0)
                    left
                else if (leftValues.size == 1 && leftValues.head == 1)
                    right
                else right match {
                    case (LongSet(rightValues)) ⇒
                        val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                            leftValue * rightValue
                        }
                        if (results.size <= maxCardinalityOfLongSets)
                            LongSet(results)
                        else
                            LongValue(vo = pc)
                    case _ ⇒
                        LongValue(vo = pc)
                }
            case _ ⇒
                right match {
                    case (LongSet(rightValues)) ⇒
                        if (rightValues.size == 1 && rightValues.head == 0)
                            right
                        else if (rightValues.size == 1 && rightValues.head == 1)
                            left
                        else
                            LongValue(vo = pc)
                    case _ ⇒
                        LongValue(vo = pc)
                }
        }
    }

    protected[this] def createLongValueOrArithmeticException(
        pc: PC,
        exception: Boolean,
        results: SortedSet[Long]): LongValueOrArithmeticException = {
        if (results.size > 0) {
            if (results.size <= maxCardinalityOfLongSets) {
                if (exception)
                    ComputedValueOrException(LongSet(results), ArithmeticException(pc))
                else
                    ComputedValue(LongSet(results))
            } else {
                if (exception)
                    ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
            }
        } else {
            if (exception)
                ThrowsException(ArithmeticException(pc))
            else
                throw new DomainException("no result and no exception")
        }
    }

    /*override*/ def ldiv(
        pc: PC,
        numerator: DomainValue,
        denominator: DomainValue): LongValueOrArithmeticException = {
        (numerator, denominator) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                var results: SortedSet[Long] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue ← leftValues; rightValue ← rightValues) {
                    if (rightValue == 0L)
                        exception = true
                    else
                        results += (leftValue / rightValue)
                }
                createLongValueOrArithmeticException(pc, exception, results)

            case (_, LongSet(rightValues)) ⇒
                if (rightValues contains (0)) {
                    if (rightValues.size == 1)
                        ThrowsException(ArithmeticException(pc))
                    else
                        ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                } else
                    ComputedValue(LongValue(vo = pc))

            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
        }
    }

    /*override*/ def lrem(
        pc: PC,
        left: DomainValue,
        right: DomainValue): LongValueOrArithmeticException = {

        (left, right) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                var results: SortedSet[Long] = SortedSet.empty
                var exception: Boolean = false
                for (leftValue ← leftValues; rightValue ← rightValues) {
                    if (rightValue == 0)
                        exception = true
                    else
                        results += (leftValue % rightValue)
                }
                createLongValueOrArithmeticException(pc, exception, results)

            case (_, LongSet(rightValues)) ⇒
                if (rightValues contains (0)) {
                    if (rightValues.size == 1)
                        ThrowsException(ArithmeticException(pc))
                    else
                        ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                } else
                    ComputedValue(LongValue(vo = pc))

            case _ ⇒
                if (throwArithmeticExceptions)
                    ComputedValueOrException(LongValue(vo = pc), ArithmeticException(pc))
                else
                    ComputedValue(LongValue(vo = pc))
        }
    }

    /*override*/ def land(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        left match {
            case (LongSet(leftValues)) ⇒
                if (leftValues.size == 1 && leftValues.head == -1)
                    right
                else if (leftValues.size == 1 && leftValues.head == 0)
                    left
                else right match {
                    case (LongSet(rightValues)) ⇒
                        val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                            leftValue & rightValue
                        }
                        if (results.size <= maxCardinalityOfLongSets)
                            LongSet(results)
                        else
                            LongValue(vo = pc)
                    case _ ⇒
                        LongValue(vo = pc)
                }
            case _ ⇒
                right match {
                    case (LongSet(rightValues)) ⇒
                        if (rightValues.size == 1 && rightValues.head == -1)
                            left
                        else if (rightValues.size == 1 && rightValues.head == 0)
                            right
                        else
                            LongValue(vo = pc)
                    case _ ⇒
                        LongValue(vo = pc)
                }
        }
    }

    /*override*/ def lor(pc: PC, left: DomainValue, right: DomainValue): DomainValue = {
        left match {
            case (LongSet(leftValues)) ⇒
                if (leftValues.size == 1 && leftValues.head == -1)
                    left
                else if (leftValues.size == 1 && leftValues.head == 0)
                    right
                else right match {
                    case (LongSet(rightValues)) ⇒
                        val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                            leftValue | rightValue
                        }
                        if (results.size <= maxCardinalityOfLongSets)
                            LongSet(results)
                        else
                            LongValue(vo = pc)
                    case _ ⇒
                        LongValue(vo = pc)
                }
            case _ ⇒
                right match {
                    case (LongSet(rightValues)) ⇒
                        if (rightValues.size == 1 && rightValues.head == -1)
                            right
                        else if (rightValues.size == 1 && rightValues.head == 0)
                            left
                        else
                            LongValue(vo = pc)
                    case _ ⇒
                        LongValue(vo = pc)
                }
        }
    }

    /*override*/ def lshl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue << rightValue
                }
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(vo = pc)

            case _ ⇒
                LongValue(vo = pc)
        }
    }

    /*override*/ def lshr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                val results = for (leftValue ← leftValues; rightValue ← rightValues) yield {
                    leftValue >> rightValue
                }
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(vo = pc)

            case _ ⇒
                LongValue(vo = pc)
        }
    }

    /*override*/ def lushr(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                val results =
                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
                        leftValue >>> rightValue
                    }
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(vo = pc)

            case _ ⇒
                LongValue(vo = pc)
        }
    }

    /*override*/ def lxor(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue = {
        (value1, value2) match {
            case (LongSet(leftValues), LongSet(rightValues)) ⇒
                val results =
                    for (leftValue ← leftValues; rightValue ← rightValues) yield {
                        leftValue ^ rightValue
                    }
                if (results.size <= maxCardinalityOfLongSets)
                    LongSet(results)
                else
                    LongValue(vo = pc)

            case _ ⇒
                LongValue(vo = pc)
        }
    }

}

