package core

import core.commands.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

var userName: String? = null

val inputStream = PipedInputStream()
val reader = inputStream.bufferedReader()
private val outputStream = PrintStream(PipedOutputStream(inputStream), true)

private val consoleInput = PipedInputStream()
private val consoleReader = consoleInput.bufferedReader()
val consoleWriter = PrintStream(PipedOutputStream(consoleInput), true)


suspend fun ConsoleInterface.newPrompt(promptText: String, value: String = ""): String = withContext(Dispatchers.IO) {
    prompt(promptText, value)
    return@withContext consoleReader.readLine()
}

var job: Job? = null
const val version="1.0e-500"//1.0 × 10^-500
suspend fun initialize(consoleInterface: ConsoleInterface) {
    outputStream.println(
        """
        EseLinux Shell ver.$version
        """.trimIndent()
    )
    do {
        if (userName != null) outputStream.println("使用できる文字は0~9の数字とアルファベットと一部記号です")
        userName = consoleInterface.newPrompt("あなたの名前は？:")
    } while (userName.isNullOrBlank() || !userName.orEmpty().matches(Regex("[0-9A-z]+")))
    outputStream.println("Hello $userName")
    CommandManager.initialize(
        outputStream, consoleReader, consoleInterface,
        ListFile, CD, Cat, Exit, SugoiUserDo,
        Yes, Clear, Echo,Remove
    )
    while (true/*TODO 終了機能*/) {
        val input = consoleInterface.newPrompt("$userName:${LocationManager.currentPath.value}>").ifBlank {
            null
        } ?: continue
        val inputArgs = input.split(' ')
        val cmd = CommandManager.tryResolve(inputArgs.first())
        commandHistoryImpl.add(0, input)
        if (cmd != null) {
            withContext(Dispatchers.Default) {
                job = launch {
                    cmd.execute(inputArgs.drop(1))
                }
                job?.join()
                job = null
            }
        } else {
            outputStream.println(
                """
            そのようなコマンドはありません。
            help と入力するとヒントが得られるかも・・・？
            """.trimIndent()
            )
        }

    }
}

private val commandHistoryImpl = mutableListOf<String>()
val commandHistory get() = commandHistoryImpl.toList()

object Variable {
    val map = mutableMapOf<String, Any>()
}

/**キャンセルされた場合は[true]*/
fun cancelCommand(): Boolean {
    job?.cancel() ?: return false

    outputStream.println("Ctrl+Cによってキャンセルされました")
    return true
}