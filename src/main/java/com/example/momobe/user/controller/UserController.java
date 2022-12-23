package com.example.momobe.user.controller;

import com.example.momobe.common.exception.enums.ErrorCode;
import com.example.momobe.common.resolver.Token;
import com.example.momobe.common.resolver.UserInfo;
import com.example.momobe.user.application.UserCommonService;
import com.example.momobe.user.domain.User;
import com.example.momobe.user.domain.UserNotFoundException;
import com.example.momobe.user.domain.UserRepository;
import com.example.momobe.user.dto.JwtTokenDto;
import com.example.momobe.user.dto.UserDto;
import com.example.momobe.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class UserController {
    private final UserCommonService userCommonService;
    private final UserRepository userRepository;
    private final UserMapper mapper;

    @DeleteMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public boolean withdrawal(@RequestHeader @Token UserInfo request){
        userCommonService.withdrawalUser(request.getEmail());
        return true;
    }
    @GetMapping("/profile")
    @ResponseStatus(HttpStatus.OK)
    public UserDto getUser(@RequestHeader @Token UserInfo request){
        User findUser = userRepository.findUserByEmail(request.getEmail()).orElseThrow(() -> new UserNotFoundException(ErrorCode.DATA_NOT_FOUND));
        return mapper.userDtoOfUser(findUser);

    }

}
