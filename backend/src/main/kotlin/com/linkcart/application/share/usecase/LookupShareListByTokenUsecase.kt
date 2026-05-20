package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Service
import java.time.Clock

/** 만료된 토큰도 미존재와 동일하게 NotFound로 응답해 존재 정보 누설을 막는다 (AIP-131). */
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
