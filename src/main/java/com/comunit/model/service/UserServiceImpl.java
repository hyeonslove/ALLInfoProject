package com.comunit.model.service;

import com.comunit.exception.BaseException;
import com.comunit.exception.ErrorMessage;
import com.comunit.model.domain.user.MypageDTO;
import com.comunit.model.domain.user.UserDTO;
import com.comunit.model.domain.param.LoginDTO;
import com.comunit.model.mapper.UserMapper;
import com.comunit.util.EmailHandler;
import com.comunit.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final EmailHandler emailHandler;
    private final UserMapper userMapper;

    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public UserDTO signup(UserDTO userDTO) throws Exception {
        if (userMapper.findUserById(userDTO.getId()).isPresent()) {
            // 이미 존재하는 아이디
            throw new BaseException(ErrorMessage.EXIST_ID);
        }

        if (userMapper.findUserByNickname(userDTO.getNickname()).isPresent()) {
            throw new BaseException(ErrorMessage.EXIST_NICKNAME);
        }

        if (userMapper.findUserByEmail(userDTO.getEmail()).isPresent()) {
            // 이미 존재하는 이메일
            throw new BaseException(ErrorMessage.EXIST_EMAIL);
        }

        userDTO.setPw(passwordEncoder.encode(userDTO.getPassword()));
        userMapper.signup(userDTO);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String salt = userDTO.getUid().toString() + calendar.getTime();

        salt = (BCrypt.hashpw(salt, BCrypt.gensalt()));
        userMapper.setSalt(userDTO.getUid(), salt);

        return userMapper.findUserById(userDTO.getUsername()).get();
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDto) throws Exception {
        UserDTO userDto = userMapper.findUserById(loginDto.getId())
                .orElseThrow(() -> new BaseException(ErrorMessage.NOT_EXIST_ID));

        if (userDto.getLevel() == 0) {
            throw new BaseException(ErrorMessage.SIGNUP_LISTEN);
        }
        if (!passwordEncoder.matches(loginDto.getPw(), userDto.getPassword())) {
            throw new BaseException(ErrorMessage.NOT_PASSWORD);
        }
        String accessToken = jwtTokenProvider.createToken(userDto.getUid(), Collections.singletonList(userDto.getRole()));
        String refreshToken = jwtTokenProvider.createRefresh(userDto.getUid(), Collections.singletonList(userDto.getRole()));
        userDto.setRefresh_token(refreshToken);
        userMapper.setRefreshToken(userDto);
        return new HashMap<String, Object>() {{
            put("name", userDto.getName());
            put("access-token", accessToken);
            put("refresh-token", refreshToken);
            put("uid", userDto.getUid());
        }};
    }

    @Override
    public String refreshToken(Long uid, String token) throws Exception {
        Optional<UserDTO> object = userMapper.findUserByUid(uid);
        if (object.isPresent()) {
            UserDTO userDTO = object.get();
            if (token.equals(userDTO.getRefresh_token())) {
                if (jwtTokenProvider.validateToken(token))
                    return jwtTokenProvider.createToken(userDTO.getUid(), Collections.singletonList(userDTO.getRole()));
                else
                    throw new BaseException(ErrorMessage.ACCESS_TOKEN_EXPIRE);
            } else {
                throw new BaseException(ErrorMessage.REFRESH_TOKEN_NOT_MATCH);
            }
        } else {
            throw new BaseException(ErrorMessage.NOT_USER_INFO);
        }
    }

    @Override
    public void sendSignupEmail(UserDTO user) throws Exception {
        user = userMapper.findUserById(user.getId()).get();
        if (user.getLevel() != 0)
            throw new BaseException(ErrorMessage.EXIST_CHECK_MAIL);
        String token = jwtTokenProvider.create(user.getUid(), Collections.singletonList(user.getRole()), 1000 * 60 * 30);
        emailHandler.sendMail(user.getEmail(), "Comunit 이메일 인증입니다.", "<h1>Comunit 이메일 인증 회원가입이예요</h1><a href='http://183.97.128.216/check?token=" + token + "'>여기를 눌러 인증해주세요.</a>", true);
    }

    @Override
    public void resendCheckMail(LoginDTO loginDTO) throws Exception {
        UserDTO user = userMapper.findUserById(loginDTO.getId())
                .orElseThrow(() -> new BaseException(ErrorMessage.NOT_EXIST_ID));

        if (passwordEncoder.matches(loginDTO.getPw(), user.getPassword())) {
            sendSignupEmail(user);
        } else {
            throw new BaseException(ErrorMessage.NOT_PASSWORD);
        }
    }

    @Override
    public void checkEmail(String token) throws Exception {
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long uid = Long.parseLong(jwtTokenProvider.getUserId(token));
            UserDTO user = userMapper.findUserByUid(uid).get();
            if (user.getLevel() != 0)
                throw new BaseException(ErrorMessage.EXIST_CHECK_MAIL);
            userMapper.checkEmail(uid);
        } else {
            throw new BaseException(ErrorMessage.ACCESS_TOKEN_INVALID);
        }
    }

    @Override
    public void findMyPW(UserDTO user) throws Exception {
        UserDTO suser = userMapper.findUserByEmail(user.getEmail()).get();

        if (suser != null) {
            if (suser.getId().equals(user.getId()) && suser.getName().equals(user.getName())) {
                Random rnd = new Random();
                StringBuilder temp_pw = new StringBuilder();
                for (int i = 0; i < 20; i++) {
                    if (rnd.nextBoolean()) {
                        temp_pw.append((char) ((int) (rnd.nextInt(26)) + 97));
                    } else {
                        temp_pw.append((rnd.nextInt(10)));

                    }
                }
                String epw = passwordEncoder.encode(temp_pw);
                userMapper.setPassword(user.getEmail(), epw);

                emailHandler.sendMail(user.getEmail(), "임시 비밀번호입니다.", "임시 비밀번호는 " + temp_pw + " 입니다.", false);
            } else {
                throw new BaseException(ErrorMessage.NOT_USER_INFO_MATCH);
            }
        } else {
            throw new BaseException(ErrorMessage.NOT_USER_INFO);
        }
    }

    @Override
    public UserDTO getMypage(Long user_Uid) {
        return userMapper.getMypage(user_Uid);
    }

    @Override
    @Transactional
    public Boolean setMypage(MypageDTO user, UserDTO auth) {
        if (!passwordEncoder.matches(user.getPw(), auth.getPassword())) {
            throw new BaseException(ErrorMessage.NOT_PASSWORD);
        }
        user.setUid(auth.getUid());
        user.setNpw(passwordEncoder.encode(user.getNpw()));

        userMapper.setMypage(user);
        return true;
    }
}
