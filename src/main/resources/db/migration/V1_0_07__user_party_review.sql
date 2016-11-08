ALTER TABLE `user` DROP `reviewer_on_parties`;

CREATE TABLE `user_party_review` (
  `user_id` bigint(20) NOT NULL,
  `party_id` bigint(20) NOT NULL,
  KEY `FKj431gt6knl9a7vxck6unjuto` (`party_id`),
  KEY `FKiy1axlf9xacdot9iq1rit6hul` (`user_id`),
  CONSTRAINT `user_party_review_fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `user_party_review_fk_party_id` FOREIGN KEY (`party_id`) REFERENCES `party` (`id`),
  UNIQUE INDEX `user_party_review_user_id_party_id_uindex` (user_id, party_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


