/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Create new array handler.
 *
 * @author Michael Eichberg
 */
sealed abstract class NEWARRAY extends CreateNewOneDimensionalArrayInstruction {

    def atype: Int = elementType.atype

    def elementType: BaseType

    override final val arrayType: ArrayType = ArrayType(elementType)

    override final def mnemonic: String = "newarray"

    override final def opcode: Opcode = NEWARRAY.opcode

    override final def length: Int = 2

    override final def toString: String = "NEWARRAY(" + elementType.toJava + "[])"

}

private object NEWARRAY_Boolean extends NEWARRAY {
    override final def elementType: BaseType = BooleanType
}

private object NEWARRAY_Char extends NEWARRAY {
    override final def elementType: BaseType = CharType
}

private object NEWARRAY_Float extends NEWARRAY {
    override final def elementType: BaseType = FloatType
}

private object NEWARRAY_Double extends NEWARRAY {
    override final def elementType: BaseType = DoubleType
}

private object NEWARRAY_Byte extends NEWARRAY {
    override final def elementType: BaseType = ByteType
}

private object NEWARRAY_Short extends NEWARRAY {
    override final def elementType: BaseType = ShortType
}

private object NEWARRAY_Integer extends NEWARRAY {
    override final def elementType: BaseType = IntegerType
}

private object NEWARRAY_Long extends NEWARRAY {
    override final def elementType: BaseType = LongType
}

object NEWARRAY extends InstructionMetaInformation {

    final val opcode = 188

    def unapply(newarray: NEWARRAY): Some[BaseType] = Some(newarray.elementType)

    def apply(atype: Int): NEWARRAY = {
        (atype: @annotation.switch) match {
            case BooleanType.atype => NEWARRAY_Boolean
            case CharType.atype    => NEWARRAY_Char
            case FloatType.atype   => NEWARRAY_Float
            case DoubleType.atype  => NEWARRAY_Double
            case ByteType.atype    => NEWARRAY_Byte
            case ShortType.atype   => NEWARRAY_Short
            case IntegerType.atype => NEWARRAY_Integer
            case LongType.atype    => NEWARRAY_Long
        }
    }

    def getType(atype: Int): BaseType = {
        (atype: @annotation.switch) match {
            case BooleanType.atype => BooleanType
            case CharType.atype    => CharType
            case FloatType.atype   => FloatType
            case DoubleType.atype  => DoubleType
            case ByteType.atype    => ByteType
            case ShortType.atype   => ShortType
            case IntegerType.atype => IntegerType
            case LongType.atype    => LongType
        }
    }
}
