package com.mtsan.techstore.controllers;

import com.mtsan.techstore.Rank;
import com.mtsan.techstore.entities.User;
import com.mtsan.techstore.exceptions.TechstoreDataException;
import com.mtsan.techstore.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
public class MerchantsController {

	@Autowired
	private UserRepository userRepository;

	PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	//fetching a list of all merchants
	@RequestMapping(value = "/merchants", method = RequestMethod.GET)
	public ResponseEntity merchants() throws TechstoreDataException {
		List<User> merchants = userRepository.getUsersByRank(Rank.Merchant);
		if (merchants.size() > 0) {
			for (User merchant : merchants) {
				merchant.setPassword(null);
			}

			return ResponseEntity.status(HttpStatus.OK).body(merchants);
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No merchants found");
		}
	}

	//adding a merchant
	@RequestMapping(value = "/merchants", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	public ResponseEntity addMerchant(@RequestBody User postedMerchant) throws TechstoreDataException {
		postedMerchant.setPassword(passwordEncoder.encode(postedMerchant.getPassword()));
		postedMerchant.setRank(Rank.Merchant);
		if (userRepository.getUsersByUsername(postedMerchant.getUsername()) == 0) {
			User savedMerchant = userRepository.save(postedMerchant);
			savedMerchant.setPassword(null);
			return ResponseEntity.status(HttpStatus.CREATED).body(savedMerchant);
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "This username is already in use, please use another one");
		}
	}

	//fetching a merchant
	@RequestMapping(value = "/merchants/{merchantId}", method = RequestMethod.GET)
	public ResponseEntity getMerchant(@PathVariable Long merchantId) throws TechstoreDataException {
		if (userRepository.getUsersByRank(Rank.Merchant).size() > 0) {
			boolean isIdReal = userRepository.existsById(merchantId);
			if (isIdReal) {
				User merchant = userRepository.findById(merchantId).get();
				merchant.setPassword(null);
				return ResponseEntity.status(HttpStatus.OK).body(merchant);
			} else {
				throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Merchant not found");
			}
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No merchants found");
		}
	}

	//editing a merchant
	@RequestMapping(value = "/merchants/{merchantId}", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
	public ResponseEntity editMerchant(@RequestBody User newMerchant, @PathVariable Long merchantId) throws TechstoreDataException {
		List<User> merchants = userRepository.getUsersByRank(Rank.Merchant);
		if (merchants.size() > 0) {
			boolean isIdReal = userRepository.existsById(merchantId);
			if (isIdReal) {
				newMerchant.setId(merchantId);
				newMerchant.setRank(Rank.Merchant);
				User oldMerchant = userRepository.findById(merchantId).get();
				if(newMerchant.getPassword() == null || newMerchant.getPassword().isEmpty())
				{
					newMerchant.setPassword(oldMerchant.getPassword());
				}
				else
				{
					newMerchant.setPassword(passwordEncoder.encode(newMerchant.getPassword()));
				}

				Long usersWhoHaveThisUsername = userRepository.getUsersByUsername(newMerchant.getUsername());
				if ((!oldMerchant.getUsername().equals(newMerchant.getUsername()) && usersWhoHaveThisUsername == 0) || (oldMerchant.getUsername().equals(newMerchant.getUsername()) && usersWhoHaveThisUsername == 1)) {
					User savedMerchant = userRepository.save(newMerchant);
					savedMerchant.setPassword(null);
					return ResponseEntity.status(HttpStatus.OK).body(savedMerchant);
				} else {
					throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "This username is already in use, please use another one");
				}
			} else {
				throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Merchant not found");
			}
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No merchants found");
		}
	}

	//deleting a merchant
	@RequestMapping(value = "/merchants/{merchantId}", method = RequestMethod.DELETE)
	public ResponseEntity deleteMerchant(@PathVariable Long merchantId) throws TechstoreDataException {
		if (userRepository.count() > 0) {
			boolean isIdReal = userRepository.existsById(merchantId);
			if (isIdReal) {
				if(userRepository.findById(merchantId).get().getRank() == Rank.Merchant) {
					userRepository.deleteById(merchantId);
					return ResponseEntity.status(HttpStatus.OK).build();
				} else {
					throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Invalid merchant ID");
				}
			} else {
				throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Merchant not found");
			}
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No merchants found");
		}
	}
}
