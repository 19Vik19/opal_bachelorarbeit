/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import java.util.Date
import java.net.URL

import org.opalj.br.{ClassFile, Method}
import org.opalj.br.analyses.{Project, SomeProject}
import org.opalj.ai.domain.l0.BaseDomain
import org.opalj.ai.domain.TheCode
import org.opalj.ai.util.XHTML
import org.opalj.ai.common.XHTML.dump
import org.opalj.ai.common.XHTML.dumpAIState
import org.opalj.io.writeAndOpen
import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.RecordDefUse
import org.opalj.graphs.toDot

/**
 * A small basic framework that facilitates the abstract interpretation of a
 * specific method using a configurable domain.
 *
 * @author Michael Eichberg
 */
object InterpretMethod {

    private class IMAI(IdentifyDeadVariables: Boolean) extends AI[Domain](IdentifyDeadVariables) {

        override def isInterrupted: Boolean = Thread.interrupted()

        override val tracer = {
            val consoleTracer = new ConsoleTracer { override val printOIDs = true }
            val xHTMLTracer = new XHTMLTracer {}
            //    Some(new ConsoleTracer {})
            //Some(new ConsoleEvaluationTracer {})
            Some(new MultiTracer(consoleTracer, xHTMLTracer))
        }
    }

    /**
     * Traces the interpretation of a single method and prints out the results.
     *
     * @param args The first element must be the name of a class file, a jar file
     *      or a directory containing the former. The second element must
     *      denote the name of a class and the third must denote the name of a method
     *      of the respective class. If the method is overloaded the first method
     *      is returned.
     */
    def main(args: Array[String]): Unit = {
        import Console.{RED, RESET}
        import language.existentials

        def printUsage(issue: Option[String]): Unit = {
            issue.foreach(issue ⇒ println(s"Failure: $issue."))
            println("You have to specify the following parameters.")
            println("\t1: a jar/class file or a directory containing jar/class files.")
            println("\t2: the name of a class.")
            println("\t3: the simple name or signature of a method of the specified class.")
            println("\t4[Optional]: -domain=CLASS the name of the class of the configurable domain to use.")
            println("\t5[Optional]: -trace={true,false} default:true")
            println("\t6[Optional]: -identifyDeadVariables={true,false} default:true")
        }

        if (args.size < 3 || args.size > 5) {
            printUsage(Some("wrong number of parameters"))
            return ;
        }
        var remainingArgs = args.toList
        val fileName = remainingArgs.head; remainingArgs = remainingArgs.tail
        val className = remainingArgs.head; remainingArgs = remainingArgs.tail
        val methodName = remainingArgs.head; remainingArgs = remainingArgs.tail
        val domainClass = {
            if (remainingArgs.nonEmpty && remainingArgs.head.startsWith("-domain=")) {
                val domainRawClass = Class.forName(remainingArgs.head.substring(8))
                val clazz = domainRawClass.asInstanceOf[Class[_ <: Domain]]
                remainingArgs = remainingArgs.tail
                clazz
            } else // default domain
                classOf[BaseDomain[java.net.URL]]
        }
        val doTrace = {
            if (remainingArgs.nonEmpty && remainingArgs.head.startsWith("-trace=")) {
                val result = (
                    remainingArgs.head == "-trace=true" ||
                    remainingArgs.head == "-trace=1"
                )
                remainingArgs = remainingArgs.tail
                result
            } else
                true
        }
        val doIdentifyDeadVariables = {
            if (remainingArgs.nonEmpty && remainingArgs.head.startsWith("-identifyDeadVariables=")) {
                val result = (
                    remainingArgs.head == "-identifyDeadVariables=true" ||
                    remainingArgs.head == "-identifyDeadVariables=1"
                )
                remainingArgs = remainingArgs.tail
                result
            } else
                true
        }
        if (remainingArgs.nonEmpty) {
            printUsage(Some(remainingArgs.mkString("unexpected arguments: ", ", ", "")))
            return ;
        }

        def createDomain[Source: reflect.ClassTag](
            project:   SomeProject,
            classFile: ClassFile,
            method:    Method
        ): Domain = {

            scala.util.control.Exception.ignoring(classOf[NoSuchMethodException]) {
                val constructor = domainClass.getConstructor(classOf[Object])
                return constructor.newInstance(classFile);
            }

            val constructor =
                domainClass.getConstructor(classOf[Project[URL]], classOf[ClassFile], classOf[Method])

            constructor.newInstance(project, classFile, method)
        }

        val file = new java.io.File(fileName)
        if (!file.exists()) {
            println(RED+"[error] The file does not exist: "+fileName+"."+RESET)
            return ;
        }

        val project =
            try {
                Project(file)
            } catch {
                case e: Exception ⇒
                    println(RED+"[error] Cannot process file: "+e.getMessage+"."+RESET)
                    return ;
            }

        val classFile = {
            val fqn = className.replace('.', '/')
            project.allClassFiles.find(_.fqn == fqn).getOrElse {
                println(RED+"[error] Cannot find the class: "+className+"."+RESET)
                return ;
            }
        }

        val method =
            (
                if (methodName.contains("("))
                    classFile.methods.find(m ⇒ m.descriptor.toJava(m.name).contains(methodName))
                else
                    classFile.methods.find(_.name == methodName)
            ) match {
                    case Some(method) ⇒
                        if (method.body.isDefined)
                            method
                        else {
                            println(RED+
                                "[error] The method: "+methodName+" does not have a body"+RESET)
                            return ;
                        }
                    case None ⇒
                        println(RED+
                            "[error] Cannot find the method: "+methodName+"."+RESET +
                            classFile.methods.map(m ⇒ m.descriptor.toJava(m.name)).toSet.
                            toSeq.sorted.mkString(" Candidates: ", ", ", "."))
                        return ;
                }

        try {
            val result =
                if (doTrace)
                    new IMAI(doIdentifyDeadVariables)(
                        classFile, method, createDomain(project, classFile, method)
                    )
                else {
                    val body = method.body.get
                    println("Starting abstract interpretation of: ")
                    println("\t"+classFile.thisType.toJava+"{")
                    println("\t\t"+method.toJava+
                        "[instructions="+body.instructions.size+
                        "; #max_stack="+body.maxStack+
                        "; #locals="+body.maxLocals+"]")
                    println("\t}")
                    val result =
                        new BaseAI(doIdentifyDeadVariables)(
                            classFile, method, createDomain(project, classFile, method)
                        )
                    println("Finished abstract interpretation.")
                    result
                }

            if (result.domain.isInstanceOf[RecordCFG]) {
                val evaluatedInstructions = result.evaluatedInstructions
                val cfgDomain = result.domain.asInstanceOf[RecordCFG]

                val cfgAsDotGraph = toDot(Set(cfgDomain.cfgAsGraph()), ranksep = "0.3").toString
                val cfgFile = writeAndOpen(cfgAsDotGraph, "AICFG", ".gv")
                println("AI CFG: "+cfgFile)

                val dominatorTreeAsDot = cfgDomain.dominatorTree.toDot((i: Int) ⇒ evaluatedInstructions.contains(i))
                val domFile = writeAndOpen(dominatorTreeAsDot, "DominatorTreeOfTheAICFG", ".gv")
                println("AI CFG - Dominator tree: "+domFile)

                val postDominatorTreeAsDot = cfgDomain.postDominatorTree.toDot((i: Int) ⇒ evaluatedInstructions.contains(i))
                val postDomFile = writeAndOpen(postDominatorTreeAsDot, "PostDominatorTreeOfTheAICFG", ".gv")
                println("AI CFG - Post-Dominator tree: "+postDomFile)

                val cdg = cfgDomain.controlDependencies
                val rdfAsDotGraph = cdg.dominanceFrontiers.toDot(evaluatedInstructions.contains(_))
                val rdfFile = writeAndOpen(rdfAsDotGraph, "ReverseDominanceFrontiersOfAICFG", ".gv")
                println("AI CFG - Reverse Dominance Frontiers: "+rdfFile)
            }

            if (result.domain.isInstanceOf[RecordDefUse]) {
                val duInfo = result.domain.asInstanceOf[RecordDefUse]
                writeAndOpen(duInfo.dumpDefUseInfo(), "DefUseInfo", ".html")

                val dotGraph = toDot(duInfo.createDefUseGraph(method.body.get)).toString()
                writeAndOpen(dotGraph, "ImplicitDefUseGraph", ".gv")
            }

            writeAndOpen(
                dump(
                    Some(classFile),
                    Some(method),
                    method.body.get,
                    Some(

                        s"Analyzed: ${new Date}<br>Domain: ${domainClass.getName}<br>"+
                            (
                                if (doIdentifyDeadVariables)
                                    "<b>Dead Variables Identification</b><br>"
                                else
                                    "<u>Dead Variables are not filtered</u>.<br>"
                            ) +
                                XHTML.instructionsToXHTML("PCs where paths join", result.cfJoins) +
                                (
                                    if (result.subroutineInstructions.nonEmpty) {
                                        XHTML.instructionsToXHTML(
                                            "Subroutine instructions", result.subroutineInstructions
                                        )
                                    } else {
                                        ""
                                    }
                                ) + XHTML.evaluatedInstructionsToXHTML(result.evaluated)
                    ),
                    result.domain
                )(result.cfJoins, result.operandsArray, result.localsArray),
                "AIResult",
                ".html"
            )
        } catch {
            case ife: InterpretationFailedException ⇒

                def causeToString(ife: InterpretationFailedException, nested: Boolean): String = {
                    val parameters =
                        if (nested) {
                            ife.localsArray(0).toSeq.reverse.
                                filter(_ != null).map(_.toString).
                                mkString("Parameters:<i>", ", ", "</i><br>")
                        } else
                            ""

                    val aiState =
                        "<p><i>"+ife.domain.getClass.getName+"</i><b>( "+ife.domain.toString+" )"+"</b></p>"+
                            parameters+
                            "Current instruction: "+ife.pc+"<br>"+
                            XHTML.evaluatedInstructionsToXHTML(ife.evaluated) + {
                                if (ife.worklist.nonEmpty)
                                    ife.worklist.mkString("Remaining worklist:\n<br>", ", ", "<br>")
                                else
                                    "Remaining worklist: <i>EMPTY</i><br>"
                            }

                    val metaInformation = ife.cause match {
                        case ife: InterpretationFailedException ⇒
                            aiState + ife.cause.
                                getStackTrace.
                                mkString("\n<ul><li>", "</li>\n<li>", "</li></ul>\n")+
                                "<div style='margin-left:5em'>"+causeToString(ife, true)+"</div>"
                        case e: Throwable ⇒
                            val message = e.getMessage()
                            if (message != null)
                                aiState+"<br>Underlying cause: "+util.XHTML.htmlify(message)
                            else
                                aiState+"<br>Underlying cause: <NULL>"
                        case _ ⇒
                            aiState
                    }

                    if (nested && ife.domain.isInstanceOf[TheCode])
                        metaInformation +
                            dumpAIState(
                                ife.domain.asInstanceOf[TheCode].code,
                                ife.domain
                            )(ife.cfJoins, ife.operandsArray, ife.localsArray)
                    else
                        metaInformation
                }

                val resultHeader = Some(causeToString(ife, false))
                val evaluationDump =
                    dump(
                        Some(classFile), Some(method), method.body.get,
                        resultHeader, ife.domain
                    )(ife.cfJoins, ife.operandsArray, ife.localsArray)
                writeAndOpen(evaluationDump, "StateOfCrashedAbstractInterpretation", ".html")
                throw ife
        }
    }
}
