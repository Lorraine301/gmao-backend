package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findBySpecialityAndAvailabilityStatus(String speciality, String availabilityStatus);
    List<User> findByRole_NameAndAvailabilityStatus(String roleName, String availabilityStatus);
    
}