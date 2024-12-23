package test.rekord

import java.lang.reflect.Method



fun parseFileName(method: Method): String {
    val packageName = method.declaringClass.packageName
    val path = packageName.toPath()

    val className = method.declaringClass.canonicalName.replace(packageName + ".", "")
    val testMethodName = method.name
    val filename = "$className.$testMethodName".snakeCase()
    return path + filename
}

private fun String.toPath() = replace(".", "/") + "/".lowercase()

private fun String.snakeCase() = replace(Regex("([A-Z])"), "_$1")
    .replace(Regex("\\W"), "_")
    .replace(Regex("^_"), "") // in case of 1st letter is uppercase, extra _ to remove. ex : _Recorder_Test
    .replace(Regex("__"), "_") // in case of '.W' replaced by '__'
    .lowercase()
