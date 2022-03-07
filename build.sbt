import org.jetbrains.sbtidea.Keys.IntelliJPlatform
import sbt.Keys.scalaVersion
import sbtbuildinfo.BuildInfoOption.{BuildTime, ToMap}

maintainer := "reid@reactific.com"

Global / onChangedBuildSource := ReloadOnSourceChanges
(Global / excludeLintKeys) ++=
  Set(buildInfoPackage, buildInfoKeys, buildInfoOptions, mainClass, maintainer,
    intellijAttachSources)

ThisBuild / versionScheme := Option("semver-spec")
ThisBuild / dynverVTagPrefix := false

// NEVER  SET  THIS: version := "0.1"
// IT IS HANDLED BY: sbt-dynver
ThisBuild / dynverSeparator := "-"
ThisBuild / organization := "com.yoppworks"
ThisBuild / scalaVersion := "2.13.7"
buildInfoOptions := Seq(ToMap, BuildTime)
buildInfoKeys := Seq[BuildInfoKey](
  name,
  normalizedName,
  description,
  homepage,
  startYear,
  organization,
  organizationName,
  organizationHomepage,
  version,
  scalaVersion,
  sbtVersion
)

lazy val scala2_13_Options = Seq(
  "-target:11",
  // "-Ypatmat-exhaust-depth 40", Zinc can't handle this :(
  "-Xsource:3",
  "-Wdead-code",
  "-deprecation",
  "-feature",
  "-Werror",
  "-Wunused:imports", // Warn if an import selector is not referenced.
  "-Wunused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Wunused:privates", // Warn if a private member is unused.
  "-Wunused:locals", // Warn if a local definition is unused.
  "-Wunused:explicits", // Warn if an explicit parameter is unused.
  "-Wunused:implicits", // Warn if an implicit parameter is unused.
  "-Wunused:params", // Enable -Wunused:explicits,implicits.
  "-Xlint:nonlocal-return", // A return statement used an exception for flow control.
  "-Xlint:implicit-not-found", // Check @implicitNotFound and @implicitAmbiguous messages.
  "-Xlint:serial", // @SerialVersionUID on traits and non-serializable classes.
  "-Xlint:valpattern", // Enable pattern checks in val definitions.
  "-Xlint:eta-zero", // Warn on eta-expansion (rather than auto-application) of zero-ary method.
  "-Xlint:eta-sam", // Warn on eta-expansion to meet a Java-defined functional
  // interface that is not explicitly annotated with @FunctionalInterface.
  "-Xlint:deprecation" // Enable linted deprecations.
)

lazy val riddl = (project in file(".")).settings(publish := {}, publishLocal := {})
  .aggregate(
    language,
    `d3-generator`,
    `hugo-theme`,
    `hugo-translator`,
    examples, doc,
    riddlc, `sbt-riddl`
  )

lazy val language = project.in(file("language"))
  .enablePlugins(BuildInfoPlugin)
  .configure(C.withCoverage)
  .settings(
    name := "riddl-language",
    buildInfoObject := "BuildInfo",
    buildInfoPackage := "com.yoppworks.ossum.riddl",
    buildInfoUsePackageAsPath := true,
    coverageExcludedPackages := "<empty>;.*AST;.*BuildInfo;.*PredefinedType;.*Terminals.*",
    scalacOptions := scala2_13_Options,
    libraryDependencies ++= Seq(Dep.scopt, Dep.fastparse)  ++ Dep.testing,
  )

lazy val `hugo-theme` = project.in(file("hugo-theme"))
  .configure(C.zipResource("main"))
  .settings(
    name := "riddl-hugo-theme"
  )

lazy val `d3-generator` = project.in(file("d3-generator"))
  .settings(
    name := "riddl-d3-generator",
    scalacOptions := scala2_13_Options,
    libraryDependencies ++= Seq(Dep.ujson) ++ Dep.testing
  ).dependsOn(language % "compile->compile;test->test")

lazy val `hugo-translator`: Project = project.in(file("hugo-translator"))
  .settings(
    name := "riddl-hugo-translator",
    Compile / unmanagedResourceDirectories += {baseDirectory.value / "resources"},
    Test / parallelExecution := false,
    libraryDependencies ++= Seq(Dep.pureconfig) ++ Dep.testing
  ).dependsOn(language % "compile->compile;test->test", `hugo-theme`)

lazy val `hugo-git-check`: Project = project.in(file("hugo-git-check"))
  .settings(
    name := "riddl-hugo-git-check-translator",
    buildInfoPackage := "com.yoppworks.ossum.riddl.translator.hugo_git_check",
    Compile / unmanagedResourceDirectories += {baseDirectory.value / "resources"},
    Test / parallelExecution := false,
    libraryDependencies ++= Seq(Dep.pureconfig, Dep.jgit) ++ Dep.testing
  ).dependsOn(`hugo-translator` % "compile->compile;test->test")

lazy val examples = project.in(file("examples")).settings(
  name := "riddl-examples",
  Compile / packageBin / publishArtifact := false,
  Compile / packageDoc / publishArtifact := false,
  Compile / packageSrc / publishArtifact := false,
  publishTo := Option(Resolver.defaultLocal),
  libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.9" % "test")
).dependsOn(`hugo-translator` % "test->test", riddlc)

lazy val doc = project.in(file("doc"))
  .enablePlugins(SitePlugin)
  .enablePlugins(SiteScaladocPlugin)
  .configure(C.zipResource("hugo"))
  .settings(
  name := "riddl-doc",
  publishTo := Option(Resolver.defaultLocal),
  // Hugo / sourceDirectory := sourceDirectory.value / "hugo",
  publishSite
).dependsOn(`hugo-translator` % "test->test", riddlc)

lazy val doc2 = project.in(file("doc2"))
  .enablePlugins(SitePlugin)
  .enablePlugins(SiteScaladocPlugin)
  .configure(C.zipResource("hugo"))
  .settings(
    name := "riddl-doc2",
    publishTo := Option(Resolver.defaultLocal),
    // Hugo / sourceDirectory := sourceDirectory.value / "hugo",
    publishSite
  ).dependsOn(`hugo-translator` % "test->test", riddlc)

lazy val riddlc: Project = project.in(file("riddlc"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "riddlc",
    mainClass := Option("com.yoppworks.ossum.riddl.RIDDLC"),
    scalacOptions := scala2_13_Options,
    libraryDependencies ++= Seq(Dep.pureconfig) ++ Dep.testing,
    maintainer := "reid.spencer@yoppworks.com",
  ).dependsOn(language, `hugo-translator` % "compile->compile;test->test",
  `hugo-git-check` % "compile->compile;test->test"
  )


lazy val `sbt-riddl` = (project in file("sbt-riddl")).enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-riddl",
    sbtPlugin := true,
    scalaVersion := "2.12.15",
    buildInfoPackage := "com.yoppworks.ossum.riddl.sbt.plugin",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )

lazy val `riddl-idea-plugin` = project.in(file("riddl-idea-plugin"))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    ThisBuild / intellijPluginName := "riddl-idea-plugin",
    ThisBuild / intellijBuild      := "213.6461.79",
    ThisBuild / intellijPlatform   := IntelliJPlatform.IdeaCommunity,
    intellijPlugins       += "com.intellij.properties".toPlugin,
    ThisBuild / intellijAttachSources := true,
    Compile / javacOptions ++= "--release" :: "11" :: Nil,
    libraryDependencies ++= Seq(
      "com.eclipsesource.minimal-json" % "minimal-json" % "0.9.5" withSources()
    ),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedResourceDirectories    += baseDirectory.value / "testResources"
  ).dependsOn(language)
