package com.mtsan.techstore.controllers;

import com.mtsan.techstore.Rank;
import com.mtsan.techstore.entities.User;
import com.mtsan.techstore.exceptions.TechstoreDataException;
import com.mtsan.techstore.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
	@Autowired
	private UserRepository userRepository;

	//fetching data about the currently authenticated user
	@RequestMapping(value = "/user", method = RequestMethod.GET)
	public ResponseEntity user(Authentication authentication) throws TechstoreDataException {
		User user = userRepository.getUserByUsername(authentication.getName()).get(0);
		user.setPassword(null);
		return ResponseEntity.status(HttpStatus.OK).body(user);
	}
}