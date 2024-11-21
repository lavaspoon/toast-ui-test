package lava.toastuitest.repository;

import lava.toastuitest.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}