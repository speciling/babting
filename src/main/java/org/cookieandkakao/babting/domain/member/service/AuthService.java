package org.cookieandkakao.babting.domain.member.service;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import org.cookieandkakao.babting.common.properties.KakaoClientProperties;
import org.cookieandkakao.babting.common.properties.KakaoProviderProperties;
import org.cookieandkakao.babting.domain.member.dto.KakaoMemberInfoGetResponse;
import org.cookieandkakao.babting.domain.member.dto.KakaoTokenGetResponse;
import org.cookieandkakao.babting.domain.member.dto.TokenIssueResponse;
import org.cookieandkakao.babting.domain.member.entity.KakaoToken;
import org.cookieandkakao.babting.domain.member.entity.Member;
import org.cookieandkakao.babting.domain.member.repository.MemberRepository;
import org.cookieandkakao.babting.domain.member.util.AuthorizationUriBuilder;
import org.cookieandkakao.babting.domain.member.util.JwtUtil;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {

    private final KakaoClientProperties kakaoClientProperties;
    private final KakaoProviderProperties kakaoProviderProperties;
    private final RestClient restClient;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    public AuthService(KakaoClientProperties kakaoClientProperties,
        KakaoProviderProperties kakaoProviderProperties, RestClient restClient,
        MemberRepository memberRepository, JwtUtil jwtUtil) {
        this.kakaoClientProperties = kakaoClientProperties;
        this.kakaoProviderProperties = kakaoProviderProperties;
        this.restClient = restClient;
        this.memberRepository = memberRepository;
        this.jwtUtil = jwtUtil;
    }

    public String getAuthUrl() {
        return new AuthorizationUriBuilder()
            .clientProperties(kakaoClientProperties)
            .providerProperties(kakaoProviderProperties)
            .build();
    }

    public KakaoTokenGetResponse requestKakaoToken(String authorizeCode) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", authorizeCode);
        body.add("redirect_uri", kakaoClientProperties.redirectUri());
        body.add("client_id", kakaoClientProperties.clientId());
        body.add("client_secret", kakaoClientProperties.clientSecret());

        return callTokenApi(body);
    }

    public KakaoTokenGetResponse refreshKakaoToken(String refreshToekn) {
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", kakaoClientProperties.clientId());
        body.add("refresh_token", refreshToekn);

        return callTokenApi(body);
    }

    public KakaoMemberInfoGetResponse requestKakaoMemberInfo(
        KakaoTokenGetResponse kakaoToken) {

        String userInfoUri = kakaoProviderProperties.userInfoUri();

        ResponseEntity<KakaoMemberInfoGetResponse> entity = restClient.get()
            .uri(userInfoUri)
            .header("Authorization", "Bearer " + kakaoToken.accessToken())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new IllegalArgumentException("카카오 사용자 정보 조회 실패");
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new RuntimeException("카카오 사용자 정보 서버 에러");
            })
            .toEntity(KakaoMemberInfoGetResponse.class);

        return entity.getBody();
    }

    public void unlinkMember(String accessToken) {

        String unlinkUri = kakaoProviderProperties.unlinkUri();

        restClient.get()
            .uri(unlinkUri)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new IllegalArgumentException("카카오 연결 끊기 실패");
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new RuntimeException("카카오 인증 서버 에러");
            });
    }

    @Transactional
    public Long saveMemberInfoAndKakaoToken(
        KakaoMemberInfoGetResponse kakaoMemberInfoGetResponse,
        KakaoTokenGetResponse kakaoTokenGetResponse) {

        Long kakaoMemberId = kakaoMemberInfoGetResponse.id();

        Member member = memberRepository.findByKakaoMemberId(kakaoMemberId)
            .orElse(new Member(kakaoMemberId));
        member.updateProfile(kakaoMemberInfoGetResponse.properties());

        member = memberRepository.save(member);

        KakaoToken kakaoToken = kakaoTokenGetResponse.toEntity();

        member.updateKakaoToken(kakaoToken);

        return member.getMemberId();
    }

    public TokenIssueResponse issueToken(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(IllegalArgumentException::new);

        return jwtUtil.issueToken(member.getMemberId());
    }

    private KakaoTokenGetResponse callTokenApi(MultiValueMap<String, String> body) {

        String tokenUri = kakaoProviderProperties.tokenUri();

        return restClient.post()
            .uri(tokenUri)
            .contentType(APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new IllegalArgumentException("카카오 토큰 발급 실패");
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new RuntimeException("카카오 인증 서버 에러");
            })
            .toEntity(KakaoTokenGetResponse.class)
            .getBody();
    }
}
