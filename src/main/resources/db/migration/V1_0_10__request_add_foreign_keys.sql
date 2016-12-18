ALTER TABLE request
ADD CONSTRAINT request_user_id_fk
FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE request
  ADD CONSTRAINT request_party_id_fk
FOREIGN KEY (party_id) REFERENCES party (id);

ALTER TABLE konfetti.request DROP user_name;
