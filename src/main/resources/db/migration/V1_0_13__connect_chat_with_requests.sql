ALTER TABLE chat
ADD CONSTRAINT chat_host_id_fk
FOREIGN KEY (host_id) REFERENCES user (id);

ALTER TABLE chat
ADD CONSTRAINT chat_party_id_fk
FOREIGN KEY (party_id) REFERENCES party (id);

ALTER TABLE chat
ADD CONSTRAINT chat_request_id_fk
FOREIGN KEY (request_id) REFERENCES request (id);
