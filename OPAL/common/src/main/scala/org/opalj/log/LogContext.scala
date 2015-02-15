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
package log

/**
 * A log context associates log messages with a specific context and logger.
 * Using a log context
 * facilitates the suppression of recurring message in a specific context and also
 * makes it possible to direct message to different targets.
 * Before using a `LogContext` it has to be registered with the [[OPALLogger$]].
 *
 * OPAL uses two primary log contexts:
 *
 *  1. The [[GlobalContext$]] which should be used for general
 *      log messages related to OPAL, such as the number of threads used for
 *      computations.
 *
 *  1. The log context associated with [[org.opalj.br.analyses.Project]]s to log
 *      project related information (e.g., such as project related results or
 *      configuration issues.)
 *
 * @note The registration of the `LogContext` with the `OPALLogger` does not prevent
 *      the garbage collection of the `LogContext` unless a logged message explicitly
 *      references its log context. This is – however – discouraged! If no message
 *      explicitly reference the log context it is then possible to unregister the log context
 *      in the `finalize` method that references the context.
 *
 * @author Michael Eichberg
 */
trait LogContext {

    @volatile private[log] var id: Int = -1

    /**
     * The unique id associated with this log context. Each log context gets a unique id
     * when it is registered with the OPALLogger. This id will not change afterwards.
     */
    def logContextId: Int = id
}

/**
 * The global log context which should be used to log global messages.
 *
 * @author Michael Eichberg
 */
case object GlobalContext extends LogContext {

    OPALLogger.globalContextMutex.synchronized {
        OPALLogger.globalContextCreated = true
        OPALLogger.register(this, OPALLogger.globalContextLogger)
    }
}
