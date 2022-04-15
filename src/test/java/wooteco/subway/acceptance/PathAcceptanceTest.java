package wooteco.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import wooteco.subway.dto.LineResponse;
import wooteco.subway.dto.PathResponse;
import wooteco.subway.dto.StationResponse;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static wooteco.subway.acceptance.LineAcceptanceTest.지하철_노선_등록되어_있음;
import static wooteco.subway.acceptance.SectionAcceptanceTest.지하철_구간_등록되어_있음;
import static wooteco.subway.acceptance.StationAcceptanceTest.지하철역_등록되어_있음;

@DisplayName("지하철 경로 조회")
public class PathAcceptanceTest extends AcceptanceTest {
    private LineResponse 신분당선;
    private LineResponse 이호선;
    private LineResponse 삼호선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 교대역;
    private StationResponse 남부터미널역;

    /**
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역  --- *3호선* ---   양재
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        강남역 = 지하철역_등록되어_있음("강남역");
        양재역 = 지하철역_등록되어_있음("양재역");
        교대역 = 지하철역_등록되어_있음("교대역");
        남부터미널역 = 지하철역_등록되어_있음("남부터미널역");

        신분당선 = 지하철_노선_등록되어_있음("신분당선", "bg-red-600", 강남역, 양재역, 10);
        이호선 = 지하철_노선_등록되어_있음("이호선", "bg-red-600", 교대역, 강남역, 10);
        삼호선 = 지하철_노선_등록되어_있음("삼호선", "bg-red-600", 교대역, 양재역, 5);

        지하철_구간_등록되어_있음(삼호선, 교대역, 남부터미널역, 3);
    }

    @DisplayName("두 역의 최단 거리 경로를 조회한다.")
    @Test
    void findPathByDistance() {
        //when
        ExtractableResponse<Response> response = 거리_경로_조회_요청(3L, 2L, null);

        //then
        적절한_경로_응답됨(response, Arrays.asList(교대역, 남부터미널역, 양재역));
        총_거리가_응답됨(response, 5);
        요금이_응답됨(response, 1250);
    }

    @DisplayName("두 역의 최단 거리 경로를 나이를 고려하여 조회한다.")
    @Test
    void findPathByDistanceWithAge() {
        //when
        ExtractableResponse<Response> response = 거리_경로_조회_요청(3L, 2L, 16);

        //then
        적절한_경로_응답됨(response, Arrays.asList(교대역, 남부터미널역, 양재역));
        총_거리가_응답됨(response, 5);
        요금이_응답됨(response, 1250);
    }

    public static ExtractableResponse<Response> 거리_경로_조회_요청(long source, long target, Integer age) {
        if (age == null) {
            return RestAssured
                    .given().log().all()
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .when().get("/paths?source={sourceId}&target={targetId}", source, target)
                    .then().log().all()
                    .extract();
        }

        return RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get("/paths?source={sourceId}&target={targetId}&age={age}", source, target, age)
                .then().log().all()
                .extract();
    }

    public static void 적절한_경로_응답됨(ExtractableResponse<Response> response, List<StationResponse> expectedPath) {
        PathResponse pathResponse = response.as(PathResponse.class);

        List<Long> stationIds = pathResponse.getStations().stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());

        List<Long> expectedPathIds = expectedPath.stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());

        assertThat(stationIds).containsExactlyElementsOf(expectedPathIds);
    }

    public static void 총_거리가_응답됨(ExtractableResponse<Response> response, int totalDistance) {
        PathResponse pathResponse = response.as(PathResponse.class);
        assertThat(pathResponse.getDistance()).isEqualTo(totalDistance);
    }

    public static void 요금이_응답됨(ExtractableResponse<Response> response, int fare) {
        PathResponse pathResponse = response.as(PathResponse.class);
        assertThat(pathResponse.getFare()).isEqualTo(fare);
    }
}
