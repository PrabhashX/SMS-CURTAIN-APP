import lk.smscurtains.app.model.CurtainMath
import lk.smscurtains.app.model.Measurement
import kotlin.math.abs

fun assertClose(a: Double, b: Double) { check(abs(a - b) < 0.001) { "Expected $b but got $a" } }
fun main() {
    assertClose(CurtainMath.feet(30.48, "cm"), 1.0)
    assertClose(CurtainMath.feet(12.0, "inch"), 1.0)
    assertClose(CurtainMath.feet(0.3048, "m"), 1.0)
    assertClose(CurtainMath.feet(2.0, "ft"), 2.0)

    val w = listOf(
        Measurement("Single", 120.0, "cm", openings = 1, frames = 2),
        Measurement("Triple", 2.0, "m", openings = 3, frames = 3)
    )
    val d = listOf(Measurement("Large", 36.0, "inch", openings = 1, frames = 2))
    val t = CurtainMath.calculate(w, d, 1.0, 420.0, 10.0, 40.0, 350.0, 150.0, 30.0, 20.0)
    assertClose(t.windowFeet, (120.0 / 30.48) * 2 + (2.0 / 0.3048) * 3)
    assertClose(t.doorFeet, 6.0)
    assertClose(t.grandFeet, t.windowFeet + 6.0)
    assertClose(t.clothMetres, t.grandFeet + 1.0)
    check(t.eyelets == 208)
    check(t.windowBrackets == 13)
    check(t.doorBrackets == 4)
    check(t.brackets == 17)
    check(t.endcaps == 14)
    assertClose(t.materialTotal, CurtainMath.round2(t.clothMetres * 420.0))
    assertClose(t.stripTotal, CurtainMath.round2(t.clothMetres * 40.0))
    assertClose(t.sewingTotal, CurtainMath.round2(t.clothMetres * 350.0))
    assertClose(t.woodTotal, CurtainMath.round2(t.grandFeet * 150.0))
    assertClose(t.bracketTotal, 17 * 30.0)
    assertClose(t.endcapTotal, 14 * 20.0)
    assertClose(t.grandTotal, listOf(t.materialTotal,t.eyeletTotal,t.stripTotal,t.sewingTotal,t.woodTotal,t.bracketTotal,t.endcapTotal).sum())

    val fourThreshold = CurtainMath.calculate(listOf(Measurement("Triple", 1.0, "ft", openings = 3, frames = 2)), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, tripleAtThree = false)
    check(fourThreshold.brackets == 4)
    check(fourThreshold.endcaps == 4)
    val defaultThreshold = CurtainMath.calculate(listOf(Measurement("Triple", 1.0, "ft", openings = 3, frames = 2)), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    check(defaultThreshold.brackets == 6)
    val onlyDoors = CurtainMath.calculate(emptyList(), listOf(Measurement("Arch", 5.0, "ft", frames = 1)), 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    assertClose(onlyDoors.grandFeet, 5.0)
    assertClose(onlyDoors.clothMetres, 5.5)
    check(onlyDoors.brackets == 2 && onlyDoors.endcaps == 2)
    check(onlyDoors.eyelets == 33)
    println("PASS: unit conversions, totals, 1-ft-to-1-m shop rule, eyelets, window/door accessories, 3-vs-4 threshold, bills")
}
