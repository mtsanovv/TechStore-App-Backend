package com.mtsan.techstore.repositories;

import com.mtsan.techstore.Rank;
import com.mtsan.techstore.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("FROM User u WHERE u.rank = ?1")
	List<User> getUsersByRank(Rank rank);

	@Query("SELECT COUNT(u) FROM User u WHERE u.username = ?1")
	Long getUsersByUsername(String username);

	@Query("FROM User u WHERE u.username = ?1")
	List<User> getUserByUsername(String username);
}
