package com.linkcart.application.share.usecase

import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Service

@Service
class DeleteShareListUsecase(
    private val shareListRepository: ShareListRepository,
) {

    fun execute(ownerId: Long, shareListId: Long) {
        val shareList = shareListRepository.findById(shareListId)
            ?: throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        // 타인 소유 시에도 동일 NotFound. 존재 정보 누설 방지 정책.
        if (!shareList.isOwnedBy(ownerId)) {
            throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        }
        shareListRepository.deleteById(shareListId)
    }
}
