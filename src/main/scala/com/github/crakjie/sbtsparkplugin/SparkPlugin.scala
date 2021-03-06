package main.scala.com.github.crakjie.sbtsparkplugin

import com.github.crakjie.sbtsparkplugin.CommandLineParser
import sbt._
import Keys._
import grizzled.sys._
import OperatingSystem._
import Def.{ inputKey, spaceDelimited }


object SparkPlugin extends sbt.AutoPlugin {

	val commandRegex = """(['"])((?:\\\1|.)+?)\1|([^\s"']+)""".r
  
  override def trigger = allRequirements
  
	object autoImport {
	  lazy val sparkHome = settingKey[String]("The directory of your spark containing /bin")
		lazy val submitLogLevel = settingKey[Level.Value]("the log level of your sbt-spark-plugin")
    lazy val submitOptions = settingKey[String]("Here you can writes every submit params not implemented in this plugin")
		lazy val submit = inputKey[Unit]("Spark submit")
	}
	
	import autoImport._
	
	
	
	override lazy val projectSettings = Seq(
      submitOptions := "",
	    sparkHome := ".",
			submitLogLevel := Level.Info,
	    submit := {

				val args : Seq[String] = spaceDelimited("<arg>").parsed

				val jars =  (dependencyClasspath in Runtime).value.files.map(_.getAbsolutePath).filterNot(_.endsWith("target/scala-2.11/classes"))
				val strJars = jars.sorted.mkString(",")
				val (art, file) = packagedArtifact.in(Compile, packageBin).value
				//Calculate the main class parameter
				val mainOp = (mainClass in Runtime).value match {
					case None => ""
					case Some(clazz) => "--class " + clazz
				}

				//on windows spark-submit is a cmd
				val execName = if(os == Windows) 	"spark-submit.cmd" else "spark-submit"
				//use path to have the right sperator depending of the operating system
				val sparkSubmitLocation = sbt.Path.apply(sparkHome.value) / "bin" / execName

				val cmd =  sparkSubmitLocation.getAbsolutePath+" "+mainOp+"  --jars " + strJars +" " + submitOptions.value + " " + file.getAbsolutePath + " " + args.mkString(" ")

				if(submitLogLevel.value == Level.Debug) {
					streams.value.log.debug(cmd)
				}

				CommandLineParser.parse(cmd) !
		}
	)

}