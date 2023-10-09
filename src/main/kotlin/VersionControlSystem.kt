import java.io.File
import java.io.FileNotFoundException
import java.util.*

fun main(args: Array<String>) {
    when {
        args.isEmpty() || args[0] == "--help" -> printHelpPage()
        args[0] == "config" -> handleConfigCommand(args)
        args[0] == "add" -> handleAddCommand(args)
        args[0] == "log" -> handleLogCommand()
        args[0] == "commit" -> handleCommitCommand(args)
        args[0] == "checkout" -> handleCheckoutCommand(args)
        else -> println("'${args[0]}' is not a SVCS command.")
    }
}

fun printHelpPage() {
    println(
        """
        These are SVCS commands:
        config     Get and set a username.
        add        Add a file to the index.
        log        Show commit logs.
        commit     Save changes.
        checkout   Restore a file.
        """.trimIndent()
    )
}

fun handleConfigCommand(args: Array<String>) {
    if (args.size < 2) {
        val username = getUsername()
        if (username.isNotEmpty()) {
            println("The username is $username.")
        } else {
            println("Please, tell me who you are.")
        }
    } else {
        val username = args[1]
        saveUsername(username)
        println("The username is $username.")
    }
}

fun saveUsername(username: String) {
    File("vcs/config.txt").apply {
        parentFile.mkdirs()
        writeText(username)
    }
}

fun getUsername(): String {
    val configFile = File("vcs/config.txt")
    return if (configFile.exists()) configFile.readText() else ""
}

fun handleAddCommand(args: Array<String>) {
    createVcsDirectory()
    if (args.size < 2) {
        listTrackedFiles()
    } else {
        val fileName = args[1]
        val file = File(fileName)
        if (file.exists()) {
            addToIndex(fileName)
            println("The file '$fileName' is tracked.")
        } else {
            println("Can't find '$fileName'.")
        }
    }
}

fun createVcsDirectory() {
    File("vcs").mkdirs()
}

fun listTrackedFiles() {
    val indexFile = File("vcs/index.txt")
    if (indexFile.exists()) {
        val trackedFiles = indexFile.readLines()
        if (trackedFiles.isNotEmpty()) {
            println("Tracked files:")
            trackedFiles.forEach(::println)
        } else {
            println("No tracked files.")
        }
    } else {
        println("Add a file to the index.")
    }
}

fun addToIndex(fileName: String) {
    File("vcs/index.txt").apply {
        parentFile.mkdirs()
        appendText("$fileName\n")
    }
}

fun handleLogCommand() {
    val logFile = File("vcs/log.txt")
    if (logFile.exists()) {
        val commits = logFile.readLines()
        if (commits.isNotEmpty()) {
            commits.forEach(::println)
        } else {
            println("No commits yet.")
        }
    } else {
        println("No commits yet.")
    }
}

fun handleCommitCommand(args: Array<String>) {
    if (args.size < 2) {
        println("Message was not passed.")
    } else {
        val message = args.slice(1 until args.size).joinToString(" ")
        val stagedFiles = getStagedFiles()
        if (stagedFiles.isNotEmpty()) {
            val commitId = generateCommitId()
            val commitDirectory = File("vcs/commits/$commitId").apply { mkdirs() }
            val previousCommitId = getLatestCommitId()
            if (previousCommitId == null || isCommitDifferent(stagedFiles)) {
                stagedFiles.forEach { file ->
                    File(file).copyTo(File(commitDirectory, File(file).name), overwrite = true)
                }
                updateLog(commitId, message)
                println("Changes are committed.")
            } else {
                println("Nothing to commit.")
            }
        } else {
            println("Nothing to commit.")
        }
    }
}

fun getStagedFiles(): List<String> {
    val indexFile = File("vcs/index.txt")
    return if (indexFile.exists()) indexFile.readLines() else emptyList()
}

fun generateCommitId(): String = UUID.randomUUID().toString()

fun updateLog(commitId: String, message: String) {
    val logFile = File("vcs/log.txt")
    val username = getUsername()

    val newEntry = """
        commit $commitId
        Author: $username
        $message
        
    """.trimIndent()

    try {
        if (logFile.exists()) {
            val existingContent = logFile.readText()
            val updatedContent = "$newEntry\n$existingContent"
            logFile.writeText(updatedContent)
        } else {
            logFile.createNewFile()
            logFile.writeText(newEntry)
        }
    } catch (e: FileNotFoundException) {
        println("Log file does not exist.")
    }
}

fun getLatestCommitId(): String? {
    val logFile = File("vcs/log.txt")
    if (logFile.exists()) {
        val commits = logFile.readLines()
        if (commits.isNotEmpty()) {
            val latestCommit = commits.firstOrNull()
            if (latestCommit != null && latestCommit.startsWith("commit ")) {
                return latestCommit.substringAfter("commit ")
            }
        }
    }
    return null
}

fun isCommitDifferent(stagedFiles: List<String>): Boolean {
    val previousCommitId = getLatestCommitId() ?: return true
    val previousCommitDirectory = File("vcs/commits/$previousCommitId")

    for (file in stagedFiles) {
        val currentFile = File(file)
        val previousFile = File(previousCommitDirectory, file)

        if (!previousFile.exists() || !currentFile.hasSameContent(previousFile)) {
            return true
        }
    }

    return false
}

fun File.hasSameContent(other: File): Boolean =
    this.readBytes().contentEquals(other.readBytes())

fun handleCheckoutCommand(args: Array<String>) {
    if (args.size < 2) {
        println("Commit id was not passed.")
    } else {
        val commitId = args[1]
        val commitDirectory = File("vcs/commits/$commitId")
        if (commitDirectory.exists()) {
            val trackedFiles = getTrackedFiles()
            trackedFiles.forEach { file ->
                val sourceFile = File(commitDirectory, file)
                val destinationFile = File(file)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destinationFile, overwrite = true)
                }
            }
            println("Switched to commit $commitId.")
        } else {
            println("Commit does not exist.")
        }
    }
}

fun getTrackedFiles(): List<String> {
    val indexFile = File("vcs/index.txt")
    return if (indexFile.exists()) indexFile.readLines() else emptyList()
}