package com.swoval.format

import com.swoval.format.impl.ClangFormatter
import java.net.URLClassLoader
import sbt.Keys._
import sbt._

/**
 * An sbt plugin that provides sources formatting tasks. The default tasks can either format the
 * task in place, or verify the source file formatting by adding the --check flag to the task key.
 */
object SourceFormatPlugin extends AutoPlugin {
  override def trigger = allRequirements

  /**
   * A simpler version `sbt.internal.io.Source` that will work for all versions of sbt.
   */
  type Source = (File, FileFilter, Boolean)
  object autoImport {
    lazy val clangfmt = inputKey[Unit]("Format source files using clang format")
    lazy val clangfmtSources =
      settingKey[Seq[Source]]("The source files to format using clang format")
    lazy val javafmt =
      inputKey[Unit]("Format source files using the google java formatter")
    lazy val javafmtSources =
      settingKey[Seq[Source]]("The source files to format using javafmt")
  }

  import autoImport._
  private def sources(extensions: String*): File => Source =
    (_, ExtensionFilter(extensions: _*), true)
  private lazy val clangSources = sources("c", "cc", "cpp", "cxx", "h", "hh", "hpp", "hxx")
  private lazy val javaSources = sources("java")
  private val javaFormatter: (File, Boolean) => Boolean = {
    val loader = this.getClass.getClassLoader match {
      case l: URLClassLoader =>
        val sorted = l.getURLs.toSeq.sortBy(u => if (u.toString.contains("guava")) -1 else 1)
        new URLClassLoader(sorted.toArray, l.getParent)
      case l => l
    }
    loader.loadClass("com.swoval.format.impl.JavaFormatter$").getDeclaredField("MODULE$").get(null)
  }.asInstanceOf[(File, Boolean) => Boolean]
  override lazy val projectSettings: Seq[Def.Setting[_]] = super.projectSettings ++ Seq(
    clangfmtSources := (unmanagedSourceDirectories in Compile).value.map(clangSources),
    clangfmt := Formatter(clangfmtSources, ClangFormatter, UnformattedFileException.Clang).evaluated,
    javafmtSources := (unmanagedSourceDirectories in Compile).value.map(javaSources),
    javafmt := Formatter(javafmtSources, javaFormatter, UnformattedFileException.Java).evaluated
  )
}
