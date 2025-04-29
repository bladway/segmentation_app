package ru.vsu.cs.bladway.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.bladway.models.image_processed;

@Repository
public interface image_processed_repository extends JpaRepository<image_processed, Long> {

}
