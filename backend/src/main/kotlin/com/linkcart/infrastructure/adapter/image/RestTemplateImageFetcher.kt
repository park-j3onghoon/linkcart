package com.linkcart.infrastructure.adapter.image

import com.linkcart.application.image.port.ImageFetcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class RestTemplateImageFetcher(
    @Qualifier("imageProxyRestTemplate")
    private val restTemplate: RestTemplate,
) : ImageFetcher {

    override fun fetch(url: String): ImageFetcher.Fetched {
        val upstream = try {
            restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, ByteArray::class.java)
        } catch (e: RestClientException) {
            throw ImageFetcher.FetchFailed("이미지 fetch 실패: $url", e)
        }
        return ImageFetcher.Fetched(
            bytes = upstream.body ?: ByteArray(0),
            contentType = upstream.headers.contentType,
        )
    }
}
