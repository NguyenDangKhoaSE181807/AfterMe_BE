-- Align email_verification_codes.id with JPA Long mapping (BIGINT)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'email_verification_codes'
          AND column_name = 'id'
          AND data_type = 'integer'
    ) THEN
        ALTER TABLE public.email_verification_codes
            ALTER COLUMN id TYPE BIGINT;

        ALTER SEQUENCE IF EXISTS public.email_verification_codes_id_seq AS BIGINT;

        ALTER TABLE public.email_verification_codes
            ALTER COLUMN id SET DEFAULT nextval('public.email_verification_codes_id_seq'::regclass);
    END IF;
END $$;