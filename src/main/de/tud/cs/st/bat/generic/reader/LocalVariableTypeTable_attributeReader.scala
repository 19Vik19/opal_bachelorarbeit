/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische 
*    Universität Darmstadt nor the names of its contributors may be used to 
*    endorse or promote products derived from this software without specific 
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.generic.reader

import java.io.DataInputStream

import de.tud.cs.st.util.ControlAbstractions.repeat


/**

 * @author Michael Eichberg
 */
trait LocalVariableTypeTable_attributeReader  {
 
	type Constant_Pool
	type Attribute >: Null
	type LocalVariableTypeTable_attribute <: Attribute
	type LocalVariableTypeTableEntry
	implicit val LocalVariableTypeTableEntryManifest : ClassManifest[LocalVariableTypeTableEntry]
	
	type LocalVariableTypeTable = IndexedSeq[LocalVariableTypeTableEntry]


	def register(r : (String,(DataInputStream, Constant_Pool, Int) => Attribute)) : Unit


	def LocalVariableTypeTable_attribute (
		attribute_name_index : Int, attribute_length : Int, 
		local_variable_type_table : LocalVariableTypeTable
	)( implicit constant_pool : Constant_Pool) : LocalVariableTypeTable_attribute


	def LocalVariableTypeTableEntry (
		start_pc : Int, length : Int, name_index : Int, signature_index : Int, index : Int
	)( implicit constant_pool : Constant_Pool) : LocalVariableTypeTableEntry


	private lazy val reader = ( 
			de.tud.cs.st.bat.native.LocalVariableTypeTable_attribute.name -> 
			((in : DataInputStream, cp : Constant_Pool, attribute_name_index : Int) => {
				val attribute_length = in.readInt()
				LocalVariableTypeTable_attribute(
					attribute_name_index, attribute_length,
					repeat(in.readUnsignedShort){ 
						LocalVariableTypeTableEntry(
							in.readUnsignedShort,
							in.readUnsignedShort,
							in.readUnsignedShort,
							in.readUnsignedShort,
							in.readUnsignedShort
						)( cp )
					}
				)( cp )
			})
	);
	
	register(reader)
}
