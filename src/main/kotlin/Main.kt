package tech.pedrosa

import tech.pedrosa.Exercise.*
import java.io.BufferedWriter
import java.io.File
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class Person(val name: String, val weekDays: List<Weekday>, val exercises: Map<LocalDate, Exercise?>)
enum class Exercise { MAT, TRX, CADILLAC, REFORMER, CHAIR, BARREL, NP }
enum class Weekday(val dayOfWeek: DayOfWeek) {
    SEG(MONDAY), TER(TUESDAY), QUA(WEDNESDAY), QUI(THURSDAY), SEX(FRIDAY), SAB(SATURDAY), DOM(SUNDAY);
}

private const val DELIMITER = ";"
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun main() {
    val filePath = "test.csv"
    processFile(filePath)
}

fun processFile(filePath: String): List<Person> {
    val lines = readFile(filePath)
    val days = extractDays(lines)
    val people = parsePeople(lines, days)
    writeNewFile("new.csv", createNewHeader(lines), formatNewLines(people))
    return people
}

private fun readFile(filePath: String): List<String> = File(filePath).readLines()

private fun extractDays(lines: List<String>): List<LocalDate> =
    lines.first().split(DELIMITER).drop(2).map { it.toLocalDate() }

private fun parsePeople(lines: List<String>, days: List<LocalDate>): List<Person> =
    lines.drop(1).map { parsePerson(it, days) }

private fun parsePerson(line: String, days: List<LocalDate>): Person {
    val fields = line.split(DELIMITER).map { it.trim().uppercase(Locale.getDefault()) }
    val name = fields.first()
    val weekDays = parseWeekDays(fields[1])
    val exercises = parseExercises(days, fields.drop(2))
    return Person(name, weekDays, calculateNextExercise(weekDays, exercises))
}

private fun parseWeekDays(weekDaysField: String): List<Weekday> =
    weekDaysField.split(",").map { Weekday.valueOf(it) }

private fun parseExercises(days: List<LocalDate>, exerciseFields: List<String>): Map<LocalDate, Exercise?> =
    days.zip(exerciseFields.map { Exercise.valueOf(it) }).toMap()

private fun calculateNextExercise(
    weekDays: List<Weekday>,
    exercises: Map<LocalDate, Exercise?>
): Map<LocalDate, Exercise?> {
    val lastExerciseDate = exercises.keys.maxOrNull()?.minusMonths(1) ?: LocalDate.now()
    val nextDay = exercises.keys.maxOrNull()?.plusDays(1) ?: LocalDate.now()
    val nextExercise = exercises.filterKeys { it.isAfter(lastExerciseDate) }
        .values.firstOrNull { it != NP } ?: NP
    return exercises + (nextDay to if (weekDays.any { it.dayOfWeek == nextDay.dayOfWeek }) nextExercise else NP)
}

private fun createNewHeader(lines: List<String>): String {
    val lastDate = lines.first().split(DELIMITER).last().toLocalDate()
    val nextDate = lastDate.takeIf { it > LocalDate.now() } ?: lastDate.plusDays(1)
    return "${lines.first()}$DELIMITER${dateFormatter.format(nextDate)}"
}

private fun formatNewLines(people: List<Person>): List<String> =
    people.map { formatPersonLine(it) }

private fun formatPersonLine(person: Person): String =
    "${person.name}$DELIMITER${person.weekDays.joinToString(",")}$DELIMITER${
        person.exercises.values.joinToString(
            DELIMITER
        )
    }"

private fun writeNewFile(filePath: String, header: String, lines: List<String>) {
    File(filePath).bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write(header)
        writer.newLine()
        lines.forEach { writer.writeLine(it) }
    }
}

private fun BufferedWriter.writeLine(line: String) {
    this.write(line)
    this.newLine()
}

private fun String.toLocalDate(): LocalDate = LocalDate.parse(this, dateFormatter)
