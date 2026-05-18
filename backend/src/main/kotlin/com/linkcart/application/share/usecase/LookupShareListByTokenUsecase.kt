package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * AIP-136 custom method: 토큰은 secret이라 path/query에 두지 않고 request body로 받는다.
 * 만료된 토큰은 미존재와 같은 NotFound로 응답한다 (존재 정보 노출 방지).
 */
@Service
class LookupShareListByTokenUsecase(
    private val shareListRepository: ShareListRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun execute(token: String): ShareList {
        val shareList = shareListRepository.findByToken(token)
            ?: throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        if (shareList.isExpired(clock.instant())) {
            throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        }
        return shareList
    }
}
