package cat.urv.cloudlab.soundless.model.repository.health.fitbit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface FitbitHealthAPI {

    @GET("/1/user/{user-id}/activities/heart/date/{start-date}/{end-date}/{detail-level}/time/{start-time}/{end-time}.json")
    suspend fun getHeartRate(
        @Path(value="user-id") userID: String,
        @Path(value="start-date") startDate: String,
        @Path(value="end-date") endDate: String,
        @Path(value="detail-level") detailLevel: String,
        @Path(value="start-time") startTime: String,
        @Path(value="end-time") endTime: String,
        @Header(value="authorization") token: String
    ): FitbitHeartRateEntity

    /**
     * From Fitbit dev docs:
     *
     * This endpoint returns a list of a user's sleep log entries for a date range. The data
     * returned for either date can include a sleep period that ended that date but began on the
     * previous date. For example, if you request a sleep log between 2021-12-22 and 2021-12-26, it
     * may return log entries that span 2021-12-21 and 2021-12-22, as well as 2021-12-25 and
     * 2021-12-26.
     */
    @GET("/1.2/user/{user-id}/sleep/date/{start-date}/{end-date}.json")
    suspend fun getSleep(
        @Path(value="user-id") userID: String,
        @Path(value="start-date") startDate: String,
        @Path(value="end-date") endDate: String,
        @Header(value="authorization") token: String
    ): FitbitSleepEntity

}