package cat.urv.cloudlab.soundless.util.datastate

import cat.urv.cloudlab.soundless.util.auth.RefreshedTokens
import cat.urv.cloudlab.soundless.util.auth.Tokens

sealed class TokenStatusDataState {
    object RequestingTokens: TokenStatusDataState()
    data class TokensObtained(val tokens: Tokens): TokenStatusDataState()
    data class RefreshedTokensObtained(val tokens: RefreshedTokens): TokenStatusDataState()
    object AccessRenewed: TokenStatusDataState()
    data class AccessNotRenewed(val exception: Exception): TokenStatusDataState()
    data class TokensNotObtained(val exception: Exception): TokenStatusDataState()
}
