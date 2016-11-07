ALTER TABLE `user` DROP admin_on_parties;

CREATE TABLE `user_party_admin` (
  `user_id` bigint(20) NOT NULL,
  `party_id` bigint(20) NOT NULL,
  KEY `FKj431gt6knl9a7vxck6unjuto` (`party_id`),
  KEY `FKiy1axlf9xacdot9iq1rit6hul` (`user_id`),
  CONSTRAINT `FKiy1axlf9xacdot9iq1rit6hul` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKj431gt6knl9a7vxck6unjuto` FOREIGN KEY (`party_id`) REFERENCES `party` (`id`),
  UNIQUE INDEX `user_party_admin_user_id_party_id_uindex` (user_id, party_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


