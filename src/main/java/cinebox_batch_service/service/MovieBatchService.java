package cinebox_batch_service.service;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cinebox_batch_service.dto.KmdbResponse;
import cinebox_batch_service.dto.KobisMovieListResponse;
import cinebox_batch_service.dto.KobisMovieListResponse.KobisMovieDto;
import cinebox_batch_service.entity.Movie;
import cinebox_batch_service.enums.MovieStatus;
import cinebox_batch_service.enums.RatingGrade;
import cinebox_batch_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieBatchService {
    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate = createRestTemplate();

    // KMDB 응답형태가 HTML이므로 JSON, HTML 지원 RestTemplate 정의
    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }

    @Value("${kobis.api.key}")
    private String kobisApiKey;

    @Value("${kobis.api.url}")
    private String kobisApiUrl;
    
    @Value("${kmdb.api.key}")
    private String kmdbApiKey;
    
    @Value("${kmdb.api.url}")
    private String kmdbApiUrl;
    
    // KOBIS 영화 목록 개수 설정 (1 ~ 100)
    @Value("${itemPerPage:100}")
    private int itemPerPage;

    // 매일 0시 30분 당일에 개봉하는 영화 개봉 상태 변경 (UPCOMING -> SHOWING) 
    @Scheduled(cron = "0 30 0 * * ?")
    public void updateMoviesToShowing() {
        LocalDate today = LocalDate.now();
        log.info("Starting updateMoviesToShowing for date: {}", today);
        
        List<Movie> movies = movieRepository.findByStatusAndReleaseDateBefore(MovieStatus.UPCOMING, today);
        
        if (movies != null && !movies.isEmpty()) {
            movies.forEach(movie -> movie.updateMovieStatus(MovieStatus.SHOWING));
            movieRepository.saveAll(movies);
            log.info("Updated {} movies to SHOWING", movies.size());
        } else {
            log.info("No movies found to update for date: {}", today);
        }
    }
    
    // 매일 자정에 영화 목록 업데이트
    @Scheduled(cron = "0 0 0 * * ?")
    public void fetchAndSaveMovies() {
        log.info("Starting movie batch job...");
        
        int openStartDt = LocalDate.now().getYear();
        List<KobisMovieDto> movieDtoList = fetchKobisMovieList(openStartDt);
        if (movieDtoList == null || movieDtoList.isEmpty()) {
            log.info("No movie data found from KOBIS API");
            return;
        }
        
        List<Movie> movies = movieDtoList.stream()
                .map(this::convertToMovie)
                .filter(movie -> movie != null)
                .collect(Collectors.toList());
        
        movieRepository.saveAll(movies);
        log.info("Movie batch job completed. Saved {} movies.", movies.size());
    }
    
    // 매년 12월 15일 01시에 다음 연도 영화 목록 조회 (fetchAndSaveMovies 와의 충돌을 피하기 위함)
    @Scheduled(cron = "0 0 1 15 12 ?")
    public void fetchAndSaveMoviesNextYear() {
        log.info("Starting movie batch job...");
        
        int openStartDt = LocalDate.now().getYear() + 1;
        List<KobisMovieDto> movieDtoList = fetchKobisMovieList(openStartDt);
        if (movieDtoList == null || movieDtoList.isEmpty()) {
            log.info("No movie data found from KOBIS API");
            return;
        }
        
        List<Movie> movies = movieDtoList.stream()
                .map(this::convertToMovie)
                .filter(movie -> movie != null)
                .collect(Collectors.toList());
        
        movieRepository.saveAll(movies);
        log.info("Movie batch job completed. Saved {} movies.", movies.size());
    }
    
    // KOBIS 영화목록 조회
    private List<KobisMovieDto> fetchKobisMovieList(int openStartDt) {
        String url = String.format("%s?key=%s&openStartDt=%d&itemPerPage=%d",
                kobisApiUrl, kobisApiKey, openStartDt, itemPerPage);
        KobisMovieListResponse response = restTemplate.getForObject(URI.create(url), KobisMovieListResponse.class);
        
        if (response == null || response.movieListResult() == null) {
            log.warn("No data returned from KOBIS API");
            return null;
        }
        
        return response.movieListResult().movieList();
    }
    
    // KMDB 영화 상세 조회
    private KmdbResponse fetchKmdbResponse(String title, String releaseDts, String releaseDte) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
            String kmdbUrl = String.format("%s&title=%s&releaseDts=%s&releaseDte=%s&ServiceKey=%s",
                    kmdbApiUrl, encodedTitle, releaseDts, releaseDte, kmdbApiKey);
            
            return restTemplate.getForObject(URI.create(kmdbUrl), KmdbResponse.class);
        } catch (UnsupportedEncodingException e) {
            log.error("Failed URL encoding: {}", e.getMessage());
            return null;
        }
    }
    
    // DTO -> Entity
    private Movie convertToMovie(KobisMovieDto dto) {
        String title = dto.movieNm();
        String openDt = dto.openDt();
        LocalDate releaseDate = LocalDate.parse(openDt, DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 제목, 개봉일이 일치하고, 영화포스터가 존재하는 영화는 KMDB API 호출을 하지 않음
        boolean exists = movieRepository.existsByTitleAndReleaseDateAndPosterImageUrlIsNotNull(title, releaseDate);
        if (exists) {
            log.info("Movie '{}' with release date {} already exists in DB. Skipping KMDB API call.", title, releaseDate);
            return null;
        }
        
        // KMDB 영화 상세정보 검색(개봉일 기준)을 위한 파라미터
        // releaseDts ~ releaseDte 의 개봉일 영화 
        String releaseDts = releaseDate.minusMonths(3).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String releaseDte = openDt;
        
        KmdbResponse kmdbResponse = fetchKmdbResponse(title, releaseDts, releaseDte);
        KmdbResponse.Result kmdbResult = extractResult(kmdbResponse);
        log.info("KMDB Response: {}", kmdbResponse);
        if (kmdbResult == null) {
            log.warn("No data returned from KMDB API");
            return null;
        }
        
        String posterImageUrl = extractPosterImageUrl(kmdbResult);
        String actors = extractActorNames(kmdbResult);
        String plot = extractPlotText(kmdbResult);
        String runtimeStr = extractRuntime(kmdbResult);
        int runtime = (runtimeStr == null || runtimeStr.isEmpty()) ? 0 : Integer.parseInt(runtimeStr);
        String rating = extractRating(kmdbResult);
        
        return Movie.builder()
            .title(title)
            .plot(plot)
            .director(dto.extractDirectors())
            .actor(actors)
            .genre(dto.genreAlt())
            .posterImageUrl(posterImageUrl)
            .releaseDate(releaseDate)
            .runTime(runtime)
            .ratingGrade(RatingGrade.fromLabel(rating))
            .status(MovieStatus.UNRELEASED)
            .likeCount(0)
            .build();
    }
    
    private KmdbResponse.Result extractResult(KmdbResponse kmdbResponse) {
        if (kmdbResponse == null || kmdbResponse.data() == null || kmdbResponse.data().isEmpty()) {
            return null;
        }
        
        KmdbResponse.Data data = kmdbResponse.data().get(0);
        if (data.result() == null || data.result().isEmpty()) {
            return null;
        }
        
        return data.result().get(0);
    }
    
    // 영화 포스터 파싱
    private String extractPosterImageUrl(KmdbResponse.Result result) {
        if (result == null || result.posters() == null || result.posters().isEmpty()) return null;
        return result.posters().split("\\|")[0];
    }
    
    // 배우 목록 파싱
    private String extractActorNames(KmdbResponse.Result result) {
        if (result == null || result.actors() == null || result.actors().actor().isEmpty()) return null;
        return result.actors().actor().stream()
                .limit(6)   // 최대 6개의 데이터만 허용  
                .map(KmdbResponse.Actor::actorNm)
                .collect(Collectors.joining(", "));
    }
    
    // 영화 줄거리 파싱
    private String extractPlotText(KmdbResponse.Result result) {
        if (result == null || result.plots() == null || result.plots().plot().isEmpty()) return null;
        return result.plots().plot().stream()
                .map(KmdbResponse.Plot::plotText)
                .findFirst()
                .orElse(null);
    }
    
    // 영화 상영시간 파싱
    private String extractRuntime(KmdbResponse.Result result) {
        if (result == null || result.runtime() == null || result.runtime().isEmpty()) return null;
        return result.runtime();
    }
    
    // 영화 관람등급 파싱
    private String extractRating(KmdbResponse.Result result) {
        if (result == null || result.rating() == null || result.rating().isEmpty()) return null;
        return result.rating();
    }
}
