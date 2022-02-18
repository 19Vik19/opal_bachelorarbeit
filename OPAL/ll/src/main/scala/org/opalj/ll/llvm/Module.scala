/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.llvm

import org.bytedeco.llvm.LLVM.{LLVMModuleRef, LLVMValueRef}
import org.bytedeco.llvm.global.LLVM.{
    LLVMDisposeMessage,
    LLVMGetFirstFunction,
    LLVMGetNamedFunction,
    LLVMGetNextFunction,
    LLVMPrintModuleToString
}

case class Module(ref: LLVMModuleRef) {
    def functions(): FunctionIterator = {
        new FunctionIterator(LLVMGetFirstFunction(ref))
    }

    def repr(): String = {
        val bytePointer = LLVMPrintModuleToString(ref)
        val string = bytePointer.getString
        LLVMDisposeMessage(bytePointer)
        string
    }

    def get_function(name: String): Function = {
        val func_ref = LLVMGetNamedFunction(ref, name)
        Value(func_ref) match {
            case _: NullValue ⇒
                throw new IllegalArgumentException("Unknown function '"+name+"'")
            case function: Function ⇒ function
        }
    }
}

class FunctionIterator(var ref: LLVMValueRef) extends Iterator[Function] {
    override def hasNext: Boolean = ref != null

    override def next(): Function = {
        val function = Function(ref)
        this.ref = LLVMGetNextFunction(ref)
        function
    }
}
