package cinebox_batch_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cinebox_batch_service.entity.Movie;
import cinebox_batch_service.enums.MovieStatus;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    boolean existsByTitleAndReleaseDate(String title, LocalDate releaseDate);

    List<Movie> findByTitleContaining(String searchText);

    List<Movie> findByStatusAndReleaseDateBefore(MovieStatus status, LocalDate releaseDate);

    boolean existsByTitleAndReleaseDateAndPosterImageUrlIsNotNull(String title, LocalDate releaseDate);
}
