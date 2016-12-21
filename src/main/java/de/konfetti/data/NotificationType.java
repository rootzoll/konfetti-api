package de.konfetti.data;

/**
 * types of notifications getting send to user
 */
public enum NotificationType {
	MEDIAITEM_FULL,			// generic 
	MEDIAITEM_INFO,			// generic
	REVIEW_OK,				// review of own task is OK
	PAYBACK,				// konfetti voted on a task got payed back (e.g. wehn task got deleted)
	REVIEW_FAIL,			// review of own task was REJECTED
	CHAT_NEW,				// a new chat message is available
	PARTY_WELCOME,			// welcome message on a party
	REWARD_GOT,				// received a reward
	SUPPORT_WIN, 			// when a task you supported got done
	LOGOUT_REMINDER,		// remind user to logout on browser
	REVIEW_WAITING;			// task is waiting for reviewer
}
