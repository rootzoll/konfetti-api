update media_item set type = "TYPE_UNKNOWN" where type = "n/a";
update media_item set type = "TYPE_TEXT" where type = "java.lang.String";
update media_item set type = "TYPE_MULTILANG" where type = "MediaItemMultiLang";
update media_item set type = "TYPE_LOCATION" where type = "Location";
update media_item set type = "TYPE_IMAGE" where type = "Image";
update media_item set type = "TYPE_DATE" where type = "Date";