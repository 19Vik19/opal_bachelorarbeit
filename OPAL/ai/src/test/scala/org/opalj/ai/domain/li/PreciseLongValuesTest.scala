/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package li

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 * This test(suite) checks if PreciseLongValues is working fine
 *
 * @author Riadh Chtara
 */
@RunWith(classOf[JUnitRunner])
class PreciseLongValuesTest extends FlatSpec with Matchers {

    object LongValuesTestDomain
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultReferenceValuesBinding
            with li.DefaultPreciseLongValues
            with li.DefaultPreciseIntegerValues
            with l0.TypeLevelPrimitiveValuesConversions
            with PredefinedClassHierarchy
            with DefaultHandlingOfMethodResults
            with RecordLastReturnedValues
            with IgnoreSynchronization {

        override def maxUpdatesForIntegerValues: Long = 5

    }

    import LongValuesTestDomain._
    //
    // TESTS
    //

    behavior of "the precise long values domain"

    //
    // QUESTION'S ABOUT VALUES
    //

    it should ("be able to check if two long values are equal") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 7L)
        val v3 = LongValue(-1, 8L)
        v1.equals(v2) should be(true)
        v1.equals(v3) should be(false)
    }

    it should ("be able to check if a long value is less than another value") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 8L)
        longIsLessThan(v1, v2) should be(Yes)
        longIsLessThan(v1, v1) should be(No)
        longIsLessThan(v2, v1) should be(No)
    }

    it should ("be able to check if a long value is less than or equal another value") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 8L)
        longIsLessThanOrEqualTo(v1, v2) should be(Answer(true))
        longIsLessThanOrEqualTo(v1, v1) should be(Answer(true))
        longIsLessThanOrEqualTo(v2, v1) should be(Answer(false))
    }

    //
    // RELATIONAL OPERATORS
    //

    it should ("be able to compare two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 8L)
        lcmp(-1, v1, v2) should be(IntegerValue(-1, -1))
        lcmp(-1, v1, v1) should be(IntegerValue(-1, 0))
        lcmp(-1, v2, v1) should be(IntegerValue(-1, 1))
    }

    //
    // UNARY EXPRESSIONS
    //

    it should ("be able to the calculate the neg of a long value") in {
        val v1 = LongValue(-1, 7L)
        val v2 = lneg(-1, lneg(-1, v1))
        v1.equals(v2) should be(true)
    }

    //
    // BINARY EXPRESSIONS
    //

    it should ("be able to the calculate the result of the add of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        ladd(-1, v1, v2) should be(LongValue(-1, 7L + 6L))
    }

    it should ("be able to the calculate the result of the and of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        land(-1, v1, v2) should be(LongValue(-1, 7L & 6L))
    }

    it should ("be able to the calculate the result of the div of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        ldiv(-1, v1, v2) should be(ComputedValue(LongValue(-1, 7L / 6L)))
    }

    it should ("be able to the calculate the result of the mul of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        lmul(-1, v1, v2) should be(LongValue(-1, 7L * 6L))
    }

    it should ("be able to the calculate the result of the or of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        lor(-1, v1, v2) should be(LongValue(-1, 7L | 6L))
    }

    it should ("be able to the calculate the result of the rem of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        lrem(-1, v1, v2) should be(ComputedValue(LongValue(-1, 7L % 6L)))
    }

    it should ("be able to the calculate the result of the sub of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        lsub(-1, v1, v2) should be(LongValue(-1, 7L - 6L))
    }

    it should ("be able to the calculate the result of the shl of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L) // FIXME ???
        lshl(-1, v1, v2) should be(LongValue(-1, 7L << 6))
    }

    it should ("be able to the calculate the result of the or of shr long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L) // FIXME ???
        lshr(-1, v1, v2) should be(LongValue(-1, 7L >> 6))
    }

    it should ("be able to the calculate the result of the ushr of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L) // FIXME ???
        lushr(-1, v1, v2) should be(LongValue(-1, 7L >>> 6))
    }

    it should ("be able to the calculate the result of the xor of two long values") in {
        val v1 = LongValue(-1, 7L)
        val v2 = LongValue(-1, 6L)
        lxor(-1, v1, v2) should be(LongValue(-1, 7L ^ 6L))
    }

    //
    // EXTRACTING INFORMATIONS FROM OPERATIONS ON SPECIFIC
    // (BUT UNKNOWN) LongValues
    //

    it should ("be able to the calculate the result of the and of a specific (but unknown) LongValue and 0") in {
        val v1 = ALongValue()
        val v2 = LongValue(-1, 0L)
        land(-1, v1, v2) should be(LongValue(-1, 0L))
        land(-1, v2, v1) should be(LongValue(-1, 0L))
    }

    it should ("be able to the calculate the result of the mul of a specific (but unknown) LongValue and 0") in {
        val v1 = ALongValue()
        val v2 = LongValue(-1, 0L)
        lmul(-1, v1, v2) should be(LongValue(-1, 0L))
        lmul(-1, v2, v1) should be(LongValue(-1, 0L))
    }

    it should ("be able to the calculate the result of the or of a specific (but unknown) LongValue and -1") in {
        val v1 = ALongValue()
        val v2 = LongValue(-1, -1L)
        lor(-1, v1, v2) should be(LongValue(-1, -1L))
        lor(-1, v2, v1) should be(LongValue(-1, -1L))
    }
}
