CREATE SEQUENCE incident_number_seq AS BIGINT START WITH 1;

-- Continue after existing numeric references, including references with fewer digits.
SELECT setval('incident_number_seq', COALESCE(MAX(substring(incident_number FROM 5)::BIGINT), 0) + 1, false)
FROM incidents
WHERE incident_number ~ '^INC-[0-9]{1,18}$';

-- Replace only the old generated UUID references; retain custom incident references.
DO $$
DECLARE
    legacy RECORD;
    number_text TEXT;
BEGIN
    FOR legacy IN
        SELECT id FROM incidents
        WHERE incident_number ~* '^INC-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        ORDER BY created_at, id
    LOOP
        number_text := nextval('incident_number_seq')::TEXT;
        UPDATE incidents
        SET incident_number = 'INC-' || lpad(number_text, GREATEST(6, length(number_text)), '0')
        WHERE id = legacy.id;
    END LOOP;
END $$;
