package lk.smscurtains.app.model

import kotlin.math.ceil
import kotlin.math.round

/** The shop's fabric rule is not a physical unit conversion: 1 measured ft -> 1 fabric metre. */
object CurtainMath {
    fun feet(value: Double, unit: String): Double = when (unit) {
        "cm" -> value / 30.48
        "m" -> value / 0.3048
        "inch" -> value / 12.0
        else -> value
    }

    fun round2(value: Double): Double = round(value * 100.0) / 100.0

    fun calculate(
        windows: List<Measurement>, doors: List<Measurement>, extraMetres: Double,
        materialRate: Double, eyeletRate: Double, stripRate: Double,
        sewingRate: Double, woodRate: Double, bracketRate: Double,
        endcapRate: Double, tripleAtThree: Boolean = true
    ): Totals {
        val windowFeet = windows.sumOf { feet(it.length, it.unit) * it.frames }
        val doorFeet = doors.sumOf { feet(it.length, it.unit) * it.frames }
        val grandFeet = windowFeet + doorFeet
        val clothMetres = grandFeet + extraMetres
        val eyelets = ceil(clothMetres * 6.0 - 1e-9).toInt()
        val windowBrackets = windows.sumOf {
            (if (if (tripleAtThree) it.openings >= 3 else it.openings > 3) 3 else 2) * it.frames
        }
        val doorBrackets = doors.sumOf { 2 * it.frames }
        val endcaps = (windows.sumOf { it.frames } + doors.sumOf { it.frames }) * 2
        val materialTotal = round2(clothMetres * materialRate)
        val eyeletTotal = round2(eyelets * eyeletRate)
        val stripTotal = round2(clothMetres * stripRate)
        val sewingTotal = round2(clothMetres * sewingRate)
        val woodTotal = round2(grandFeet * woodRate)
        val bracketTotal = round2((windowBrackets + doorBrackets) * bracketRate)
        val endcapTotal = round2(endcaps * endcapRate)
        return Totals(
            windowFeet, doorFeet, grandFeet, clothMetres, eyelets,
            windowBrackets, doorBrackets, endcaps,
            materialTotal, eyeletTotal, stripTotal, sewingTotal,
            woodTotal, bracketTotal, endcapTotal,
            round2(materialTotal + eyeletTotal + stripTotal + sewingTotal + woodTotal + bracketTotal + endcapTotal)
        )
    }
}

data class Measurement(
    val type: String,
    val length: Double,
    val unit: String,
    val height: Double? = null,
    val heightUnit: String = "ft",
    val openings: Int = 1,
    val frames: Int = 1
)

data class Totals(
    val windowFeet: Double, val doorFeet: Double, val grandFeet: Double,
    val clothMetres: Double, val eyelets: Int,
    val windowBrackets: Int, val doorBrackets: Int, val endcaps: Int,
    val materialTotal: Double, val eyeletTotal: Double, val stripTotal: Double,
    val sewingTotal: Double, val woodTotal: Double, val bracketTotal: Double,
    val endcapTotal: Double, val grandTotal: Double
) {
    val brackets: Int get() = windowBrackets + doorBrackets
}
