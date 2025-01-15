package cat.urv.cloudlab.soundless.util.auth

import android.content.Context
import android.net.Uri
import cat.urv.cloudlab.soundless.util.datastate.TokenStatusDataState
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.lang.Exception

class FitbitAuthenticator : PKCEAuthenticator() {

    /**
     * Build an URI to send the OAuth request to, appending all necessary query parameters such
     * as Fitbit scopes, the app code for the Fitbit platform and the code challenge.
     *
     * @param context This is required because generating or retrieving the code verifier needs a
     * valid context.
     *
     * @see getOrCreateCodeVerifier
     */
    override fun generateAuthorizationURI(context: Context): Uri {
        val codeVerifier = getOrCreateCodeVerifier(context)
        val codeChallenge = getCodeChallenge(codeVerifier)

        return Uri.parse("$FITBIT_API_URL/oauth2/authorize").buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", SOUNDLESS_CLIENT_ID)
            .appendQueryParameter("scope", FITBIT_SCOPES)
            .appendQueryParameter("redirect_uri", SOUNDLESS_REDIRECT_URI)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", CODE_CHALLENGE_ALGORITHM_ALIAS)
            .build()
    }

    /**
     * Asynchronous call to Fitbit API to get the user access and refresh tokens.
     */
    override suspend fun getAccessAndRefreshTokensFromAPI(
        authCode: String,
        codeVerifier: String
    ): Flow<TokenStatusDataState> = flow {

        emit(TokenStatusDataState.RequestingTokens)

        // https://dev.fitbit.com/build/reference/web-api/developer-guide/authorization/
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/x-www-form-urlencoded".toMediaType()

        val fitbitAPI = Retrofit.Builder()
            .baseUrl(FITBIT_API_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OAuthFitbitAPI::class.java)

        println("***** REQUESTING ACCESS AND REFRESH TOKENS *****")
        println("Soundless client id: $SOUNDLESS_CLIENT_ID")
        println("Auth code: $authCode")
        println("Code verifier: $codeVerifier")
        println("Grant type: $GRANT_TYPE_AUTHORIZATION")

        try {
            val tokens: Tokens = fitbitAPI.postQueryForAccessAndRefreshTokens(
                SOUNDLESS_CLIENT_ID, authCode, codeVerifier, GRANT_TYPE_AUTHORIZATION, SOUNDLESS_REDIRECT_URI
            )
            emit(TokenStatusDataState.TokensObtained(tokens))
        } catch (e: Exception) {
            emit(TokenStatusDataState.TokensNotObtained(e))
        }
    }

    override suspend fun refreshTokensAPI(
        refreshToken: String
    ): Flow<TokenStatusDataState> = flow {

        emit(TokenStatusDataState.RequestingTokens)

        // https://dev.fitbit.com/build/reference/web-api/authorization/refresh-token/
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/x-www-form-urlencoded".toMediaType()

        val fitbitAPI = Retrofit.Builder()
            .baseUrl(FITBIT_API_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OAuthFitbitAPI::class.java)

        println("***** REFRESHING TOKENS *****")
        println("Soundless client id: $SOUNDLESS_CLIENT_ID")
        println("Refresh token: $refreshToken")
        println("Grant type: $GRANT_TYPE_REFRESH")

        try {
            val tokens: RefreshedTokens = fitbitAPI.postQueryForRefreshingTokens(
                SOUNDLESS_CLIENT_ID, GRANT_TYPE_REFRESH, refreshToken
            )
            emit(TokenStatusDataState.RefreshedTokensObtained(tokens))
        } catch (e: Exception) {
            emit(TokenStatusDataState.TokensNotObtained(e))
        }
    }

    suspend fun renewAccess(context: Context): Flow<TokenStatusDataState> = flow {
        val refreshToken = getRefreshToken(context)
        refreshTokensAPI(refreshToken).onEach { tokenStatusDataState ->
            when (tokenStatusDataState) {
                is TokenStatusDataState.RequestingTokens -> {
                    println("REFRESHING TOKENS...")
                }
                is TokenStatusDataState.RefreshedTokensObtained -> {
                    println("REFRESHED TOKENS: ${tokenStatusDataState.tokens.accessToken}, ${tokenStatusDataState.tokens.refreshToken}")
                    storeTokens(context, tokenStatusDataState.tokens)
                    emit(TokenStatusDataState.AccessRenewed)
                }
                is TokenStatusDataState.TokensNotObtained -> {
                    println("TOKENS NOT REFRESHED...")
                    emit(TokenStatusDataState.AccessNotRenewed(tokenStatusDataState.exception))
                }
                else -> {}
            }
        }.collect()
    }

    suspend fun requestFitbitAccess(
        context: Context,
        fitbitAuthCode: String
    ) {
        val codeVerifier = getOrCreateCodeVerifier(context)
        getAccessAndRefreshTokensFromAPI(fitbitAuthCode, codeVerifier).onEach { tokenStatusDataState ->

            when (tokenStatusDataState) {
                is TokenStatusDataState.RequestingTokens -> {
                    println("REQUESTING TOKENS...")
                }
                is TokenStatusDataState.TokensObtained -> {
                    println("TOKENS: ${tokenStatusDataState.tokens.accessToken}, ${tokenStatusDataState.tokens.refreshToken}")
                    storeTokens(context, tokenStatusDataState.tokens)
                }
                is TokenStatusDataState.TokensNotObtained -> {
                    println("TOKENS NOT OBTAINED...")
                }
                else -> {}
            }
        }.collect()
    }

    companion object {
        const val FITBIT_API_URL = "https://api.fitbit.com"
        const val FITBIT_SCOPES = "sleep heartrate"     // Space-delimited
        const val SOUNDLESS_CLIENT_ID = "XXXXXXXXXX"        // Get from dev.fitbit.com
        const val SOUNDLESS_REDIRECT_URI = "https://soundless.app/redirect_uri_android.html"
        const val CODE_CHALLENGE_ALGORITHM_ALIAS = "XXXXXXXXXX"
        const val GRANT_TYPE_AUTHORIZATION = "authorization_code"
        const val GRANT_TYPE_REFRESH = "refresh_token"
    }

}