package org.cookieandkakao.babting.domain.member.service;

import org.cookieandkakao.babting.common.properties.KakaoProviderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class KakaoAuthClientTest {

    @Mock
    private KakaoProviderProperties providerProperties;

    private RestClient.Builder restClientBuilder = RestClient.builder();

    private MockRestServiceServer mockRestServiceServer;

    private KakaoAuthClient kakaoAuthClient;

    @BeforeEach
    void setup() {
        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        kakaoAuthClient = new KakaoAuthClient(providerProperties, restClientBuilder.build());
    }


}