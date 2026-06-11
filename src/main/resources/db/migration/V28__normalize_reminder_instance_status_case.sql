UPDATE reminder_instances
SET status = UPPER(status)
WHERE status <> UPPER(status);
