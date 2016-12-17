/* BSD 2-Clause License:
 * Copyright (c) 2016
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
package ba

import java.io.ByteArrayInputStream

import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.junit.runner.RunWith

import org.opalj.util.InMemoryClassLoader
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.ACC_PUBLIC
import org.opalj.br.MethodDescriptor
import org.opalj.br.reader.Java8Framework.{ClassFile ⇒ ClassFileReader}
import org.opalj.bc.Assembler

/**
 * Tests general properties of a classes build with the BytecodeAssembler DSL by loading and
 * instantiating them with the JVM and doing a round trip `BRClassFile` -> `DAClassFile` ->
 * `Assembler` -> `BRClassFile`.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class ClassDeclarationTest extends FlatSpec {

    behavior of "the ClassFileDeclarationBuilder"

    val markerInterface1 = ABSTRACT + INTERFACE CLASS "MarkerInterface1"
    val markerInterface2 = ABSTRACT + INTERFACE CLASS "MarkerInterface2"

    val abstractClass = ABSTRACT + PUBLIC CLASS "org/opalj/bc/AbstractClass"

    val simpleConcreteClass = (
        PUBLIC + SUPER + FINAL + SYNTHETIC CLASS "ConcreteClass"
        EXTENDS "org/opalj/bc/AbstractClass"
        IMPLEMENTS ("MarkerInterface1", "MarkerInterface2")
    ) Version (minorVersion = 2, majorVersion = 49)

    val abstractAsm = Assembler(abstractClass.buildDAClassFile)
    val concreteAsm = Assembler(simpleConcreteClass.buildDAClassFile)
    val abstractBRClassFile = ClassFileReader(() ⇒ new ByteArrayInputStream(abstractAsm)).head
    val concreteBRClassFile = ClassFileReader(() ⇒ new ByteArrayInputStream(concreteAsm)).head

    val loader = new InMemoryClassLoader(
        Map(
            "MarkerInterface1" → Assembler(markerInterface1.buildDAClassFile),
            "MarkerInterface2" → Assembler(markerInterface2.buildDAClassFile),
            "ConcreteClass" → concreteAsm,
            "org.opalj.bc.AbstractClass" → Assembler(abstractClass.buildDAClassFile)
        ),
        this.getClass.getClassLoader
    )

    "the generated classes" should "load correctly" in {
        loader.loadClass("MarkerInterface1")
        loader.loadClass("MarkerInterface2")
        loader.loadClass("org.opalj.bc.AbstractClass")
        loader.loadClass("ConcreteClass")

        val clazz = loader.loadClass("ConcreteClass")
        assert(clazz.newInstance() != null)
    }

    "the generated class 'ConcreteClass'" should "have a generated default Constructor" in {
        assert(concreteBRClassFile.methods.size == 1)
        assert(concreteBRClassFile.methods.head.descriptor == MethodDescriptor("()V"))
    }

    it should "extend the specified Class" in {
        assert(concreteBRClassFile.superclassType.get.fqn == "org/opalj/bc/AbstractClass")
    }

    it should "implement the specified Interfaces" in {
        assert(concreteBRClassFile.interfaceTypes.map(i ⇒ i.fqn).contains("MarkerInterface1"))
        assert(concreteBRClassFile.interfaceTypes.map(i ⇒ i.fqn).contains("MarkerInterface2"))
    }

    it should "have the specified access_flags" in {
        assert(concreteBRClassFile.accessFlags ==
            (ACC_PUBLIC.mask | ACC_FINAL.mask | ACC_SYNTHETIC.mask | ACC_SUPER.mask))
    }

    it should "have the specified minor version" in {
        assert(concreteBRClassFile.minorVersion == 2)
    }

    it should "have the specified major version" in {
        assert(concreteBRClassFile.majorVersion == 49)
    }

    "the generated class 'AbstractClass'" should "have the default minor version" in {
        assert(abstractBRClassFile.minorVersion == ClassFileBuilder.defaultMinorVersion)
    }

    it should "have the default major version" in {
        assert(abstractBRClassFile.majorVersion == ClassFileBuilder.defaultMajorVersion)
    }
}