import org.junit.Test


enum class A {
    A, B, C
}

class EnumTest {

    @Test
    fun testReflectEnumValueGet() {
        println("qwq")
        println(java.lang.Enum.valueOf(A::class.java,"B"))
    }
}