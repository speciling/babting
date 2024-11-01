package org.cookieandkakao.babting.domain.member.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.cookieandkakao.babting.common.apiresponse.ApiResponseBody;
import org.cookieandkakao.babting.common.apiresponse.ApiResponseGenerator;
import org.cookieandkakao.babting.domain.member.dto.KakaoMemberInfoGetResponse;
import org.cookieandkakao.babting.domain.member.dto.KakaoTokenGetResponse;
import org.cookieandkakao.babting.domain.member.dto.TokenIssueResponse;
import org.cookieandkakao.babting.domain.member.service.AuthService;
import org.cookieandkakao.babting.domain.member.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/auth")
public class AuthController {

    private static final int REFRESH_TOKEN_EXPIRES_IN = 1000 * 60 * 60 * 24 * 14;  // 2주

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:" + authService.getAuthUrl();
    }

    @GetMapping("/login/code/kakao")
    public String issueToken(
        @RequestParam(name = "code") String authorizeCode, HttpServletResponse response) {

        KakaoTokenGetResponse kakaoTokenDto;
        KakaoMemberInfoGetResponse kakaoMemberInfoDto;

        try {
            kakaoTokenDto = authService.requestKakaoToken(authorizeCode);
            kakaoMemberInfoDto = authService.requestKakaoMemberInfo(kakaoTokenDto);
        } catch (Exception e) {
            return "redirect:/login/fail";  // 프론트 페이지 구현 후 수정 예정
        }

        Long memberId = authService.saveMemberInfoAndKakaoToken(kakaoMemberInfoDto, kakaoTokenDto);
        TokenIssueResponse tokenDto = authService.issueToken(memberId);

        response.addCookie(createRefreshTokenCookie(tokenDto));

        return "redirect:/login/success";  // 프론트 페이지 구현 후 수정 예정
    }

    @GetMapping("access-token")
    @ResponseBody
    public ResponseEntity<ApiResponseBody.SuccessBody<Void>> reissueToken(
        @CookieValue(required = false) String refreshToken, HttpServletResponse response) {

        Long memberId = Long.parseLong(jwtUtil.parseClaims(refreshToken).getSubject());
        TokenIssueResponse tokenDto = authService.issueToken(memberId);

        response.setHeader("Authorization", tokenDto.accessToken());

        return ApiResponseGenerator.success(HttpStatus.OK, "토큰 재발급 성공");
    }

    private static Cookie createRefreshTokenCookie(TokenIssueResponse tokenDto) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenDto.refreshToken());
        refreshTokenCookie.setPath("/api/auth/access-token");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_EXPIRES_IN);
        return refreshTokenCookie;
    }
}
