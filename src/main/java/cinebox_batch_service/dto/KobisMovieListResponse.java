package cinebox_batch_service.dto;

import java.util.List;
import java.util.stream.Collectors;

public record KobisMovieListResponse(MovieListResult movieListResult) {
	public record MovieListResult(
		    int totCnt,
		    String source,
		    List<KobisMovieDto> movieList
	) {}
	
	public record KobisMovieDto(
			String movieCd,
			String movieNm,
			String movieNmEn,
			String prdtYear,
			String openDt,
			String typeNm,
			String prdtStatNm,
			String nationAlt,
			String genreAlt,
			String repNationNm,
			String repGenreNm,
			List<DirectorDto> directors
	) {
		public String extractDirectors() {
			if (directors == null || directors.isEmpty()) {
				return null;
			}
			
			return directors.stream()
					.map(DirectorDto::peopleNm)
					.collect(Collectors.joining(", "));
		}
	}
	
	public record DirectorDto(String peopleNm) {}
}
