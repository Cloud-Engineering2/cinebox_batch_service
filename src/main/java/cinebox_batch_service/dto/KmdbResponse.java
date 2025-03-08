package cinebox_batch_service.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record KmdbResponse(
    @JsonProperty("Data") List<Data> data
) {
    public static record Data(
        @JsonProperty("Result") List<Result> result
    ) {}

    public static record Result(
        @JsonProperty("plots") Plots plots,
        String runtime,
        String rating,
        String posters,
        String kmdbUrl,
        @JsonProperty("actors") Actors actors
    ) {}

    public static record Plots(
        @JsonProperty("plot") List<Plot> plot
    ) {}

    public static record Plot(
        String plotLang,
        String plotText
    ) {}

    public static record Actors(
        @JsonProperty("actor") List<Actor> actor
    ) {}

    public static record Actor(
        String actorNm
    ) {}
}
