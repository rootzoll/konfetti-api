ALTER TABLE `user` DROP `active_on_parties`;

CREATE TABLE `user_party_active` (
  `user_id` bigint(20) NOT NULL,
  `party_id` bigint(20) NOT NULL,
  KEY `user_party_active_key_user_id` (`party_id`),
  KEY `user_party_active_key_party_id` (`user_id`),
  CONSTRAINT `user_party_active_fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `user_party_active_fk_party_id` FOREIGN KEY (`party_id`) REFERENCES `party` (`id`),
  UNIQUE INDEX `user_party_active_user_id_party_id_uindex` (user_id, party_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


