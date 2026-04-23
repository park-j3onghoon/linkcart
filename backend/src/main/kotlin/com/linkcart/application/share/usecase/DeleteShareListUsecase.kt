package com.linkcart.application.share.usecase

import com.linkcart.domain.port.ShareListRepository
import org.springframework.stereotype.Service

@Service
class DeleteShareListUsecase(
    private val shareListRepository: ShareListRepository,
) {

    fun execute(ownerId: Long, token: String) {
        val shareList = shareListRepository.findByToken(token)
            ?: throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        // 타인 소유 시 존재 정보 노출 방지로 동일 예외 반환
        if (shareList.ownerId != ownerId) {
            throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        }
        shareListRepository.deleteById(requireNotNull(shareList.id))
    }
}
