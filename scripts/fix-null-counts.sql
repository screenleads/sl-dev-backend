-- Fix null values in Promotion counts
-- These fields should never be null based on @Builder.Default annotations
-- But existing records might have nulls from before the annotations were added

UPDATE promotion
SET 
    click_count = COALESCE(click_count, 0),
    impression_count = COALESCE(impression_count, 0),
    redemption_count = COALESCE(redemption_count, 0),
    unique_users_count = COALESCE(unique_users_count, 0)
WHERE 
    click_count IS NULL 
    OR impression_count IS NULL 
    OR redemption_count IS NULL
    OR unique_users_count IS NULL;

-- Show updated records
SELECT 
    id,
    name,
    click_count,
    impression_count,
    redemption_count,
    unique_users_count
FROM promotion
ORDER BY id;
