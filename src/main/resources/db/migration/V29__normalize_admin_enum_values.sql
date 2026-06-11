UPDATE safety_events
SET status = 'SENT'
WHERE UPPER(status) = 'DELIVERED';

UPDATE safety_events
SET status = UPPER(status)
WHERE status <> UPPER(status);

UPDATE user_responses
SET action = 'IM_SAFE'
WHERE UPPER(action) IN ('COMPLETED', 'DONE', 'RESOLVED');

UPDATE user_responses
SET action = 'SNOOZE'
WHERE UPPER(action) IN ('DISMISSED', 'IGNORED');

UPDATE user_responses
SET action = UPPER(action)
WHERE action <> UPPER(action);

ALTER TABLE safety_events
ADD CONSTRAINT chk_safety_events_status
CHECK (status IN ('SENT', 'FAILED', 'ACKNOWLEDGED'));

ALTER TABLE user_responses
ADD CONSTRAINT chk_user_responses_action
CHECK (action IN ('SNOOZE', 'IM_SAFE', 'NEED_HELP'));
