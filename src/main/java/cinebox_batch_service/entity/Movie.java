package cinebox_batch_service.entity;

import java.time.LocalDate;

import cinebox_batch_service.enums.MovieStatus;
import cinebox_batch_service.enums.RatingGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movie")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Movie extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "movie_id")
	private Long movieId;

	@Column(nullable = false)
	private String title;

	// 줄거리 (description)
	@Lob
	private String plot;

	private String director;
	private String actor;
	private String genre;

	@Column(name = "poster_image_url", length = 500)
	private String posterImageUrl;

	@Column(name = "release_date")
	private LocalDate releaseDate;

	@Column(name = "run_time")
	private Integer runTime;

	@Column(name = "rating_grade")
	@Enumerated(EnumType.STRING)
	private RatingGrade ratingGrade;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private MovieStatus status;
	
	@Column(name = "like_count", nullable = false)
	private Integer likeCount = 0;
	
	public void updateMovieStatus(MovieStatus status) {
		this.status = status;
	}
}
