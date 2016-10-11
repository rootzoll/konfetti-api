package de.konfetti.controller;

import de.konfetti.Application;
import de.konfetti.data.MediaItem;
import de.konfetti.data.enums.MediaItemTypeEnum;
import de.konfetti.service.MediaService;
import io.restassured.http.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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
                .then().statusCode(HttpStatus.OK.value());
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