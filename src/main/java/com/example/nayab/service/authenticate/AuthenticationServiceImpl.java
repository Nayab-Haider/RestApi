package com.example.nayab.service.authenticate;

import com.example.nayab.configuration.RefreshToken;
import com.example.nayab.util.CustomGenerator;
import com.example.nayab.util.authenticate.ResetPassword;
import com.example.nayab.util.mail.MailModel;
import com.example.nayab.util.mail.MyMailSender;
import com.example.nayab.configuration.JwtTokenProvider;
import com.example.nayab.domain.user.User;
import com.example.nayab.exception.authentication.AuthenticationFailed;
import com.example.nayab.repository.user.UserRepository;
import com.example.nayab.util.response.ResponseDomain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService{


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MyMailSender myMailSender;

    @Autowired
    private CustomGenerator customGenerator;

    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    private static final Logger logger= LogManager.getLogger(AuthenticationService.class);

    public ResponseEntity<?> signin(String username, String password) {
        logger.info("Entering into AuthenticationService inside method signin");
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        logger.info("Returning from AuthenticationService inside method signin");
        return new ResponseEntity<>(new ResponseDomain(jwtTokenProvider.createToken(username, userRepository.findByUsername(username).getRoles()),true), HttpStatus.OK);
    }

    public  ResponseEntity<?> signup(User user) {
        logger.info("Entering into AuthenticationService inside method signup");
        if (!userRepository.existsByUsername(user.getUsername())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            logger.info("Returning from AuthenticationService inside method signup");

            return new ResponseEntity<>( new ResponseDomain("Signup successfull",true),HttpStatus.OK);
        } else {
            logger.error("Returning from AuthenticationService inside method signup");
            return new ResponseEntity<>(new ResponseDomain("User Already Exist",false),HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> delete(String username) {

        logger.info("Entering into AuthenticationService inside method delete");
        int result = userRepository.deleteByUsername(username);
        if(result>0){
            logger.info("Returning from AuthenticationService inside method delete");
            return new ResponseEntity<>(new ResponseDomain("Successfully deleted user",true),HttpStatus.OK);
        }
        else
            throw new AuthenticationFailed("User does not exist");
    }


    public ResponseEntity<?> whoami(HttpServletRequest req) {
        logger.info("Entering into AuthenticationService inside method whoami");
        User currentUser;
        try {
            currentUser  = userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken
                    (req)));
        }catch (Exception e){
            logger.error("Returning from AuthenticationService inside method whoami");
            return new ResponseEntity<>("Invalid Token",HttpStatus.BAD_REQUEST);
        }
        logger.info("Returning from AuthenticationService inside method whoami");
        return new ResponseEntity<>(currentUser,HttpStatus.OK);

    }

    @Override
    public ResponseEntity<?> forgotPassword(HttpServletRequest request, String userName) {
        logger.info("Entering into AuthenticationService inside method forgotPassword");
        User currentUser=null;
        try {
            currentUser  = userRepository.findByUsername(userName);

            String appUrl = request.getScheme() + "://" + request.getServerName();
            currentUser.setResetToken(UUID.randomUUID().toString());
            myMailSender.sendMail(new MailModel(currentUser.getEmail(),"support.proh2r@niletechnologies.com","Password Reset Request","To reset your password, click the link below:\n" + appUrl
                    + "/reset?token=" + currentUser.getResetToken()));
            userRepository.save(currentUser);
            return new ResponseEntity<>(currentUser,HttpStatus.OK);

        }catch (Exception e){

        }
        logger.info("Returning from AuthenticationService inside method forgotPassword");
        return new ResponseEntity<>(currentUser,HttpStatus.OK);

    }

    @Override
    public ResponseEntity<?> resetPassword(ResetPassword resetPassword) {
        logger.info("Entering into AuthenticationService inside method resetPassword");
        User currentUser=null;
        currentUser=userRepository.findUserByResetToken(resetPassword.getToken());
            if (currentUser!=null){
                currentUser.setPassword(passwordEncoder.encode(resetPassword.getPassword()));
                currentUser.setResetToken(null);
                userRepository.save(currentUser);
                return new ResponseEntity<>("Password Reset successfully",HttpStatus.OK);

            }
        logger.info("Returning from AuthenticationService inside method forgotPassword");

        // This should always be non-null but we check just in case


        return new ResponseEntity<>("Failed to Reset Password",HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> refreshToken(HttpServletRequest req) {

        User currentUser;
        try {
            currentUser  = userRepository.findByUsername(jwtTokenProvider.getUsername(jwtTokenProvider.resolveToken
                    (req)));
        }catch (Exception e){
            logger.error("Returning from AuthenticationService inside method refreshToken");
            return new ResponseEntity<>("Invalid Token",HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ResponseDomain(jwtTokenProvider.createToken(currentUser.getUsername(), userRepository.findByUsername(currentUser.getUsername()).getRoles()),true), HttpStatus.OK);
    }


}
