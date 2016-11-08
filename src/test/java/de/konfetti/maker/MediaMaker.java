package de.konfetti.maker;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import de.konfetti.data.MediaItem;
import de.konfetti.data.enums.MediaItemReviewEnum;
import de.konfetti.data.enums.MediaItemTypeEnum;

import java.util.Date;

import static com.natpryce.makeiteasy.Property.newProperty;
import static de.konfetti.data.enums.MediaItemReviewEnum.REVIEWED_PUBLIC;
import static de.konfetti.data.enums.MediaItemTypeEnum.TYPE_TEXT;

/**
 * Created by relampago on 05.10.16.
 */
public class MediaMaker {

    public static final Property<MediaItem, Long> id = newProperty();
    public static final Property<MediaItem, Long> userId = newProperty();
    public static final Property<MediaItem, Long> lastUpdateTS = newProperty();
    public static final Property<MediaItem, MediaItemTypeEnum> type = newProperty();
    public static final Property<MediaItem, MediaItemReviewEnum> reviewed = newProperty();
    public static final Property<MediaItem, String> comment = newProperty();
    public static final Property<MediaItem, String> data = newProperty();

    public static final Instantiator<MediaItem> ExampleMediaItem = new Instantiator<MediaItem>() {
        @Override
        public MediaItem instantiate(PropertyLookup<MediaItem> propertyLookup) {
            MediaItem mediaItem = new MediaItem();
            mediaItem.setId(propertyLookup.valueOf(id, Long.parseLong("0")));
            mediaItem.setUserId(propertyLookup.valueOf(userId, Long.parseLong("0")));
            mediaItem.setLastUpdateTS(propertyLookup.valueOf(lastUpdateTS, new Date().getTime()));
            mediaItem.setType(propertyLookup.valueOf(type, TYPE_TEXT));
            mediaItem.setReviewed(propertyLookup.valueOf(reviewed, REVIEWED_PUBLIC));
            mediaItem.setComment(propertyLookup.valueOf(comment, "Test Comment"));
            mediaItem.setData(propertyLookup.valueOf(data, "Test Content"));
            return mediaItem;
        }
    };

}
