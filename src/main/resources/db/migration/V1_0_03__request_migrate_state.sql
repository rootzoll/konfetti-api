update request set state = "STATE_REVIEW" where state = "review";
update request set state = "STATE_REJECTED" where state = "rejected";
update request set state = "STATE_OPEN" where state = "open";
update request set state = "STATE_PROCESSING" where state = "processing";
update request set state = "STATE_DONE" where state = "done";