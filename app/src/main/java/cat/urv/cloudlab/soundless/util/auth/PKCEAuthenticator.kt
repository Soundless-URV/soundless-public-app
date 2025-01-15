package cat.urv.cloudlab.soundless.util.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.util.datastate.TokenStatusDataState
import cat.urv.cloudlab.soundless.view.activity.MainActivity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles all the OAuth PKCE flow tasks for authorization and token retrieval. Followed the guide:
 * https://technology.doximity.com/articles/improving-android-s-auth-flow-with-pkce
 */
abstract class PKCEAuthenticator {

    /**
     * If another code verifier has been created and stored into [EncryptedSharedPreferences], then
     * return it. Otherwise create a new one and store it into [EncryptedSharedPreferences].
     *
     * @param context Context from which to open the [EncryptedSharedPreferences]
     * @return The code verifier in store, if any, or a newly created one.
     */
    protected fun getOrCreateCodeVerifier(context: Context): String {
        val sharedPreferences = getEncryptedSharedPreferences(context)

        val codeVerifier = if (sharedPreferences.contains(CODE_VERIFIER_KEY)) {
            sharedPreferences.getString(CODE_VERIFIER_KEY, "")!!
        } else {
            val secureRandom = SecureRandom()
            val bytes = ByteArray(64)
            secureRandom.nextBytes(bytes)
            val newCodeVerifier = Base64.encodeToString(bytes, ENCODING)
            // Save new code verifier to EncryptedSharedPreferences
            sharedPreferences.edit().putString(CODE_VERIFIER_KEY, newCodeVerifier).apply()
            newCodeVerifier
        }

        return codeVerifier
    }

    /**
     * Given a code verifier, get its corresponding code challenge. Notice the code challenge is not
     * stored anywhere (as opposed to the code verifier being saved in an encrypted store) and is
     * computed every time you call this method.
     *
     * @return Code challenge, i.e. SHA-256 hash of the code verifier
     */
    protected fun getCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray()
        val messageDigest = MessageDigest.getInstance(CODE_CHALLENGE_ALGORITHM)
        messageDigest.update(bytes)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, ENCODING)
    }

    abstract fun generateAuthorizationURI(context: Context): Uri

    protected abstract suspend fun getAccessAndRefreshTokensFromAPI(
        authCode: String, codeVerifier: String
    ): Flow<TokenStatusDataState>

    abstract suspend fun refreshTokensAPI(
        refreshToken: String
    ): Flow<TokenStatusDataState>

    /**
     * Save access and refresh tokens in [EncryptedSharedPreferences].
     *
     * @param tokens Access and refresh tokens, plus other metadata
     */
    protected fun storeTokens(context: Context, tokens: Tokens) {
        val sharedPreferences = getEncryptedSharedPreferences(context)

        // tokens.expiresIn (seconds) - 3600 = 1h before expiring
        // Multiply by 1000 to make it milliseconds and add to time of storing
        val mustRefreshTimestamp = System.currentTimeMillis() + (tokens.expiresIn - 3600) * 1000

        sharedPreferences
            .edit()
            .putString(ACCESS_TOKEN_KEY, tokens.accessToken)
            .putString(REFRESH_TOKEN_KEY, tokens.refreshToken)
            .putString(USER_ID_KEY, tokens.userID)
            .putLong(MUST_REFRESH_TIMESTAMP_KEY, mustRefreshTimestamp)
            .apply()
    }

    /**
     * Save access and refresh tokens in [EncryptedSharedPreferences].
     *
     * @param tokens Access and refresh tokens, plus other metadata
     */
    protected fun storeTokens(context: Context, tokens: RefreshedTokens) {
        val sharedPreferences = getEncryptedSharedPreferences(context)

        // tokens.expiresIn (seconds) - 3600 = 1h before expiring
        // Multiply by 1000 to make it milliseconds and add to time of storing
        val mustRefreshTimestamp = System.currentTimeMillis() + (tokens.expiresIn - 3600) * 1000

        sharedPreferences
            .edit()
            .putString(ACCESS_TOKEN_KEY, tokens.accessToken)
            .putString(REFRESH_TOKEN_KEY, tokens.refreshToken)
            .putString(USER_ID_KEY, tokens.userID)
            .putLong(MUST_REFRESH_TIMESTAMP_KEY, mustRefreshTimestamp)
            .apply()
    }

    /**
     * Checks if Soundless has an access and refresh tokens available, which means successful
     * authentication.
     */
    fun isAccessGranted(context: Context): Boolean {
        val sharedPreferences = getEncryptedSharedPreferences(context)

        return sharedPreferences.contains(ACCESS_TOKEN_KEY)
                && sharedPreferences.contains(REFRESH_TOKEN_KEY)
                && sharedPreferences.contains(USER_ID_KEY)
    }

    /**
     * Convenience method to avoid code repetition.
     */
    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            CODE_ENCRYPTED_STORE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Retrieves Access token from the token storage.
     *
     * @throws NotAuthenticatedYetException if user has not been authenticated before
     * @throws InvalidTokenException if Access token is empty, blank or corrupted
     * @throws TokenNeedsRefreshException if Access token has expired
     * @return Access token
     */
    fun getAccessToken(context: Context): String {
        if (!isAccessGranted(context)) throw NotAuthenticatedYetException()
        if (hasAccessTokenExpired(context)) throw TokenNeedsRefreshException()

        val sharedPreferences = getEncryptedSharedPreferences(context)
        val accessToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, "").orEmpty()

        if (accessToken.isEmpty() || accessToken.isBlank()) throw InvalidTokenException()
        return accessToken
    }

    private fun hasAccessTokenExpired(context: Context): Boolean {
        val sharedPreferences = getEncryptedSharedPreferences(context)

        val mustRefreshTimestamp = sharedPreferences.getLong(MUST_REFRESH_TIMESTAMP_KEY, 0L)
        return mustRefreshTimestamp < System.currentTimeMillis()
    }

    fun resetAccess(context: Context) {
        val sharedPreferences = getEncryptedSharedPreferences(context)

        sharedPreferences
            .edit()
            .remove(AUTHORIZATION_CODE_KEY)
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(USER_ID_KEY)
            .apply()
    }

    /**
     * Retrieves Refresh token from the token storage.
     *
     * @throws NotAuthenticatedYetException if user has not been authenticated before
     * @throws InvalidTokenException if token is empty, blank or corrupted
     * @return Refresh token
     */
    fun getRefreshToken(context: Context): String {
        if (!isAccessGranted(context)) throw NotAuthenticatedYetException()

        val sharedPreferences = getEncryptedSharedPreferences(context)
        val refreshToken = sharedPreferences.getString(REFRESH_TOKEN_KEY, "").orEmpty()

        if (refreshToken.isEmpty() || refreshToken.isBlank()) throw InvalidTokenException()
        return refreshToken
    }

    /**
     * Retrieves Authorization code from the token storage.
     *
     * @throws NotAuthenticatedYetException if user has not been authenticated before
     * @throws InvalidTokenException if Authorization code is empty, blank or corrupted
     * @return Authorization code
     */
    fun getAuthorizationCode(context: Context): String {
        if (!isAccessGranted(context)) throw NotAuthenticatedYetException()

        val sharedPreferences = getEncryptedSharedPreferences(context)
        val authCode = sharedPreferences.getString(AUTHORIZATION_CODE_KEY, "").orEmpty()

        if (authCode.isEmpty() || authCode.isBlank()) throw InvalidTokenException()
        return authCode
    }

    fun saveAuthorizationCode(context: Context, authCode: String) {
        val sharedPreferences = getEncryptedSharedPreferences(context)
        sharedPreferences
            .edit()
            .putString(AUTHORIZATION_CODE_KEY, authCode)
            .apply()
    }

    /**
     * Retrieves User ID token from the token storage.
     *
     * @throws NotAuthenticatedYetException if User has not been authenticated before
     * @throws InvalidTokenException if User ID is empty, blank or corrupted
     * @return User ID
     */
    fun getUserID(context: Context): String {
        if (!isAccessGranted(context)) throw NotAuthenticatedYetException()
        val sharedPreferences = getEncryptedSharedPreferences(context)
        val userID = sharedPreferences.getString(USER_ID_KEY, "").orEmpty()

        if (userID.isEmpty() || userID.isBlank()) throw InvalidTokenException()
        return userID
    }

    class NotAuthenticatedYetException: Exception()
    class InvalidTokenException: Exception()
    class TokenNeedsRefreshException: Exception()
    class CannotRefreshTokenException: Exception()

    companion object {
        private const val ENCODING = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        private const val MASTER_KEY_ALIAS = "masterKeyAlias"
        private const val CODE_CHALLENGE_ALGORITHM = "SHA-256"
        private const val CODE_ENCRYPTED_STORE = "pkceAuthCodes"
        private const val CODE_VERIFIER_KEY = "codeVerifier"
        private const val ACCESS_TOKEN_KEY = "accessToken"
        private const val REFRESH_TOKEN_KEY = "refreshToken"
        private const val AUTHORIZATION_CODE_KEY = "authCode"
        private const val USER_ID_KEY = "userID"
        private const val MUST_REFRESH_TIMESTAMP_KEY = "mustRefreshTimestamp"
    }
}