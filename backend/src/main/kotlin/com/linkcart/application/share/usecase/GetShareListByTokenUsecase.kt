package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Service
import java.time.Clock

class ShareListNotFoundException(message: String) : RuntimeException(message)

@Service
class GetShareListByTokenUsecase(
    private val shareListRepository: ShareListRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun execute(token: String): ShareList {
        val shareList = shareListRepository.findByToken(token)
            ?: throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        // 만료 시에도 존재 정보 노출 방지를 위해 동일 예외로 응답한다
        if (shareList.isExpired(clock.instant())) {
            throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        }
        return shareList
    }
}
