-- Item stats now read precomputed summary tables; pg_stat_user_indexes showed 0 uses of this 603 MB index.
DROP INDEX IF EXISTS idx_item_purchases_item;