-- Make sure enum-backed columns can store full enum names.
-- This fixes MySQL schemas that were created with too-short VARCHAR or ENUM.
-- If the table/columns do not exist yet, we allow startup to continue.

ALTER TABLE product MODIFY category VARCHAR(50);
ALTER TABLE product MODIFY status VARCHAR(30);
