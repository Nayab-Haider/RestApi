package com.example.nayab.service.authenticate;

import com.example.nayab.configuration.JwtTokenProvider;
import com.example.nayab.controller.authenticate.AuthenticationController;
import com.example.nayab.domain.user.User;
import com.example.nayab.repository.user.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    private static final Logger logger= LogManager.getLogger(AuthenticationService.class);

    public ResponseEntity<?> signin(String username, String password) {
        logger.info("Entering into AuthenticationService inside method signin");
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            logger.info("Returning from AuthenticationService inside method signin");
            return new ResponseEntity<>(jwtTokenProvider.createToken(username, userRepository.findByUsername(username).getRoles()),HttpStatus.OK);
        } catch (AuthenticationException e) {
            logger.error("Returning from AuthenticationService inside method signin");
            return new ResponseEntity<>("User does not Exist",HttpStatus.BAD_REQUEST);
        }
    }

    public  ResponseEntity<?> signup(User user) {
        logger.info("Entering into AuthenticationService inside method signup");
        if (!userRepository.existsByUsername(user.getUsername())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            logger.info("Returning from AuthenticationService inside method signup");
            return new ResponseEntity<>("Signup successfull",HttpStatus.OK);
        } else {
            logger.error("Returning from AuthenticationService inside method signup");
            return new ResponseEntity<>("User Already Exist",HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<?> delete(String username) {
        logger.info("Entering into AuthenticationService inside method delete");
       try {
           userRepository.deleteByUsername(username);
           logger.info("Returning from AuthenticationService inside method delete");
           return new ResponseEntity<>("Successfully deleted user",HttpStatus.OK);
       }
       catch (Exception e){

       }
        logger.error("Returning from AuthenticationService inside method delete");
        return new ResponseEntity<>("Failed to delete user",HttpStatus.BAD_REQUEST);
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
}
