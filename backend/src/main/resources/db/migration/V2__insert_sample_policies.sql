;WITH batch_numbers AS (
    SELECT batch_number
    FROM (VALUES
        (0), (1), (2), (3), (4), (5), (6), (7), (8), (9),
        (10), (11), (12), (13), (14), (15), (16), (17), (18), (19)
    ) AS batches(batch_number)
),
unit_numbers AS (
    SELECT unit_number
    FROM (VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9)
    ) AS units(unit_number)
),
given_names AS (
    SELECT name_number, first_name
    FROM (VALUES
        (1, N'Mei'),
        (2, N'Wei'),
        (3, N'Haruto'),
        (4, N'Aiko'),
        (5, N'Kenji'),
        (6, N'Yuki'),
        (7, N'Siti'),
        (8, N'Nur'),
        (9, N'Dewi'),
        (10, N'Budi'),
        (11, N'Arjun'),
        (12, N'Priya'),
        (13, N'Ananya'),
        (14, N'Rohan'),
        (15, N'Min-jun'),
        (16, N'Hye-jin'),
        (17, N'Ji-hoon'),
        (18, N'Dara'),
        (19, N'Kanya'),
        (20, N'Somchai')
    ) AS names(name_number, first_name)
),
family_names AS (
    SELECT name_number, family_name
    FROM (VALUES
        (1, N'Tan'),
        (2, N'Lim'),
        (3, N'Wong'),
        (4, N'Lee'),
        (5, N'Sato'),
        (6, N'Suzuki'),
        (7, N'Nguyen'),
        (8, N'Tran'),
        (9, N'Chen'),
        (10, N'Kumar')
    ) AS names(name_number, family_name)
),
regions AS (
    SELECT region_number, region_name
    FROM (VALUES
        (1, N'Singapore'),
        (2, N'Hong Kong'),
        (3, N'Australia'),
        (4, N'Japan'),
        (5, N'Thailand'),
        (6, N'Indonesia'),
        (7, N'Malaysia'),
        (8, N'Philippines')
    ) AS values_by_region(region_number, region_name)
),
currencies AS (
    SELECT currency_number, currency_code
    FROM (VALUES
        (1, 'USD'),
        (2, 'SGD'),
        (3, 'HKD'),
        (4, 'AUD'),
        (5, 'JPY'),
        (6, 'THB')
    ) AS values_by_currency(currency_number, currency_code)
),
underwriters AS (
    SELECT underwriter_number, underwriter_name
    FROM (VALUES
        (1, N'A. Tan'),
        (2, N'M. Chen'),
        (3, N'Y. Sato'),
        (4, N'P. Kumar'),
        (5, N'N. Nguyen'),
        (6, N'S. Lim'),
        (7, N'R. Wong'),
        (8, N'D. Lee')
    ) AS values_by_underwriter(underwriter_number, underwriter_name)
),
numbered_rows AS (
    SELECT batch_numbers.batch_number * 10 + unit_numbers.unit_number + 1 AS record_number
    FROM batch_numbers
    CROSS JOIN unit_numbers
),
policy_values AS (
    SELECT
        numbered_rows.record_number,
        CONCAT(given_names.first_name, N' ', family_names.family_name) AS policyholder_name,
        CASE (numbered_rows.record_number - 1) % 4
            WHEN 0 THEN 'Property'
            WHEN 1 THEN 'Casualty'
            WHEN 2 THEN 'A&H'
            ELSE 'Marine'
        END AS line_of_business,
        CASE (numbered_rows.record_number - 1) % 4
            WHEN 0 THEN 'Active'
            WHEN 1 THEN 'Expired'
            WHEN 2 THEN 'Pending'
            ELSE 'Cancelled'
        END AS status,
        CONVERT(
            DECIMAL(19, 2),
            1000
                + ((numbered_rows.record_number * 25139) % 4999000)
                + ((numbered_rows.record_number * 37) % 100) / 100.0
        ) AS premium_amount,
        currencies.currency_code AS currency,
        regions.region_name AS region,
        underwriters.underwriter_name AS underwriter,
        CONVERT(DATE, SYSUTCDATETIME()) AS as_of_date
    FROM numbered_rows
    INNER JOIN given_names
        ON given_names.name_number = (numbered_rows.record_number - 1) % 20 + 1
    INNER JOIN family_names
        ON family_names.name_number = (numbered_rows.record_number - 1) / 20 + 1
    INNER JOIN regions
        ON regions.region_number = (numbered_rows.record_number - 1) % 8 + 1
    INNER JOIN currencies
        ON currencies.currency_number = (numbered_rows.record_number - 1) % 6 + 1
    INNER JOIN underwriters
        ON underwriters.underwriter_number = (numbered_rows.record_number - 1) % 8 + 1
),
dated_policies AS (
    SELECT
        policy_values.*,
        CASE policy_values.status
            WHEN 'Active' THEN DATEADD(day, -((record_number - 1) % 364), as_of_date)
            WHEN 'Expired' THEN DATEADD(day, -(366 + ((record_number - 1) % 730)), as_of_date)
            WHEN 'Pending' THEN DATEADD(day, 1 + ((record_number - 1) % 180), as_of_date)
            ELSE DATEADD(day, -(30 + ((record_number - 1) % 730)), as_of_date)
        END AS effective_date
    FROM policy_values
)
INSERT INTO dbo.policies (
    id,
    policy_number,
    policyholder_name,
    line_of_business,
    status,
    premium_amount,
    currency,
    effective_date,
    expiry_date,
    region,
    underwriter,
    flagged_for_review,
    created_at,
    updated_at
)
SELECT
    NEWID(),
    CONCAT('POL-', RIGHT('000000' + CONVERT(VARCHAR(6), record_number), 6)),
    policyholder_name,
    line_of_business,
    status,
    premium_amount,
    currency,
    effective_date,
    DATEADD(day, 365, effective_date),
    region,
    underwriter,
    CONVERT(BIT, 0),
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM dated_policies;
