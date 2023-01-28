@file:JvmName("Main")

package dev.slimevr

import io.eiren.util.logging.LogManager
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.IOException
import java.lang.System
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

val VERSION = "v0.7.0/android"
lateinit var vrServer: VRServer

fun main(args: Array<String>) {
	System.setProperty("awt.useSystemAAFontSettings", "on")
	System.setProperty("swing.aatext", "true")

	val parser: CommandLineParser = DefaultParser()
	val formatter = HelpFormatter()
	val options = Options()
	val help = Option("h", "help", false, "Show help")
	val version = Option("V", "version", false, "Show version")
	options.addOption(help)
	options.addOption(version)
	val cmd: CommandLine = try {
		parser.parse(options, args, true)
	} catch (e: org.apache.commons.cli.ParseException) {
		formatter.printHelp("slimevr.jar", options)
		exitProcess(1)
	}
	if (cmd.hasOption("help")) {
		formatter.printHelp("slimevr.jar", options)
		exitProcess(0)
	}
	if (cmd.hasOption("version")) {
		println("SlimeVR Server $VERSION")
		exitProcess(0)
	}
	val dir = File("").absoluteFile
	try {
		LogManager.initialize(dir)
	} catch (e1: java.lang.Exception) {
		e1.printStackTrace()
	}
	LogManager.info("Running version $VERSION")
	if (!SystemUtils.isJavaVersionAtLeast(org.apache.commons.lang3.JavaVersion.JAVA_17)) {
		LogManager.severe("SlimeVR start-up error! A minimum of Java 17 is required.")
		return
	}
	try {
		// This is disabled because the config can't be read at this point
		// new ServerSocket(6969).close();
		ServerSocket(35903).close()
		ServerSocket(21110).close()
	} catch (e: IOException) {
		LogManager
			.severe(
				"SlimeVR start-up error! Required ports are busy. " +
					"Make sure there is no other instance of SlimeVR Server running."
			)
		LogManager.closeLogger()
		return
	}
	try {
		vrServer = VRServer()
		vrServer.start()
		Keybinding(vrServer)
		val scanner = thread {
			while (true) {
				if (readln() == "exit") {
					vrServer.interrupt()
					break
				}
			}
		}
		vrServer.join()
		scanner.join()
		LogManager.closeLogger()
		exitProcess(0)
	} catch (e: Throwable) {
		e.printStackTrace()
		exitProcess(1)
	}
}
