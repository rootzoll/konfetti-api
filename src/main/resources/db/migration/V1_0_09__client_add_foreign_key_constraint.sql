ALTER TABLE `client`
ADD CONSTRAINT client_user_id_fk
FOREIGN KEY (user_id) REFERENCES `user` (id);