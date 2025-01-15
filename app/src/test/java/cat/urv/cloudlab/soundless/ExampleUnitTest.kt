package cat.urv.cloudlab.soundless

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun getLastDateNotInitialised() {
        assertEquals(MapViewModelUnitTest().getLastDate(), "")
    }

    @Test
    fun getNameFileNotInitialised() {
        assertEquals(MapViewModelUnitTest().getNameFile(1, "Tarragona"), "Tarragona__week.html")
    }

    @Test
    fun getLastDateInitialised() {
        val viewModel = MapViewModelUnitTest()
        viewModel.setLastDate("8-10-2022")
        assertEquals(viewModel.getLastDate(), "8-10-2022")
    }

    @Test
    fun getNameFileInitialised() {
        val viewModel = MapViewModelUnitTest()
        viewModel.setLastDate("8-10-2022")
        assertEquals(viewModel.getNameFile(2, "Reus"), "Reus_8-10-2022_month.html")
    }
}