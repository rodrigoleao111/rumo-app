package com.rodrigoleao.pipa.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renderizador de markdown **leve** (sem dependências externas) para o que a IA
 * costuma devolver: negrito, itálico, código inline, links, listas com marcador,
 * listas numeradas e cabeçalhos. Cada linha vira um bloco; o inline é convertido
 * para [AnnotatedString]. Usado no chat de criação e no detalhe de conversas.
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fontSize    = 14.sp
    val lineHeight  = 20.sp
    val bulletRegex = remember { Regex("^\\s*[-*+]\\s+(.*)") }
    val numberRegex = remember { Regex("^\\s*(\\d+)[.)]\\s+(.*)") }
    val headerRegex = remember { Regex("^\\s*(#{1,6})\\s+(.*)") }

    Column(modifier = modifier) {
        text.trim('\n').split("\n").forEach { raw ->
            val line = raw.trimEnd()
            val bullet = bulletRegex.find(line)
            val number = numberRegex.find(line)
            val header = headerRegex.find(line)
            when {
                line.isBlank() -> Spacer(Modifier.height(6.dp))

                header != null -> Text(
                    text       = parseInlineMarkdown(header.groupValues[2], color),
                    color      = color,
                    fontSize   = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                bullet != null -> Row {
                    Text("•  ", color = color, fontSize = fontSize, lineHeight = lineHeight)
                    Text(parseInlineMarkdown(bullet.groupValues[1], color), color = color, fontSize = fontSize, lineHeight = lineHeight)
                }

                number != null -> Row {
                    Text("${number.groupValues[1]}.  ", color = color, fontSize = fontSize, lineHeight = lineHeight, fontWeight = FontWeight.Medium)
                    Text(parseInlineMarkdown(number.groupValues[2], color), color = color, fontSize = fontSize, lineHeight = lineHeight)
                }

                else -> Text(parseInlineMarkdown(line, color), color = color, fontSize = fontSize, lineHeight = lineHeight)
            }
        }
    }
}

/** Converte marcações inline (`**negrito**`, `*itálico*`, `` `código` ``, `[texto](url)`) em AnnotatedString. */
private fun parseInlineMarkdown(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = text.length
    while (i < n) {
        val c = text[i]
        when {
            c == '*' && i + 1 < n && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else { append(c); i++ }
            }
            c == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(c); i++ }
            }
            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(c); i++ }
            }
            c == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                val hasParen = closeBracket != -1 && closeBracket + 1 < n && text[closeBracket + 1] == '('
                val closeParen = if (hasParen) text.indexOf(')', closeBracket + 2) else -1
                if (hasParen && closeParen != -1) {
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append(text.substring(i + 1, closeBracket))
                    }
                    i = closeParen + 1
                } else { append(c); i++ }
            }
            else -> { append(c); i++ }
        }
    }
}
