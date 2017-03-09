package de.bieniekconsulting.logstore.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LogstoreRepository extends JpaRepository<LogstoreRecord, Long> {

}
