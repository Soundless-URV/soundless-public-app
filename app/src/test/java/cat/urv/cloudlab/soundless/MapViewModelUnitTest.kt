package cat.urv.cloudlab.soundless

/*
    Class to test some methods without the Firebase initialize error
 */
class MapViewModelUnitTest {
    private var lastDate = ""

    fun setLastDate(lastDate: String){
        this.lastDate = lastDate
    }

    fun getLastDate(): String {
        return lastDate
    }

    // Method to get the name of the file to download
    fun getNameFile(dateSelected: Int, cityFilename: String): String {
        val dateFilename = when(dateSelected){
            0 -> "yesterday"
            1 -> "week"
            2 -> "month"
            else -> "yesterday"
        }

        return cityFilename + "_" + lastDate + "_" + dateFilename + ".html"
    }
}