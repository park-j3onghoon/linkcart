package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ShareList
import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class GetShareListByIdUsecase(
    private val shareListRepository: ShareListRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun execute(id: Long): ShareList {
        val shareList = shareListRepository.findById(id)
            ?: throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        if (shareList.isExpired(clock.instant())) {
            throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        }
        return shareList
    }
}
