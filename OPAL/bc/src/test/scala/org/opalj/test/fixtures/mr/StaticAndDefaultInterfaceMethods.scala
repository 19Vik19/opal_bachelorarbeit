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
package bc

import java.nio.file.Files
import java.nio.file.Paths

import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_ABSTRACT
import org.opalj.da.ClassFile
import org.opalj.da.Method_Info
import org.opalj.da.Constant_Pool_Entry
import org.opalj.da.CONSTANT_Class_info
import org.opalj.da.CONSTANT_Utf8
import org.opalj.da.CONSTANT_NameAndType_info
import org.opalj.da.CONSTANT_Methodref_info
import org.opalj.da.CONSTANT_String_info
import org.opalj.da.Code_attribute
import org.opalj.da.Code
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_STATIC

/**
 * Generates two interfaces SuperIntf and Intf where Intf inherits from
 * SuperIntf and where Intf defines a static method that has the same
 * name and descriptor as a default method defined SuperIntf.
 *
 * @see For further details see: `Readme.md`.
 *
 * @author Michael Eichberg
 */
object StaticAndDefaultInterfaceMethods extends App {

    val superIntfCF = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null,
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/SuperIntf"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Class_info(6),
            /*  6 */ CONSTANT_Utf8("mr/SuperIntf"),
            /*  7 */ CONSTANT_Utf8("m"),
            /*  8 */ CONSTANT_Utf8("()V"),
            /*  9 */ CONSTANT_Utf8("Code"),
            /* 10 */ CONSTANT_String_info(11),
            /* 11 */ CONSTANT_Utf8("SuperIntf.m"),
            /* 12 */ CONSTANT_Methodref_info(13, 15),
            /* 13 */ CONSTANT_Class_info(14),
            /* 14 */ CONSTANT_Utf8("mr/Helper"),
            /* 15 */ CONSTANT_NameAndType_info(16, 17),
            /* 16 */ CONSTANT_Utf8("println"),
            /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
        ),
        minor_version = 0, major_version = 52,
        access_flags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask,
        this_class = 1 /*mr/SuperIntf*/ , super_class = 3 /*extends java.lang.Object*/ ,
        // Interfaces.empty,
        // Fields.empty,
        methods = IndexedSeq(
            Method_Info(
                access_flags = ACC_PUBLIC.mask,
                name_index = 7, descriptor_index = 8,
                attributes = IndexedSeq(
                    Code_attribute(
                        attribute_name_index = 9,
                        max_stack = 1, max_locals = 1,
                        code =
                        new Code(
                            Array[Byte](
                                18, // ldc
                                10, // #10
                                (0xff & 184).toByte, // invokestatic
                                0, // -> Methodref
                                12, // #12
                                (0xff & 177).toByte // return
                            )
                        )
                    )
                )
            )
        )
    )
    val assembledSuperIntf = Assembler(superIntfCF)
    val assembledSuperIntfPath = Paths.get("OPAL/bc/src/test/resources/StaticAndDefaultInterfaceMethods/mr/SuperIntf.class")
    val assembledSuperIntfFile = Files.write(assembledSuperIntfPath, assembledSuperIntf)
    println("Created class file: "+assembledSuperIntfFile.toAbsolutePath())

    val intfCF = ClassFile(
        Array[Constant_Pool_Entry](
            /*  0 */ null,
            /*  1 */ CONSTANT_Class_info(2),
            /*  2 */ CONSTANT_Utf8("mr/Intf"),
            /*  3 */ CONSTANT_Class_info(4),
            /*  4 */ CONSTANT_Utf8("java/lang/Object"),
            /*  5 */ CONSTANT_Class_info(6),
            /*  6 */ CONSTANT_Utf8("mr/SuperIntf"),
            /*  7 */ CONSTANT_Utf8("m"),
            /*  8 */ CONSTANT_Utf8("()V"),
            /*  9 */ CONSTANT_Utf8("Code"),
            /* 10 */ CONSTANT_String_info(11),
            /* 11 */ CONSTANT_Utf8("Intf.m"),
            /* 12 */ CONSTANT_Methodref_info(13, 15),
            /* 13 */ CONSTANT_Class_info(14),
            /* 14 */ CONSTANT_Utf8("mr/Helper"),
            /* 15 */ CONSTANT_NameAndType_info(16, 17),
            /* 16 */ CONSTANT_Utf8("println"),
            /* 17 */ CONSTANT_Utf8("(Ljava/lang/String;)V")
        ),
        minor_version = 0, major_version = 52,
        access_flags = ACC_INTERFACE.mask | ACC_ABSTRACT.mask,
        this_class = 1 /*mr/Intf*/ , super_class = 3 /*extends java.lang.Object*/ ,
        interfaces = IndexedSeq(5),
        // Fields.empty,
        methods = IndexedSeq(
            Method_Info(
                access_flags = ACC_PUBLIC.mask | ACC_STATIC.mask,
                name_index = 7, descriptor_index = 8,
                attributes = IndexedSeq(
                    Code_attribute(
                        attribute_name_index = 9,
                        max_stack = 1, max_locals = 1,
                        code =
                        new Code(
                            Array[Byte](
                                18, // ldc
                                10, // #10
                                (0xff & 184).toByte, // invokestatic
                                0, // -> Methodref
                                12, //    #12
                                (0xff & 177).toByte // return
                            )
                        )
                    )
                )
            )
        )
    )
    val assembledIntf = Assembler(intfCF)
    val assembledIntfPath = Paths.get("OPAL/bc/src/test/resources/StaticAndDefaultInterfaceMethods/mr/Intf.class")
    val assembledIntfFile = Files.write(assembledIntfPath, assembledIntf)
    println("Created class file: "+assembledIntfFile.toAbsolutePath())
}