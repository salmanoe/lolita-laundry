-- Store the login email on the users row so the SUPER_ADMIN approval flow can detect a duplicate
-- identity — the same person coming back under a second Auth0 sub (e.g. Google vs email+password) —
-- before it silently creates a second users row the person can never log in as.
-- Nullable: existing rows and the manual "paste sub" create path (POST /api/users) carry no email.
ALTER TABLE users ADD COLUMN email VARCHAR(160);
