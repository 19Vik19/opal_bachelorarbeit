/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
package android

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.EntryPointFinder
import org.opalj.br.android.AndroidManifest
import org.opalj.br.android.AndroidManifestKey
import org.opalj.tac.fpcf.analyses.MethodDescription

/**
 * The AndroidEntryPointFinder considers specific methods of launcher Activity Clases as entry points.
 * An activity is a launcher activity if it contains an intent filter with action "android.intent.action.MAIN"
 * and category "android.intent.category.LAUNCHER". Requires Android Manifest to be loaded.
 *
 * @author Julius Naeumann
 */
object AndroidEntryPointsFinder extends EntryPointFinder {

    val configKey = "org.opalj.tac.cg.android.AndroidEntryPointsFinder.entryPoints"

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        super.requirements(project) ++ Seq(AndroidManifestKey)
    }

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        val entryPointDescriptions = getConfiguredEntryPoints(project)
        val manifest: AndroidManifest = project.get(AndroidManifestKey)

        // get launcher activities
        val launchableClasses = manifest.activities.filter(_.isLauncherActivity).map(_.cls)
        val classHierarchy = project.classHierarchy
        val entryPoints = ArrayBuffer[Method]()

        // iterate over launchable classes, collect their respective entry point methods according to config
        for (componentClass <- launchableClasses) {
            for (epd <- entryPointDescriptions) {
                if (
                    classHierarchy.isASubtypeOf(ReferenceType(componentClass.fqn), ReferenceType(epd.cf)).isYesOrUnknown
                ) {
                    entryPoints ++= componentClass.findMethod(epd.name, MethodDescriptor(epd.desc))
                }
            }
        }

        entryPoints
    }

    private def getConfiguredEntryPoints(project: SomeProject) = {
        val entryPointDescriptionsConfig = project.config.getConfigList(configKey).asScala.toArray
        val entryPointDescriptions = entryPointDescriptionsConfig.map(c => MethodDescription.reader.read(c, ""))
        entryPointDescriptions
    }
}
