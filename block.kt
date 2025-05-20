sealed class Block {  // базовый блок типа алгоритма
    data class VarDecl(var names: String = "", var hasError: Boolean = false) : Block()  // блок объявления переменных

    data class Assignment(var varName: String = "", var expression: String = "", var hasError: Boolean = false) : Block()  // блок присваивания

    data class IfBlock(  // блок условия
        var leftExpr: String = "",  // выражение слева
        var op: String = "",  // оператор сравнения
        var rightExpr: String = "",  // выражение справа
        var hasError: Boolean = false  // признак ошибки
    ) : Block()
}




fun evaluateExpression(expr: String, variables: Map<String, Double>): Double {  // функция по вычислению ариф. выражения с учетом переменных
    data class Data(val rest: List<Char>, val value: Double)  // рекурсивные функции для разбора выражений

    val chars = expr.filter { !it.isWhitespace() }.toList()  // убираем пробелы, делаем из строки список символов

    fun parseExpression(chars: List<Char>): Data {
        var (rest, value) = parseTerm(chars)
        while (rest.isNotEmpty()) {  // цикл обработки сложения/вычитания
            when (rest.first()) {
                '+' -> {
                    val next = parseTerm(rest.drop(1))
                    rest = next.rest
                    value += next.value
                }
                '-' -> {
                    val next = parseTerm(rest.drop(1))
                    rest = next.rest
                    value -= next.value
                }
                else -> return Data(rest, value)
            }
        }
        return Data(rest, value)
    }

    fun parseTerm(chars: List<Char>): Data {
        var (rest, value) = parseFactor(chars)
        while (rest.isNotEmpty()) {  // цикл обработки умножения/деления
            when (rest.first()) {
                '*' -> {
                    val next = parseFactor(rest.drop(1))
                    rest = next.rest
                    value *= next.value
                }
                '/' -> {
                    val next = parseFactor(rest.drop(1))
                    rest = next.rest
                    value /= next.value
                }
                else -> return Data(rest, value)
            }
        }
        return Data(rest, value)
    }

    fun parseFactor(chars: List<Char>): Data {
        val first = chars.firstOrNull() ?: throw RuntimeException("Пустой фактор")
        return when {  // унарный плюс/минус и скобки обрабатывать
            first == '+' -> parseFactor(chars.drop(1))  // скипаем '+'
            first == '-' -> {
                val next = parseFactor(chars.drop(1))
                Data(next.rest, -next.value)  // знак меняем
            }
            first == '(' -> {
                val inside = parseExpression(chars.drop(1))  // если "(", то парсим внутри скобок
                if (inside.rest.isEmpty() || inside.rest.first() != ')') throw RuntimeException("Отсутствует закрывающая скобка")
                Data(inside.rest.drop(1), inside.value)
            }
            first.isDigit() || first == '.' -> {  // число (цифры или десятичная точка)
                val numberChars = chars.takeWhile { it.isDigit() || it == '.' }
                val numStr = numberChars.joinToString("")
                val numValue = numStr.toDoubleOrNull() ?: throw RuntimeException("Неверный формат числа")
                Data(chars.drop(numberChars.size), numValue)
            }
            first.isLetter() -> {  // переменная (сначала буквы, потом буквы/цифры)
                val nameChars = chars.takeWhile { it.isLetterOrDigit() }
                val name = nameChars.joinToString("")
                val varValue = variables[name] ?: throw RuntimeException("Неизвестная переменная: $name")  // значение переменной получаем из словаря
                Data(chars.drop(nameChars.size), varValue)
            }
            else -> throw RuntimeException("Непредвидимый символ: $first")
        }
    }

    val result = parseExpression(chars)  // разбор всего выражения (проверяем, что лишних символов не осталось)
    if (result.rest.isNotEmpty()) throw RuntimeException("Неожиданный символ: ${result.rest.first()}")
    return result.value
}




fun runAlgorithm(blocks: List<Block>): Map<String, Double> {  // функция выполнения алгоритма на основании списка блоков
    val variables = mutableMapOf<String, Double>()  // словарь для значений переменных
    var i = 0
    while (i < blocks.size) {
        val block = blocks[i]
        try {
            when (block) {
                is Block.VarDecl -> {
                    val names = block.names.split(",").map { it.trim() }.filter { it.isNotEmpty() }  // разбор строки с именами переменных через запятую
                    if (names.isEmpty()) throw RuntimeException("Ни одной переменной не введено")
                    for (name in names) {
                        if (!name.matches(Regex("[a-zA-Z]+"))) {
                            throw RuntimeException("Неверное имя переменной: $name")  // проверяем, чтобы имя состояло только из букв
                        }
                        variables[name] = 0.0  // если уже объявляли переменную, можно игнорировать
                    }
                }
                is Block.Assignment -> {
                    val varName = block.varName.trim()  // присваивание переменной выражения
                    if (!variables.containsKey(varName)) {
                        throw RuntimeException("Переменная '$varName' не объявлена")
                    }
                    val value = evaluateExpression(block.expression, variables)  // вычисляем значение выражения
                    variables[varName] = value  // результат сохраняем
                }
                is Block.IfBlock -> {
                    val left = evaluateExpression(block.leftExpr, variables)  // сравниваем два выражения через оператор (условие)
                    val right = evaluateExpression(block.rightExpr, variables)
                    val cond = when (block.op.trim()) {
                        ">"  -> left > right
                        "<"  -> left < right
                        "="  -> left == right
                        "==" -> left == right
                        "!=" -> left != right
                        ">=" -> left >= right
                        "<=" -> left <= right
                        else -> throw RuntimeException("Неверный оператор: ${block.op}")
                    }
                    if (!cond) {
                        if (i + 1 < blocks.size) {  // когда ложное условие, скипаем некст блок (по логике if)
                            i += 1  // скипаем некст блок
                        }
                    }
                }
            }
        } catch (e: Exception) {
            block.hasError = true  // если вылезла ошибка, блок отмечаем и стопаем выполнение
            break
        }
        i += 1
    }
    return variables
}
