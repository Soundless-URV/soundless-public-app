package cat.urv.cloudlab.soundless.util.auth

import retrofit2.http.*

interface OAuthFitbitAPI : OAuthRemoteAPI {

    @FormUrlEncoded
    @POST("/oauth2/token")
    suspend fun postQueryForAccessAndRefreshTokens(
        @Field("client_id") clientID: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") callbackURI: String
    ): Tokens

    @FormUrlEncoded
    @POST("/oauth2/token")
    suspend fun postQueryForRefreshingTokens(
        @Field("client_id") clientID: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String
    ): RefreshedTokens

}