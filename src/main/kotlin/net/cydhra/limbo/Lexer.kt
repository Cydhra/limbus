package net.cydhra.limbo

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

class Lexer private constructor(private val inputStream: InputStream) : Iterator<Token>, Closeable, AutoCloseable {

    companion object {
        private val whitespace = arrayOf(' ', '\r', '\n', '\t')

        fun fromFile(file: File) = Lexer(UnicodeBOMInputStream(file.inputStream()))

        fun fromInputStream(inputStream: InputStream) = Lexer(UnicodeBOMInputStream(inputStream))
    }

    /**
     * A reader for the [inputStream] that was given to the lexer
     */
    private val reader = inputStream.reader(Charset.forName("UTF-8"))

    /**
     * The next character in the input stream. If this is `null` then the stream ends after the current character.
     */
    private var peekChar: Char? = reader.read().takeIf { it >= 0 }?.toChar()
            ?: error("could not peek into stream: stream is empty")

    /**
     * The character in the input stream that comes after [peekChar]. If this is `null` then the stream ends after
     * the peekChar character.
     */
    private var peekNextChar: Char? = reader.read().takeIf { it >= 0 }?.toChar()

    /**
     * The current, already read character from the input stream.
     *
     * It is supposed to be uninitialized before the first read, so the contract must be, that it is initialized
     * before reading.
     */
    private var currentChar: Char = Char.MIN_VALUE

    /**
     * Advance the scanner by one character. If no more character is available, `false` is returned.
     */
    private fun advanceLexer(): Boolean {
        currentChar = peekChar ?: return false
        peekChar = peekNextChar
        peekNextChar = reader.read().takeIf { it >= 0 }?.toChar()
        return true
    }

    override fun hasNext(): Boolean {
        return peekChar != null
    }

    /**
     * Read one token from the input stream. Throws an exception, if no more tokens are available.
     */
    override fun next(): Token {
        check(advanceLexer()) { "cannot read next token: end of stream" }

        return when {
            currentChar == '+' -> readPlusToken()
            currentChar == '-' -> readMinusToken()
            currentChar == '*' -> TimesToken
            currentChar == '/' -> readSlashToken()
            currentChar.isDigit() -> readNumericLiteral()
            currentChar in whitespace -> readWhitespace()
            else -> ErroneousToken(currentChar.toString())
        }
    }

    private fun readWhitespace(): Token {
        val buffer = StringBuilder()
        buffer.append(currentChar)

        while (hasNext()) {
            if (peekChar in whitespace) {
                advanceLexer()
                buffer.append(currentChar)
            } else {
                break
            }
        }

        return WhitespaceToken(buffer.toString())
    }

    private fun readPlusToken(): Token {
        return if (peekChar == '+') {
            advanceLexer()
            PlusPlusToken
        } else {
            PlusToken
        }
    }

    private fun readMinusToken(): Token {
        return if (peekChar == '-') {
            advanceLexer()
            MinusMinusToken
        } else {
            MinusToken
        }
    }

    private fun readSlashToken(): Token {
        return if (peekChar == '/') {
            readComment()
        } else {
            DivideToken
        }
    }

    private fun readComment(): Token {
        TODO()
    }

    /**
     * Read any numeric literal. This encompasses integer as well as floating point literals. This method
     */
    private fun readNumericLiteral(): Token {
        if (currentChar == '0') {
            when (peekChar!!) {
                'x' -> return this.readHexadecimalIntegerLiteral()
                'b' -> return this.readBinaryIntegerLiteral()
                'o' -> return this.readOctalIntegerLiteral()
            }
        }

        val literal = StringBuilder()
        literal.append(currentChar)

        while (peekChar != null) {
            when {
                peekChar!!.isDigit() -> this.advanceLexer()
                peekChar!! == '_' -> this.advanceLexer()
                peekChar!! == '.' -> return this.readFloatingPointLiteral(literal)
                else -> break
            }

            literal.append(currentChar)
        }

        return IntegerLiteral(literal.toString())
    }

    /**
     * Read hexadecimal integers.
     *
     * @see readSpecialIntegerLiteral
     */
    private fun readHexadecimalIntegerLiteral(): Token {
        return readSpecialIntegerLiteral {
            return@readSpecialIntegerLiteral when {
                peekChar!!.isDigit() -> true
                peekChar!! == '_' -> true

                // is between A-F inclusive
                peekChar!! >= 65.toChar() && peekChar!! <= 70.toChar() -> true

                // is between a-f inclusive
                peekChar!! >= 97.toChar() && peekChar!! <= 102.toChar() -> true
                else -> false
            }
        }
    }

    /**
     * Read binary integers.
     *
     * @see readSpecialIntegerLiteral
     */
    private fun readBinaryIntegerLiteral(): Token {
        return readSpecialIntegerLiteral {
            return@readSpecialIntegerLiteral when {
                peekChar!! == '0' || peekChar!! == '1' -> true
                peekChar!! == '_' -> true
                else -> false
            }
        }
    }

    /**
     * Read octal integers.
     *
     * @see readSpecialIntegerLiteral
     */
    private fun readOctalIntegerLiteral(): Token {
        return readSpecialIntegerLiteral {
            return@readSpecialIntegerLiteral when {
                // is between 0 and 7 inclusive
                peekChar!! >= 48.toChar() && peekChar!! <= 55.toChar() -> true
                peekChar!! == '_' -> true
                else -> false
            }
        }
    }

    /**
     * Read an integer literal in a different positional system. The [currentChar] is required to be `0` and the
     * `peekChar` is required to be the special character for a different representation.
     *
     * @param acceptor which characters to accept as part of the special representation. Whenever this acceptor
     * returns true, the lexer will advance by one and add the character to the current token.
     */
    private inline fun readSpecialIntegerLiteral(crossinline acceptor: (Char) -> Boolean): Token {
        val literal = StringBuilder()

        // consume the 0 and advance thus the special character ('x', 'b', 'o') is current char
        literal.append(currentChar)

        // consume it
        this.advanceLexer()
        literal.append(currentChar)

        // check if there is more to this literal. If there isn't, the literal is illegal and we fail early
        if (!acceptor(peekChar!!)) {
            return ErroneousToken(literal.toString())
        }

        // read the remaining literal
        while (peekChar != null) {
            if (acceptor(peekChar!!)) {
                this.advanceLexer()
            } else {
                break
            }

            literal.append(currentChar)
        }

        // TODO read suffixes

        return IntegerLiteral(literal.toString())
    }

    private fun readFloatingPointLiteral(literal: StringBuilder): Token {
        // only if the character after the dot is a number, this is a floating point number. Otherwise the dot is
        // likely part of a method call
        when {
            peekNextChar?.isDigit() ?: false -> this.advanceLexer()
            else -> return IntegerLiteral(literal.toString())
        }

        literal.append(currentChar)

        while (peekChar != null) {
            when {
                peekChar!!.isDigit() -> this.advanceLexer()
                peekChar!! == '_' -> this.advanceLexer()
                else -> break
            }

            literal.append(currentChar)
        }

        return FloatingPointLiteral(literal.toString())
    }

    override fun close() {
        this.reader.close()
    }
}