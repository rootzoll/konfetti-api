ALTER TABLE notification
ADD CONSTRAINT notification_user_id_fk
FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE notification
  ADD CONSTRAINT notification_party_id_fk
FOREIGN KEY (party_id) REFERENCES party (id);