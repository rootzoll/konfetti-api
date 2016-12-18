package de.konfetti.maker.dto;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.Chat;
import de.konfetti.data.MediaItem;
import de.konfetti.data.enums.RequestStateEnum;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.natpryce.makeiteasy.Property.newProperty;

/**
 * Created by relampago on 05.10.16.
 */
public class RequestVmMaker {

    public static final Property<RequestVm, Long> id = newProperty();
    public static final Property<RequestVm, Long> userId = newProperty();
    public static final Property<RequestVm, Long> partyId = newProperty();
    public static final Property<RequestVm, RequestStateEnum> state = newProperty();
    public static final Property<RequestVm, String> title = newProperty();
    public static final Property<RequestVm, Long> titleMultiLangRef = newProperty();
    public static final Property<RequestVm, Long> time = newProperty();
    public static final Property<RequestVm, Long[]> mediaItemIds = newProperty();
    public static final Property<RequestVm, String> userName = newProperty();
    public static final Property<RequestVm, Long> imageMediaId = newProperty();
    public static final Property<RequestVm, String[]> spokenLangs = newProperty();
    public static final Property<RequestVm, Long> konfettiCount = newProperty();
    public static final Property<RequestVm, Long> konfettiAdd = newProperty();
    public static final Property<RequestVm, List<Chat>> chats = newProperty();
    public static final Property<RequestVm, List<MediaItem>> info = newProperty();
//    public static final Property<RequestVm, MediaItem> titleMultiLang = newProperty();
    public static final Property<RequestVm, Long> konfettiAmountSupport = newProperty();
    public static final Property<RequestVm, Long> konfettiAmountReward = newProperty();


    public static final Instantiator<RequestVm> ExampleRequestVm = new Instantiator<RequestVm>() {
        @Override
        public RequestVm instantiate(PropertyLookup<RequestVm> propertyLookup) {
            Long nullLongValue = null;
            MediaItem nullMediaItem = null;

            RequestVm requestVm = new RequestVm();
            requestVm.setId(propertyLookup.valueOf(id, nullLongValue));
            requestVm.setUserId(propertyLookup.valueOf(userId, nullLongValue));
            requestVm.setPartyId(propertyLookup.valueOf(partyId, nullLongValue));
            requestVm.setState(propertyLookup.valueOf(state, RequestStateEnum.STATE_OPEN));
            requestVm.setTitle(propertyLookup.valueOf(title, "defaultRequestTitle"));
            requestVm.setTitleMultiLangRef(propertyLookup.valueOf(titleMultiLangRef, nullLongValue));
            requestVm.setTime(propertyLookup.valueOf(time, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));
            requestVm.setMediaItemIds(propertyLookup.valueOf(mediaItemIds, new Long[]{}));
            requestVm.setUserName(propertyLookup.valueOf(userName, "defaultUserName"));
            requestVm.setImageMediaID(propertyLookup.valueOf(imageMediaId, nullLongValue));
            requestVm.setSpokenLangs(propertyLookup.valueOf(spokenLangs, new String[]{}));
            requestVm.setKonfettiCount(propertyLookup.valueOf(konfettiCount, Long.valueOf(0L)));
            requestVm.setKonfettiAdd(propertyLookup.valueOf(konfettiAdd, Long.valueOf(0L)));
            requestVm.setChats(propertyLookup.valueOf(chats, new ArrayList<>()));
            requestVm.setInfo(propertyLookup.valueOf(info, new ArrayList<>()));
//            requestVm.setTitleMultiLang(propertyLookup.valueOf(titleMultiLang, nullMediaItem));
            requestVm.setKonfettiAmountSupport(propertyLookup.valueOf(konfettiAmountSupport, Long.valueOf(0L)));
            requestVm.setKonfettiAmountReward(propertyLookup.valueOf(konfettiAmountReward, Long.valueOf(0L)));
            return requestVm;
        }
    };

}
