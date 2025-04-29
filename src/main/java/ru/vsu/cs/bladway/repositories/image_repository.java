package ru.vsu.cs.bladway.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.bladway.models.image;

@Repository
public interface image_repository extends JpaRepository<image, Long> {

}
