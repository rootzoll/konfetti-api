package de.konfetti.controller;

import de.konfetti.Application;
import de.konfetti.data.MediaItem;
import de.konfetti.data.enums.MediaItemTypeEnum;
import de.konfetti.service.MediaService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static de.konfetti.data.enums.MediaItemReviewEnum.REVIEWED_PRIVATE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Created by relampago on 11.10.16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MediaItemControllerTest extends BaseControllerTest {

    @Autowired
    MediaService mediaService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void createMedia() throws Exception {
        MediaItem mediaItem = testHelper.getMediaItem();
        myGivenAdmin()
                .contentType(ContentType.JSON)
                .body(mediaItem)
                .post(MediaItemController.REST_API_MAPPING)
                .then().statusCode(HttpStatus.OK.value())
                .body("id", greaterThan(0))
                .body("reviewed", is(REVIEWED_PRIVATE.name()))
                .body("comment", is(mediaItem.getComment()))
                .body("type", is(mediaItem.getType().name()))
                .body("data", is(mediaItem.getData()))
        ;
    }

    @Test
    public void createMediaWithWrongType() {
        MediaItem mediaItem = testHelper.getMediaItem();
        mediaItem.setType(MediaItemTypeEnum.TYPE_UNKOWN);
        myGivenAdmin()
                .contentType(ContentType.JSON)
                .body(mediaItem)
                .post(MediaItemController.REST_API_MAPPING)
                .then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void createMediaWithArabicText() {
        String arabicText = "اختبار للغة العربية";
        MediaItem mediaItem = testHelper.getMediaItem();
        mediaItem.setData(arabicText);
        MediaItem persistedMediaItem = mediaService.create(mediaItem);
        Response response = myGivenAdmin()
                .get(MediaItemController.REST_API_MAPPING + "/" + persistedMediaItem.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().
                        response();

        Assert.assertEquals(response.path("data"), arabicText);
//        assertTh response.path("data");
    }

    @Test
    public void getMedia() throws Exception {
        MediaItem mediaItem = testHelper.getMediaItem();
        MediaItem persistedMediaItem = mediaService.create(mediaItem);
        myGivenAdmin()
                .get(MediaItemController.REST_API_MAPPING + "/" + persistedMediaItem.getId())
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Ignore
    @Test
    public void getMediaAsImage() throws Exception {

    }

}